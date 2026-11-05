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

    @Value("${app.orders.cancellation-window-hours:48}")
    private long cancellationWindowHours = 48;

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
                            PaymentGateway paymentGateway) {
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
        PricingCalculator.Breakdown price = pricing.price(subtotal, couponDiscount);

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
        order.setPointsRedeemed(pointsToRedeem);
        order.setPointsDiscount(pointsDiscount);
        order.setPointsEarned(pointsEarned);
        order.setWalletAmountUsed(walletAmountUsed);
        order.setTotalAmount(payable);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCardLast4(cardLast4);
        order.setPaymentReference(paymentReference);

        int maxDeliveryDays = cart.getItems().stream()
                .mapToInt(i -> i.getBook().getTentativeDeliveryDays())
                .max().orElse(2);
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(maxDeliveryDays));

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

        // 6. Ledgers.
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

        // 7. Clear the cart.
        cartService.clearCart(userId);

        OrderConfirmationResponse response = baseResponse(savedOrder, user);
        response.setMessage("Payment successful and order confirmed! Your books are being prepared for dispatch.");
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

        // 48-hour business constraint
        Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        if (duration.toHours() >= cancellationWindowHours) {
            throw new OrderCancellationException("Cancellation window expired. Orders can only be cancelled within " + cancellationWindowHours + " hours of purchase.");
        }

        // 1. Mark cancelled and refunded
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request != null && request.getReason() != null ? request.getReason() : "Customer cancelled within 48 hours");

        // 2. Restore stock
        for (OrderItem item : order.getItems()) {
            if (item.getBook() != null) {
                Book book = item.getBook();
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                book.setCopiesSold(Math.max(0, book.getCopiesSold() - item.getQuantity()));
                bookRepository.save(book);
            }
        }

        // 3. Restore redeemed points, revoke earned points, return wallet money
        User user = order.getUser();
        if (order.getPointsRedeemed() > 0) {
            user.setGiftPointsBalance(user.getGiftPointsBalance() + order.getPointsRedeemed());
            giftPointsService.recordTransaction(user, order.getId(), order.getPointsRedeemed(),
                    PointsTransactionType.REFUNDED, "Refunded points from cancelled Order #" + order.getOrderNumber());
        }
        if (order.getPointsEarned() > 0) {
            int revoked = Math.min(order.getPointsEarned(), user.getGiftPointsBalance());
            user.setGiftPointsBalance(user.getGiftPointsBalance() - revoked);
            if (revoked > 0) {
                giftPointsService.recordTransaction(user, order.getId(), revoked,
                        PointsTransactionType.REVOKED, "Points reversed for cancelled Order #" + order.getOrderNumber());
            }
        }
        userRepository.save(user);

        if (order.getWalletAmountUsed() != null && order.getWalletAmountUsed().signum() > 0) {
            walletService.credit(user, order.getWalletAmountUsed(), order.getId(),
                    "Refund for cancelled Order #" + order.getOrderNumber());
        }

        Order savedOrder = orderRepository.save(order);

        OrderConfirmationResponse response = baseResponse(savedOrder, user);
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
        dto.setItemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());

        Duration age = Duration.between(order.getCreatedAt(), LocalDateTime.now());
        boolean eligible = (order.getOrderStatus() == OrderStatus.CONFIRMED || order.getOrderStatus() == OrderStatus.PROCESSING)
                && age.toHours() < cancellationWindowHours;
        dto.setCanCancel(eligible);

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
        dto.setItems(order.getItems().stream().map(OrderConfirmationResponse::itemFrom).collect(Collectors.toList()));
        return dto;
    }
}
