package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.domain.entities.OrderReturn;
import com.ibm.capstone.bookstore.domain.entities.ReturnStatus;
import com.ibm.capstone.bookstore.domain.entities.Shipment;
import com.ibm.capstone.bookstore.domain.entities.ShipmentStatus;
import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.ReturnItem;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.OrderRepository;
import com.ibm.capstone.bookstore.repository.OrderReturnRepository;
import com.ibm.capstone.bookstore.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Returns and return shipments. A customer asks to send delivered books back (REQUESTED), the carrier collects
 * them (PICKED_UP) and, once the store has the parcel, the refund is paid out (REFUNDED). Books only go back on the
 * shelf when the refund is processed, never when the customer merely asks.
 */
@Service
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderReturnRepository returnRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReturnPolicy returnPolicy;
    private final RefundCalculator refundCalculator;
    private final RefundProcessor refundProcessor;

    public ReturnService(OrderRepository orderRepository, OrderReturnRepository returnRepository,
                         ShipmentRepository shipmentRepository, ReturnPolicy returnPolicy,
                         RefundCalculator refundCalculator, RefundProcessor refundProcessor) {
        this.orderRepository = orderRepository;
        this.returnRepository = returnRepository;
        this.shipmentRepository = shipmentRepository;
        this.returnPolicy = returnPolicy;
        this.refundCalculator = refundCalculator;
        this.refundProcessor = refundProcessor;
    }

    // ---- Customer ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LifecycleDto.ReturnEligibility eligibility(Long userId, Long orderId) {
        Order order = loadOrder(userId, orderId);
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
        var blocker = returnPolicy.blocker(order, shipment);
        LocalDate deadline = returnPolicy.deadline(shipment).orElse(null);
        if (blocker.isPresent()) {
            return new LifecycleDto.ReturnEligibility(false, blocker.get(), deadline, List.of());
        }
        List<LifecycleDto.ReturnableLine> lines = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            int available = returnable(item);
            if (available > 0) {
                lines.add(new LifecycleDto.ReturnableLine(item.getId(), item.getBookTitle(), available, item.getUnitPrice()));
            }
        }
        if (lines.isEmpty()) {
            return new LifecycleDto.ReturnEligibility(false, "Every book on this order has already been returned or is being returned.", deadline, lines);
        }
        return new LifecycleDto.ReturnEligibility(true, null, deadline, lines);
    }

    @Transactional
    public LifecycleDto.ReturnResponse request(Long userId, Long orderId, LifecycleDto.ReturnRequest request) {
        Order order = loadOrder(userId, orderId);
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
        returnPolicy.blocker(order, shipment).ifPresent(reason -> {
            throw new BadRequestException(reason);
        });

        int[] units = new int[order.getItems().size()];
        List<ReturnItem> lines = new ArrayList<>();
        for (LifecycleDto.ReturnLine line : request.items()) {
            int index = indexOf(order, line.orderItemId());
            OrderItem item = order.getItems().get(index);
            units[index] += line.quantity();
            if (units[index] > returnable(item)) {
                throw new BadRequestException("You can return at most " + returnable(item) + " of '" + item.getBookTitle() + "'.");
            }
        }
        for (int i = 0; i < units.length; i++) {
            if (units[i] > 0) lines.add(new ReturnItem(order.getItems().get(i), units[i]));
        }

        boolean everything = true;
        for (int i = 0; i < units.length; i++) {
            if (order.getItems().get(i).remainingQuantity() - openReturnUnits(order, order.getItems().get(i)) - units[i] > 0) {
                everything = false;
                break;
            }
        }
        // A preview of what the customer will get back. The final figure is fixed when the refund is processed.
        RefundCalculator.Refund preview = refundCalculator.refund(order, units, everything);

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setRmaNumber("RMA-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + (100000 + ThreadLocalRandom.current().nextInt(900000)));
        orderReturn.setOrder(order);
        orderReturn.setReason(request.reason().trim());
        orderReturn.setEstimatedRefund(preview.grossRefund().min(order.getTotalAmount()));
        orderReturn.setReturnCarrier(shipment != null ? shipment.getCarrier() : "BlueDart Express");
        orderReturn.setReturnTrackingNumber("RT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        lines.forEach(orderReturn::addItem);

        OrderReturn saved = returnRepository.save(orderReturn);
        return toResponse(saved, "Return " + saved.getRmaNumber() + " requested. A carrier will collect the parcel; your refund is paid once we receive it.");
    }

    @Transactional(readOnly = true)
    public List<LifecycleDto.ReturnResponse> mine(Long userId) {
        return returnRepository.findByOrderUserIdOrderByRequestedAtDesc(userId).stream()
                .map(r -> toResponse(r, null)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LifecycleDto.ReturnResponse get(Long userId, Long returnId) {
        return toResponse(returnRepository.findByIdAndOrderUserId(returnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnId)), null);
    }

    @Transactional
    public LifecycleDto.ReturnResponse cancel(Long userId, Long returnId) {
        OrderReturn orderReturn = returnRepository.findByIdAndOrderUserId(returnId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnId));
        if (orderReturn.getStatus() != ReturnStatus.REQUESTED) {
            throw new BadRequestException("This return can no longer be withdrawn because " + describe(orderReturn.getStatus()) + ".");
        }
        orderReturn.setStatus(ReturnStatus.REJECTED);
        orderReturn.setAdminNote("Withdrawn by the customer");
        orderReturn.setCompletedAt(LocalDateTime.now());
        return toResponse(returnRepository.save(orderReturn), "Return " + orderReturn.getRmaNumber() + " withdrawn.");
    }

    // ---- Administrator -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LifecycleDto.ReturnResponse> all(boolean openOnly) {
        List<OrderReturn> returns = openOnly
                ? returnRepository.findByStatusInOrderByRequestedAtAsc(List.of(ReturnStatus.REQUESTED, ReturnStatus.PICKED_UP))
                : returnRepository.findAllByOrderByRequestedAtDesc();
        return returns.stream().map(r -> toResponse(r, null)).collect(Collectors.toList());
    }

    /** The carrier has collected the parcel from the customer. */
    @Transactional
    public LifecycleDto.ReturnResponse markPickedUp(Long returnId) {
        OrderReturn orderReturn = loadReturn(returnId);
        if (orderReturn.getStatus() != ReturnStatus.REQUESTED) {
            throw new BadRequestException("Only a requested return can be marked as picked up; this one " + describe(orderReturn.getStatus()) + ".");
        }
        orderReturn.setStatus(ReturnStatus.PICKED_UP);
        return toResponse(returnRepository.save(orderReturn), "Return " + orderReturn.getRmaNumber() + " marked as picked up.");
    }

    /** The store has the parcel: put the books back on the shelf and pay the refund. */
    @Transactional
    public LifecycleDto.ReturnResponse receiveAndRefund(Long returnId, String note) {
        OrderReturn orderReturn = loadReturn(returnId);
        if (!orderReturn.getStatus().isOpen()) {
            throw new BadRequestException("This return is already closed because " + describe(orderReturn.getStatus()) + ".");
        }
        Order order = orderReturn.getOrder();

        int[] units = new int[order.getItems().size()];
        for (ReturnItem line : orderReturn.getItems()) {
            units[indexOf(order, line.getOrderItem().getId())] += line.getQuantity();
        }
        boolean everything = true;
        for (int i = 0; i < units.length; i++) {
            if (order.getItems().get(i).remainingQuantity() - units[i] > 0) {
                everything = false;
                break;
            }
        }

        RefundCalculator.Refund refund = refundCalculator.refund(order, units, everything);
        String reference = refundProcessor.apply(order, refund, units, true, "returned Order #" + order.getOrderNumber());

        orderReturn.setRefundAmount(refund.cashBack());
        orderReturn.setRefundReference(reference);
        orderReturn.setPointsRefunded(refund.pointsBack());
        orderReturn.setPointsRevoked(refund.pointsToRevoke());
        orderReturn.setStatus(ReturnStatus.REFUNDED);
        orderReturn.setCompletedAt(LocalDateTime.now());
        orderReturn.setAdminNote(note);
        if (everything) {
            order.setOrderStatus(OrderStatus.RETURNED);
        }
        orderRepository.save(order);
        return toResponse(returnRepository.save(orderReturn),
                "Return " + orderReturn.getRmaNumber() + " received. ₹" + refund.cashBack().toPlainString() + " refunded.");
    }

    @Transactional
    public LifecycleDto.ReturnResponse reject(Long returnId, String note) {
        OrderReturn orderReturn = loadReturn(returnId);
        if (!orderReturn.getStatus().isOpen()) {
            throw new BadRequestException("This return is already closed because " + describe(orderReturn.getStatus()) + ".");
        }
        orderReturn.setStatus(ReturnStatus.REJECTED);
        orderReturn.setCompletedAt(LocalDateTime.now());
        orderReturn.setAdminNote(note == null || note.isBlank() ? "Rejected by the store" : note);
        return toResponse(returnRepository.save(orderReturn), "Return " + orderReturn.getRmaNumber() + " rejected.");
    }

    // ---- Helpers -----------------------------------------------------------------------

    private Order loadOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderReturn loadReturn(Long id) {
        return returnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Return not found: " + id));
    }

    /** Units of this item that can still be put in a new return: not refunded, and not already in an open return. */
    private int returnable(OrderItem item) {
        return Math.max(0, item.remainingQuantity() - openReturnUnits(item.getOrder(), item));
    }

    private int openReturnUnits(Order order, OrderItem item) {
        return returnRepository.findByOrderId(order.getId()).stream()
                .filter(r -> r.getStatus().isOpen())
                .flatMap(r -> r.getItems().stream())
                .filter(l -> l.getOrderItem().getId().equals(item.getId()))
                .mapToInt(ReturnItem::getQuantity)
                .sum();
    }

    private static int indexOf(Order order, Long orderItemId) {
        for (int i = 0; i < order.getItems().size(); i++) {
            if (orderItemId != null && orderItemId.equals(order.getItems().get(i).getId())) {
                return i;
            }
        }
        throw new ResourceNotFoundException("Order item not found: " + orderItemId);
    }

    private static String describe(ReturnStatus status) {
        return switch (status) {
            case REQUESTED -> "it has been requested";
            case PICKED_UP -> "the carrier has already collected it";
            case REFUNDED -> "it has already been refunded";
            case REJECTED -> "it was rejected or withdrawn";
        };
    }

    private LifecycleDto.ReturnResponse toResponse(OrderReturn r, String message) {
        List<LifecycleDto.ReturnLineResponse> lines = r.getItems().stream()
                .map(l -> new LifecycleDto.ReturnLineResponse(l.getOrderItem().getId(), l.getOrderItem().getBookTitle(), l.getQuantity(),
                        l.getOrderItem().getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()))))
                .collect(Collectors.toList());
        return new LifecycleDto.ReturnResponse(r.getId(), r.getRmaNumber(), r.getOrder().getId(), r.getOrder().getOrderNumber(),
                r.getStatus(), r.getReason(), lines, r.getEstimatedRefund(), r.getRefundAmount(), r.getRefundReference(),
                r.getPointsRefunded(), r.getPointsRevoked(), r.getReturnCarrier(), r.getReturnTrackingNumber(),
                r.getRequestedAt(), r.getCompletedAt(), r.getAdminNote(), message);
    }
}
