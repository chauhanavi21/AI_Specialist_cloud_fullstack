package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.Shipment;
import com.ibm.capstone.bookstore.domain.entities.ShipmentStatus;
import com.ibm.capstone.bookstore.domain.entities.ShippingZone;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.OrderRepository;
import com.ibm.capstone.bookstore.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shipment tracking for customers, shipping rate quotes for anyone, and status changes for the store's staff. */
@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final ShippingCalculator shippingCalculator;
    private final PricingCalculator pricing;

    public ShipmentService(ShipmentRepository shipmentRepository, OrderRepository orderRepository,
                           ShippingCalculator shippingCalculator, PricingCalculator pricing) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.shippingCalculator = shippingCalculator;
        this.pricing = pricing;
    }

    /** What delivery would cost to a city and state, before the customer has even signed in. */
    public LifecycleDto.ShippingQuoteResponse quote(LifecycleDto.ShippingQuoteRequest request) {
        com.ibm.capstone.bookstore.domain.entities.Address probe = new com.ibm.capstone.bookstore.domain.entities.Address();
        probe.setCity(request.city());
        probe.setState(request.state());
        ShippingZone zone = shippingCalculator.zoneFor(probe);
        BigDecimal subtotal = request.subtotal() == null ? BigDecimal.ZERO : request.subtotal();
        BigDecimal fee = pricing.price(subtotal, BigDecimal.ZERO, zone).shippingFee();
        // With no basket value the charge shown is the zone's flat rate.
        if (subtotal.signum() == 0) {
            fee = shippingCalculator.feeFor(zone);
        }
        boolean free = subtotal.signum() > 0 && fee.signum() == 0;
        return new LifecycleDto.ShippingQuoteResponse(zone.name(), fee, free, pricing.getFreeShippingThreshold(),
                shippingCalculator.extraTransitDays(zone), shippingCalculator.carrierFor(zone));
    }

    @Transactional(readOnly = true)
    public LifecycleDto.ShipmentResponse track(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return toResponse(shipmentFor(order.getId()), order);
    }

    // ---- Store staff -------------------------------------------------------------------

    /** Hands the parcel to the carrier (SHIPPED) or records that it arrived (DELIVERED). */
    @Transactional
    public LifecycleDto.ShipmentResponse advance(Long orderId, ShipmentStatus target) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        Shipment shipment = shipmentFor(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new BadRequestException("Order #" + order.getOrderNumber() + " was cancelled, so it has nothing to ship.");
        }
        switch (target) {
            case SHIPPED -> {
                if (shipment.getStatus() != ShipmentStatus.PROCESSING) {
                    throw new BadRequestException("Only a parcel that is still being prepared can be marked as shipped; this one is "
                            + shipment.getStatus().name().toLowerCase() + ".");
                }
                shipment.setStatus(ShipmentStatus.SHIPPED);
                shipment.setShippedAt(LocalDateTime.now());
                order.setOrderStatus(OrderStatus.SHIPPED);
            }
            case DELIVERED -> {
                if (shipment.getStatus() != ShipmentStatus.SHIPPED) {
                    throw new BadRequestException("A parcel must be shipped before it can be delivered; this one is "
                            + shipment.getStatus().name().toLowerCase() + ".");
                }
                shipment.setStatus(ShipmentStatus.DELIVERED);
                shipment.setDeliveredAt(LocalDateTime.now());
                order.setOrderStatus(OrderStatus.DELIVERED);
            }
            default -> throw new BadRequestException("Status must be SHIPPED or DELIVERED.");
        }
        orderRepository.save(order);
        return toResponse(shipmentRepository.save(shipment), order);
    }

    // ---- Helpers -----------------------------------------------------------------------

    private Shipment shipmentFor(Long orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No shipment found for order " + orderId));
    }

    private LifecycleDto.ShipmentResponse toResponse(Shipment s, Order order) {
        List<LifecycleDto.TrackingEvent> events = new ArrayList<>();
        events.add(new LifecycleDto.TrackingEvent("ORDER_PLACED", "Order placed and payment confirmed", order.getCreatedAt()));
        events.add(new LifecycleDto.TrackingEvent("LABEL_CREATED",
                s.getCarrier() + " will carry this parcel, tracking " + s.getTrackingNumber(), s.getCreatedAt()));
        if (s.getShippedAt() != null) {
            events.add(new LifecycleDto.TrackingEvent("SHIPPED", "Handed to " + s.getCarrier(), s.getShippedAt()));
        }
        if (s.getDeliveredAt() != null) {
            events.add(new LifecycleDto.TrackingEvent("DELIVERED", "Delivered to the address on the order", s.getDeliveredAt()));
        }
        if (s.getStatus() == ShipmentStatus.CANCELLED && order.getCancelledAt() != null) {
            events.add(new LifecycleDto.TrackingEvent("CANCELLED", "Order cancelled before dispatch", order.getCancelledAt()));
        }
        return new LifecycleDto.ShipmentResponse(s.getId(), order.getId(), s.getCarrier(), s.getTrackingNumber(), s.getStatus(),
                s.getShippingZone() != null ? s.getShippingZone().name() : null, s.getShippingFee(),
                s.getEstimatedDeliveryDate(), s.getShippedAt(), s.getDeliveredAt(), events);
    }
}
