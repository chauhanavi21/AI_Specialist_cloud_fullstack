package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.*;
import com.ibm.capstone.bookstore.domain.entities.*;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.OrderCancellationException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.*;
import com.ibm.capstone.bookstore.service.CartService;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import com.ibm.capstone.bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
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

    @Value("${app.orders.cancellation-window-hours:48}")
    private long cancellationWindowHours;

    @Value("${app.loyalty.dollars-per-point-redeemed:0.10}")
    private double dollarsPerPointRedeemed;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartRepository cartRepository,
                            AddressRepository addressRepository,
                            UserRepository userRepository,
                            BookRepository bookRepository,
                            GiftPointsService giftPointsService,
                            CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.giftPointsService = giftPointsService;
        this.cartService = cartService;
    }

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

        // 1. Verify and reduce stock
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            if (book.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Book '" + book.getTitle() + "' is out of stock or requested quantity unavailable.");
            }
            book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            bookRepository.save(book);
        }

        // 2. Compute subtotal & shipping
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = (subtotal.compareTo(BigDecimal.valueOf(50.00)) >= 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(5.99);

        // 3. Handle Gift Points Redemption (10 points = $1.00 USD)
        int pointsToRedeem = request.getPointsToRedeem() != null ? request.getPointsToRedeem() : 0;
        BigDecimal pointsDiscount = BigDecimal.ZERO;

        if (pointsToRedeem > 0) {
            if (user.getGiftPointsBalance() < pointsToRedeem) {
                throw new BadRequestException("Insufficient gift points balance. Available: " + user.getGiftPointsBalance());
            }

            pointsDiscount = BigDecimal.valueOf(pointsToRedeem * dollarsPerPointRedeemed).setScale(2, RoundingMode.HALF_UP);
            BigDecimal grossTotal = subtotal.add(tax).add(shippingFee);
            if (pointsDiscount.compareTo(grossTotal) > 0) {
                pointsDiscount = grossTotal;
                pointsToRedeem = (int) Math.ceil(grossTotal.doubleValue() / dollarsPerPointRedeemed);
            }

            user.setGiftPointsBalance(user.getGiftPointsBalance() - pointsToRedeem);
        }

        BigDecimal totalAmount = subtotal.add(tax).add(shippingFee).subtract(pointsDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 4. Calculate earned points (1 point per $10 spent)
        int pointsEarned = totalAmount.divide(BigDecimal.valueOf(10), 0, RoundingMode.FLOOR).intValue();
        user.setGiftPointsBalance(user.getGiftPointsBalance() + pointsEarned);
        userRepository.save(user);

        // 5. Create Order
        Order order = new Order();
        String orderNumber = "BK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + (1000 + new Random().nextInt(9000));
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setDeliveryAddress(address);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingFee(shippingFee);
        order.setPointsRedeemed(pointsToRedeem);
        order.setPointsDiscount(pointsDiscount);
        order.setPointsEarned(pointsEarned);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCardLast4(request.getCardLast4() != null ? request.getCardLast4() : "4242");

        // Estimated delivery: max delivery days of items in cart
        int maxDeliveryDays = cart.getItems().stream()
                .mapToInt(i -> i.getBook().getTentativeDeliveryDays())
                .max().orElse(2);
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(maxDeliveryDays));

        // Create Order Items
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

        // 6. Record Gift Points Transactions
        if (pointsToRedeem > 0) {
            giftPointsService.recordTransaction(
                    user,
                    savedOrder.getId(),
                    pointsToRedeem,
                    PointsTransactionType.REDEEMED,
                    "Redeemed on Order #" + savedOrder.getOrderNumber()
            );
        }
        if (pointsEarned > 0) {
            giftPointsService.recordTransaction(
                    user,
                    savedOrder.getId(),
                    pointsEarned,
                    PointsTransactionType.EARNED,
                    "Earned from Order #" + savedOrder.getOrderNumber()
            );
        }

        // 7. Clear Shopping Cart
        cartService.clearCart(userId);

        OrderConfirmationResponse response = new OrderConfirmationResponse();
        response.setOrderId(savedOrder.getId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setPaymentStatus(savedOrder.getPaymentStatus());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setPointsRedeemed(savedOrder.getPointsRedeemed());
        response.setPointsEarned(savedOrder.getPointsEarned());
        response.setNewGiftPointsBalance(user.getGiftPointsBalance());
        response.setEstimatedDeliveryDate(savedOrder.getEstimatedDeliveryDate());
        response.setMessage("Payment authorized and order confirmed! Your books are being prepared for dispatch.");

        return response;
    }

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

    @Override
    @Transactional
    public OrderConfirmationResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderCancellationException("Order #" + order.getOrderNumber() + " is already cancelled.");
        }

        if (order.getOrderStatus() != OrderStatus.CONFIRMED && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new OrderCancellationException("Order #" + order.getOrderNumber() + " cannot be cancelled because it has already shipped or completed.");
        }

        // 48-Hour Business Constraint Check
        Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        if (duration.toHours() >= cancellationWindowHours) {
            throw new OrderCancellationException("Cancellation window expired. Orders can only be cancelled within " + cancellationWindowHours + " hours of purchase.");
        }

        // 1. Mark Order as Cancelled and Payment as Refunded
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request != null && request.getReason() != null ? request.getReason() : "Customer cancelled within 48 hours");

        // 2. Restore Book Stock
        for (OrderItem item : order.getItems()) {
            if (item.getBook() != null) {
                Book book = item.getBook();
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                bookRepository.save(book);
            }
        }

        // 3. Restore Points & Revoke Earned Points
        User user = order.getUser();
        if (order.getPointsRedeemed() > 0) {
            user.setGiftPointsBalance(user.getGiftPointsBalance() + order.getPointsRedeemed());
            giftPointsService.recordTransaction(
                    user,
                    order.getId(),
                    order.getPointsRedeemed(),
                    PointsTransactionType.REFUNDED,
                    "Refunded points from cancelled Order #" + order.getOrderNumber()
            );
        }
        if (order.getPointsEarned() > 0) {
            user.setGiftPointsBalance(Math.max(0, user.getGiftPointsBalance() - order.getPointsEarned()));
        }
        userRepository.save(user);

        Order savedOrder = orderRepository.save(order);

        OrderConfirmationResponse response = new OrderConfirmationResponse();
        response.setOrderId(savedOrder.getId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setPaymentStatus(savedOrder.getPaymentStatus());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setPointsRedeemed(savedOrder.getPointsRedeemed());
        response.setPointsEarned(savedOrder.getPointsEarned());
        response.setNewGiftPointsBalance(user.getGiftPointsBalance());
        response.setEstimatedDeliveryDate(savedOrder.getEstimatedDeliveryDate());
        response.setMessage("Order #" + savedOrder.getOrderNumber() + " cancelled successfully. Full refund initiated and points restored.");

        return response;
    }

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

    private OrderSummaryResponse mapToOrderSummary(Order order) {
        OrderSummaryResponse dto = new OrderSummaryResponse();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPointsRedeemed(order.getPointsRedeemed());
        dto.setPointsDiscount(order.getPointsDiscount());
        dto.setPointsEarned(order.getPointsEarned());
        dto.setItemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        Duration age = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        boolean eligible = (order.getOrderStatus() == OrderStatus.CONFIRMED || order.getOrderStatus() == OrderStatus.PROCESSING)
                && age.toHours() < cancellationWindowHours;
        dto.setCanCancel(eligible);

        double hoursRemaining = Math.max(0.0, cancellationWindowHours - (age.toMinutes() / 60.0));
        dto.setHoursRemainingToCancel(Math.round(hoursRemaining * 10.0) / 10.0);

        return dto;
    }

    private OrderDetailResponse mapToOrderDetail(Order order) {
        OrderDetailResponse dto = new OrderDetailResponse();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPointsRedeemed(order.getPointsRedeemed());
        dto.setPointsDiscount(order.getPointsDiscount());
        dto.setPointsEarned(order.getPointsEarned());
        dto.setItemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        Duration age = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        boolean eligible = (order.getOrderStatus() == OrderStatus.CONFIRMED || order.getOrderStatus() == OrderStatus.PROCESSING)
                && age.toHours() < cancellationWindowHours;
        dto.setCanCancel(eligible);
        double hoursRemaining = Math.max(0.0, cancellationWindowHours - (age.toMinutes() / 60.0));
        dto.setHoursRemainingToCancel(Math.round(hoursRemaining * 10.0) / 10.0);

        dto.setDeliveryAddress(new AddressResponse(
                order.getDeliveryAddress().getId(),
                order.getDeliveryAddress().getRecipientName(),
                order.getDeliveryAddress().getStreetAddress(),
                order.getDeliveryAddress().getCity(),
                order.getDeliveryAddress().getState(),
                order.getDeliveryAddress().getPostalCode(),
                order.getDeliveryAddress().getCountry(),
                order.getDeliveryAddress().getIsDefault()
        ));

        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setCardLast4(order.getCardLast4());
        dto.setSubtotal(order.getSubtotal());
        dto.setTax(order.getTax());
        dto.setShippingFee(order.getShippingFee());
        dto.setCancelReason(order.getCancelReason());

        dto.setItems(order.getItems().stream().map(i -> new OrderItemResponse(
                i.getId(),
                i.getBook() != null ? i.getBook().getId() : null,
                i.getBookTitle(),
                i.getBookAuthor(),
                i.getCoverImageUrl(),
                i.getQuantity(),
                i.getUnitPrice(),
                i.getTotalPrice()
        )).collect(Collectors.toList()));

        return dto;
    }
}
