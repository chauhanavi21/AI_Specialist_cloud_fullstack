package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.*;
import com.ibm.capstone.bookstore.domain.entities.*;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.OrderCancellationException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.*;
import com.ibm.capstone.bookstore.service.CartService;
import com.ibm.capstone.bookstore.service.CouponService;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import com.ibm.capstone.bookstore.service.OrderService;
import com.ibm.capstone.bookstore.service.PaymentGateway;
import com.ibm.capstone.bookstore.service.PricingCalculator;
import com.ibm.capstone.bookstore.service.RefundCalculator;
import com.ibm.capstone.bookstore.service.RefundProcessor;
import com.ibm.capstone.bookstore.service.ReturnPolicy;
import com.ibm.capstone.bookstore.service.ShippingCalculator;
import com.ibm.capstone.bookstore.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final GiftPointsService giftPointsService;
    private final CartService cartService;
    private final PricingCalculator pricing;
    private final CouponService couponService;
    private final WalletService walletService;
    private final PaymentGateway paymentGateway;
    private final ShippingCalculator shippingCalculator;
    private final ShipmentRepository shipmentRepository;
    private final RefundCalculator refundCalculator;
    private final RefundProcessor refundProcessor;
    private final ReturnPolicy returnPolicy;

    @Value("${app.orders.cancellation-window-hours:48}")
    private long cancellationWindowHours = 48;

    @Value("${app.orders.modification-window-hours:24}")
    private long modificationWindowHours = 24;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartRepository cartRepository,
                            AddressRepository addressRepository,
                            UserRepository userRepository,
                            BookRepository bookRepository,
                            GiftPointsService giftPointsService,
                            CartService cartService,
                            PricingCalculator pricing,
                            CouponService couponService,
                            WalletService walletService,
                            PaymentGateway paymentGateway,
                            ShippingCalculator shippingCalculator,
                            ShipmentRepository shipmentRepository,
                            RefundCalculator refundCalculator,
                            RefundProcessor refundProcessor,
                            ReturnPolicy returnPolicy) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.giftPointsService = giftPointsService;
        this.cartService = cartService;
        this.pricing = pricing;
        this.couponService = couponService;
        this.walletService = walletService;
        this.paymentGateway = paymentGateway;
        this.shippingCalculator = shippingCalculator;
        this.shipmentRepository = shipmentRepository;
        this.refundCalculator = refundCalculator;
        this.refundProcessor = refundProcessor;
        this.returnPolicy = returnPolicy;
    }

    // =====================================================================================
    // Placing an order
    // =====================================================================================

    @Override
    @Transactional
    public OrderConfirmationResponse placeOrder(Long userId, PlaceOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + request.getAddressId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty. Add items before placing an order.");
        }

        // 1. Validate stock up front; nothing is changed until every check has passed.
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            if (book.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Book '" + book.getTitle() + "' is out of stock or requested quantity unavailable.");
            }
        }

        // 2. Price the order: coupon, tax and delivery all come from the shared calculator.
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponService.requireApplicable(request.getCouponCode(), subtotal);
            couponDiscount = coupon.discountFor(subtotal);
        }
        ShippingZone zone = shippingCalculator.zoneFor(address);
        PricingCalculator.Breakdown price = pricing.price(subtotal, couponDiscount, zone);

        // 3. Gift points redemption.
        int pointsToRedeem = request.getPointsToRedeem() != null ? request.getPointsToRedeem() : 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (pointsToRedeem > 0) {
            if (user.getGiftPointsBalance() < pointsToRedeem) {
                throw new BadRequestException("Insufficient gift points balance. Available: " + user.getGiftPointsBalance());
            }
            pointsDiscount = pricing.pointsValue(pointsToRedeem);
            if (pointsDiscount.compareTo(price.grossTotal()) > 0) {
                pointsDiscount = price.grossTotal();
                pointsToRedeem = pricing.pointsNeededFor(price.grossTotal());
            }
        }

        BigDecimal payable = price.grossTotal().subtract(pointsDiscount).max(BigDecimal.ZERO);

        // 4. Payment. A zero balance (fully covered by points) needs no instrument.
        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        String paymentReference = "NO-CHARGE";
        String cardLast4 = null;
        if (payable.signum() > 0) {
            if (request.getPaymentMethod() == PaymentMethod.WALLET) {
                if (user.getWalletBalance().compareTo(payable) < 0) {
                    throw new BadRequestException("Insufficient wallet balance. Available: ₹" + user.getWalletBalance()
                            + ", needed: ₹" + payable + ". Top up your wallet or choose another payment method.");
                }
                walletAmountUsed = payable;
            }
            PaymentGateway.PaymentResult result = paymentGateway.charge(request.getPaymentMethod(), request);
            paymentReference = result.reference();
            cardLast4 = result.cardLast4();
        }

        // 5. Everything validated: now apply the changes.
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            book.setCopiesSold(book.getCopiesSold() + item.getQuantity());
            bookRepository.save(book);
        }

        if (pointsToRedeem > 0) {
            user.setGiftPointsBalance(user.getGiftPointsBalance() - pointsToRedeem);
        }
        int pointsEarned = pricing.pointsEarned(payable);
        user.setGiftPointsBalance(user.getGiftPointsBalance() + pointsEarned);
        userRepository.save(user);

        Order order = new Order();
        order.setOrderNumber("BK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + (100000 + ThreadLocalRandom.current().nextInt(900000)));
        order.setUser(user);
        order.setDeliveryAddress(address);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setSubtotal(subtotal);
        order.setCouponCode(coupon != null ? coupon.getCode() : null);
        order.setCouponDiscount(couponDiscount);
        order.setTax(price.tax());
        order.setShippingFee(price.shippingFee());
        order.setShippingZone(zone);
        order.setPointsRedeemed(pointsToRedeem);
        order.setPointsDiscount(pointsDiscount);
        order.setPointsEarned(pointsEarned);
        order.setWalletAmountUsed(walletAmountUsed);
        order.setTotalAmount(payable);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCardLast4(cardLast4);
        order.setPaymentReference(paymentReference);

        int slowestBookDays = cart.getItems().stream()
                .mapToInt(i -> i.getBook().getTentativeDeliveryDays())
                .max().orElse(2);
        order.setEstimatedDeliveryDate(shippingCalculator.deliveryDate(slowestBookDays, zone));

        for (CartItem item : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(item.getBook());
            orderItem.setBookTitle(item.getBook().getTitle());
            orderItem.setBookAuthor(item.getBook().getAuthor());
            orderItem.setCoverImageUrl(item.getBook().getCoverImageUrl());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setTotalPrice(item.getSubtotal());
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 6. The outbound shipment: carrier and tracking number are assigned at purchase.
        Shipment shipment = new Shipment();
        shipment.setOrder(savedOrder);
        shipment.setCarrier(shippingCalculator.carrierFor(zone));
        shipment.setTrackingNumber(newTrackingNumber(zone));
        shipment.setStatus(ShipmentStatus.PROCESSING);
        shipment.setShippingZone(zone);
        shipment.setShippingFee(price.shippingFee());
        shipment.setEstimatedDeliveryDate(savedOrder.getEstimatedDeliveryDate());
        shipmentRepository.save(shipment);

        // 7. Ledgers.
        if (walletAmountUsed.signum() > 0) {
            walletService.debit(user, walletAmountUsed, savedOrder.getId(), "Paid for order " + savedOrder.getOrderNumber());
        }
        if (pointsToRedeem > 0) {
            giftPointsService.recordTransaction(user, savedOrder.getId(), pointsToRedeem,
                    PointsTransactionType.REDEEMED, "Redeemed on Order #" + savedOrder.getOrderNumber());
        }
        if (pointsEarned > 0) {
            giftPointsService.recordTransaction(user, savedOrder.getId(), pointsEarned,
                    PointsTransactionType.EARNED, "Earned from Order #" + savedOrder.getOrderNumber());
        }

        // 8. Clear the cart.
        cartService.clearCart(userId);

        OrderConfirmationResponse response = baseResponse(savedOrder, user);
        response.setMessage("Payment successful and order confirmed! Your books are being prepared for dispatch.");
        return response;
    }

    private static String newTrackingNumber(ShippingZone zone) {
        String prefix = switch (zone) {
            case LOCAL -> "DZ";
            case REGIONAL -> "BD";
            case NATIONAL -> "DL";
        };
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    // =====================================================================================
    // Quote
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public StoreDto.QuoteResponse quote(Long userId, StoreDto.QuoteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request != null && request.couponCode() != null && !request.couponCode().isBlank()) {
            coupon = couponService.requireApplicable(request.couponCode(), subtotal);
            couponDiscount = coupon.discountFor(subtotal);
        }

        // With an address the delivery charge and date depend on the zone; without one the basket assumes regional delivery.
        Address address = request != null && request.addressId() != null
                ? addressRepository.findByIdAndUserId(request.addressId(), userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + request.addressId()))
                : null;
        ShippingZone zone = shippingCalculator.zoneFor(address);
        PricingCalculator.Breakdown price = pricing.price(subtotal, couponDiscount, zone);

        int slowestBookDays = cart.getItems().stream()
                .mapToInt(i -> i.getBook().getTentativeDeliveryDays()).max().orElse(2);

        int maxPoints = Math.min(user.getGiftPointsBalance(), pricing.pointsNeededFor(price.grossTotal()));
        int requested = request != null && request.pointsToRedeem() != null ? Math.max(0, request.pointsToRedeem()) : 0;
        int pointsApplied = Math.min(requested, maxPoints);
        BigDecimal pointsDiscount = pricing.pointsValue(pointsApplied).min(price.grossTotal());
        BigDecimal payable = price.grossTotal().subtract(pointsDiscount).max(BigDecimal.ZERO);

        return new StoreDto.QuoteResponse(
                subtotal, couponDiscount, coupon != null ? coupon.getCode() : null,
                price.tax(), price.shippingFee(), pointsDiscount, pointsApplied, maxPoints,
                payable, pricing.pointsEarned(payable), user.getWalletBalance(),
                user.getWalletBalance().compareTo(payable) >= 0,
                zone.name(), shippingCalculator.deliveryDate(slowestBookDays, zone));
    }

    // =====================================================================================
    // Reading orders
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return mapToOrderDetail(order);
    }

    // =====================================================================================
    // Cancelling (inside the cancellation window, before the parcel ships)
    // =====================================================================================

    @Override
    @Transactional
    public OrderConfirmationResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderCancellationException("Order #" + order.getOrderNumber() + " is already cancelled.");
        }

        if (order.getOrderStatus() != OrderStatus.CONFIRMED && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new OrderCancellationException("Order #" + order.getOrderNumber() + " cannot be cancelled because it has already shipped or completed. "
                    + "Once it is delivered you can return it instead.");
        }

        // 48-hour business constraint
        Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        if (duration.toHours() >= cancellationWindowHours) {
            throw new OrderCancellationException("Cancellation window expired. Orders can only be cancelled within " + cancellationWindowHours + " hours of purchase.");
        }

        // Refund only what is still outstanding: part of the order may already have been refunded by an edit.
        int[] units = new int[order.getItems().size()];
        for (int i = 0; i < units.length; i++) {
            units[i] = order.getItems().get(i).remainingQuantity();
        }
        RefundCalculator.Refund refund = refundCalculator.refund(order, units, true);
        refundProcessor.apply(order, refund, units, true, "cancelled Order #" + order.getOrderNumber());

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request != null && request.getReason() != null ? request.getReason() : "Customer cancelled within 48 hours");

        shipmentRepository.findByOrderId(order.getId()).ifPresent(s -> {
            s.setStatus(ShipmentStatus.CANCELLED);
            shipmentRepository.save(s);
        });

        Order savedOrder = orderRepository.save(order);

        OrderConfirmationResponse response = baseResponse(savedOrder, order.getUser());
        response.setMessage("Order #" + savedOrder.getOrderNumber() + " cancelled successfully. Full refund initiated and points restored.");
        return response;
    }

    // =====================================================================================
    // Modifying an order (change the address, or take books off)
    // =====================================================================================

    @Override
    @Transactional
    public OrderConfirmationResponse modifyOrder(Long userId, Long orderId, LifecycleDto.ModifyOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        boolean removing = request != null && request.removeItems() != null && !request.removeItems().isEmpty();
        boolean moving = request != null && request.addressId() != null;
        if (!removing && !moving) {
            throw new BadRequestException("Choose a new delivery address or the books to remove.");
        }
        requireModifiable(order);

        if (moving) {
            changeAddress(order, userId, request.addressId());
        }

        String message = "Order #" + order.getOrderNumber() + " updated.";
        if (removing) {
            message = removeItems(order, request.removeItems());
        }
        order.setModifiedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        OrderConfirmationResponse response = baseResponse(saved, order.getUser());
        response.setMessage(message);
        return response;
    }

    private void requireModifiable(Order order) {
        if (order.getOrderStatus() != OrderStatus.CONFIRMED && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new BadRequestException("Order #" + order.getOrderNumber() + " can no longer be changed because it is "
                    + order.getOrderStatus().name().toLowerCase() + ".");
        }
        Duration age = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        if (age.toHours() >= modificationWindowHours) {
            throw new BadRequestException("Orders can only be changed within " + modificationWindowHours
                    + " hours of purchase, before they ship.");
        }
        shipmentRepository.findByOrderId(order.getId()).ifPresent(s -> {
            if (s.getStatus() != ShipmentStatus.PROCESSING) {
                throw new BadRequestException("Order #" + order.getOrderNumber() + " has already been handed to the carrier.");
            }
        });
    }

    /** Moving the parcel to another address changes the delivery date but not what was already paid for delivery. */
    private void changeAddress(Order order, Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        ShippingZone newZone = shippingCalculator.zoneFor(address);
        if (newZone != order.getShippingZone() && newZone.ordinal() > order.getShippingZone().ordinal()) {
            throw new BadRequestException("That address is in a farther delivery zone than the one you paid for ("
                    + order.getShippingZone().name().toLowerCase() + "). Place a new order for it instead.");
        }
        order.setDeliveryAddress(address);
        int slowestBookDays = order.getItems().stream()
                .filter(i -> i.remainingQuantity() > 0 && i.getBook() != null)
                .mapToInt(i -> i.getBook().getTentativeDeliveryDays()).max().orElse(2);
        order.setShippingZone(newZone);
        order.setEstimatedDeliveryDate(shippingCalculator.deliveryDate(slowestBookDays, newZone));
        shipmentRepository.findByOrderId(order.getId()).ifPresent(s -> {
            s.setShippingZone(newZone);
            s.setCarrier(shippingCalculator.carrierFor(newZone));
            s.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
            shipmentRepository.save(s);
        });
    }

    private String removeItems(Order order, List<LifecycleDto.ItemRemoval> removals) {
        int[] units = new int[order.getItems().size()];
        for (LifecycleDto.ItemRemoval removal : removals) {
            int index = indexOfItem(order, removal.orderItemId());
            OrderItem item = order.getItems().get(index);
            units[index] += removal.quantity();
            if (units[index] > item.remainingQuantity()) {
                throw new BadRequestException("You can remove at most " + item.remainingQuantity() + " of '" + item.getBookTitle() + "'.");
            }
        }

        boolean allGone = true;
        for (int i = 0; i < units.length; i++) {
            if (order.getItems().get(i).remainingQuantity() - units[i] > 0) {
                allGone = false;
                break;
            }
        }
        if (allGone) {
            throw new BadRequestException("That would remove every book. Cancel the order instead.");
        }

        RefundCalculator.Refund refund = refundCalculator.refund(order, units, false);
        refundProcessor.apply(order, refund, units, true, "changes to Order #" + order.getOrderNumber());
        return "Removed the books from Order #" + order.getOrderNumber() + ". ₹" + refund.cashBack().toPlainString()
                + " refunded" + (refund.pointsBack() > 0 ? " and " + refund.pointsBack() + " points returned." : ".");
    }

    private static int indexOfItem(Order order, Long orderItemId) {
        for (int i = 0; i < order.getItems().size(); i++) {
            if (order.getItems().get(i).getId() != null && order.getItems().get(i).getId().equals(orderItemId)) {
                return i;
            }
        }
        throw new ResourceNotFoundException("Order item not found: " + orderItemId);
    }

    // =====================================================================================
    // Buy It Again
    // =====================================================================================

    @Override
    @Transactional
    public CartResponse buyItAgain(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        for (OrderItem item : order.getItems()) {
            if (item.getBook() != null) {
                AddToCartRequest req = new AddToCartRequest();
                req.setBookId(item.getBook().getId());
                req.setQuantity(item.getQuantity());
                cartService.addToCart(userId, req);
            }
        }

        return cartService.getCartByUserId(userId);
    }

    // =====================================================================================
    // Mapping
    // =====================================================================================

    private OrderConfirmationResponse baseResponse(Order order, User user) {
        OrderConfirmationResponse response = new OrderConfirmationResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCouponDiscount(order.getCouponDiscount());
        response.setWalletAmountUsed(order.getWalletAmountUsed());
        response.setPaymentReference(order.getPaymentReference());
        response.setPointsRedeemed(order.getPointsRedeemed());
        response.setPointsEarned(order.getPointsEarned());
        response.setNewGiftPointsBalance(user.getGiftPointsBalance());
        response.setNewWalletBalance(user.getWalletBalance());
        response.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        response.setItems(order.getItems().stream().map(OrderConfirmationResponse::itemFrom).collect(Collectors.toList()));
        return response;
    }

    private void fillSummary(OrderSummaryResponse dto, Order order) {
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPointsRedeemed(order.getPointsRedeemed());
        dto.setPointsDiscount(order.getPointsDiscount());
        dto.setPointsEarned(order.getPointsEarned());
        dto.setItemCount(order.getItems().stream().mapToInt(OrderItem::remainingQuantity).sum());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        dto.setRefundedAmount(order.getRefundedAmount());
        dto.setShippingZone(order.getShippingZone() != null ? order.getShippingZone().name() : null);

        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        dto.setShipmentStatus(shipment != null ? shipment.getStatus().name() : null);

        Duration age = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        boolean open = order.getOrderStatus() == OrderStatus.CONFIRMED || order.getOrderStatus() == OrderStatus.PROCESSING;
        dto.setCanCancel(open && age.toHours() < cancellationWindowHours);
        dto.setCanModify(open && age.toHours() < modificationWindowHours
                && (shipment == null || shipment.getStatus() == ShipmentStatus.PROCESSING)
                && order.getItems().stream().mapToInt(OrderItem::remainingQuantity).sum() > 1);

        returnPolicy.deadline(shipment).ifPresent(dto::setReturnDeadline);
        dto.setCanReturn(returnPolicy.blocker(order, shipment).isEmpty()
                && order.getItems().stream().anyMatch(i -> i.remainingQuantity() > 0));

        double hoursRemaining = Math.max(0.0, cancellationWindowHours - (age.toMinutes() / 60.0));
        dto.setHoursRemainingToCancel(Math.round(hoursRemaining * 10.0) / 10.0);
    }

    private OrderSummaryResponse mapToOrderSummary(Order order) {
        OrderSummaryResponse dto = new OrderSummaryResponse();
        fillSummary(dto, order);
        return dto;
    }

    private OrderDetailResponse mapToOrderDetail(Order order) {
        OrderDetailResponse dto = new OrderDetailResponse();
        fillSummary(dto, order);

        dto.setDeliveryAddress(AddressResponse.from(order.getDeliveryAddress()));
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCardLast4(order.getCardLast4());
        dto.setSubtotal(order.getSubtotal());
        dto.setTax(order.getTax());
        dto.setShippingFee(order.getShippingFee());
        dto.setCouponCode(order.getCouponCode());
        dto.setCouponDiscount(order.getCouponDiscount());
        dto.setWalletAmountUsed(order.getWalletAmountUsed());
        dto.setPaymentReference(order.getPaymentReference());
        dto.setCancelReason(order.getCancelReason());
        shipmentRepository.findByOrderId(order.getId()).ifPresent(s -> {
            dto.setCarrier(s.getCarrier());
            dto.setTrackingNumber(s.getTrackingNumber());
        });
        dto.setItems(order.getItems().stream().map(OrderConfirmationResponse::itemFrom).collect(Collectors.toList()));
        return dto;
    }
}
