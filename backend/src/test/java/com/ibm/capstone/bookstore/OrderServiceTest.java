package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.config.PricingProperties;
import com.ibm.capstone.bookstore.domain.dto.CancelOrderRequest;
import com.ibm.capstone.bookstore.domain.dto.OrderConfirmationResponse;
import com.ibm.capstone.bookstore.domain.dto.PlaceOrderRequest;
import com.ibm.capstone.bookstore.domain.entities.*;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.OrderCancellationException;
import com.ibm.capstone.bookstore.repository.*;
import com.ibm.capstone.bookstore.service.CartService;
import com.ibm.capstone.bookstore.service.CouponService;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import com.ibm.capstone.bookstore.service.PaymentGateway;
import com.ibm.capstone.bookstore.service.PricingCalculator;
import com.ibm.capstone.bookstore.service.RefundCalculator;
import com.ibm.capstone.bookstore.service.RefundProcessor;
import com.ibm.capstone.bookstore.service.ReturnPolicy;
import com.ibm.capstone.bookstore.service.ShippingCalculator;
import com.ibm.capstone.bookstore.service.WalletService;
import com.ibm.capstone.bookstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private GiftPointsService giftPointsService;
    @Mock private CartService cartService;
    @Mock private CouponRepository couponRepository;
    @Mock private WalletService walletService;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private StoreRepository storeRepository;

    private OrderServiceImpl orderService;
    private User testUser;
    private Book testBook;
    private Address testAddress;
    private Order testOrder;
    private Cart cart;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        PricingProperties props = new PricingProperties();
        PricingCalculator pricing = new PricingCalculator(props);
        RefundProcessor refundProcessor = new RefundProcessor(bookRepository, userRepository, giftPointsService, walletService);
        orderService = new OrderServiceImpl(orderRepository, cartRepository, addressRepository, userRepository,
                bookRepository, giftPointsService, cartService, pricing,
                new CouponService(couponRepository), walletService, new PaymentGateway(),
                new ShippingCalculator(props), shipmentRepository,
                new RefundCalculator(pricing), refundProcessor, new ReturnPolicy(storeRepository));
        ReflectionTestUtils.setField(orderService, "cancellationWindowHours", 48L);
        ReflectionTestUtils.setField(orderService, "modificationWindowHours", 24L);

        testUser = new User("alex.dev@ibm.com", "hashedpass", "Alex Rivera", "+91 98100 00144");
        testUser.setId(1L);
        testUser.setGiftPointsBalance(200);
        testUser.setWalletBalance(bd("2000.00"));

        testBook = new Book();
        testBook.setId(10L);
        testBook.setTitle("Cloud Native Spring in Action");
        testBook.setAuthor("Thomas Vitale");
        testBook.setPrice(bd("899.00"));
        testBook.setOriginalPrice(bd("999.00"));
        testBook.setStockQuantity(20);
        testBook.setCopiesSold(100);
        testBook.setTentativeDeliveryDays(2);

        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setRecipientName("Alex Rivera");
        testAddress.setStreetAddress("IBM India");
        testAddress.setCity("Bengaluru");
        testAddress.setState("Karnataka");
        testAddress.setPostalCode("560071");
        testAddress.setCountry("India");

        cart = new Cart(testUser);
        cart.setId(5L);
        cart.addItem(new CartItem(cart, testBook, 1, testBook.getPrice()));

        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setOrderNumber("BK-20261104-123456");
        testOrder.setUser(testUser);
        testOrder.setDeliveryAddress(testAddress);
        testOrder.setOrderStatus(OrderStatus.CONFIRMED);
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        testOrder.setShippingZone(ShippingZone.REGIONAL);
        testOrder.setSubtotal(bd("899.00"));
        testOrder.setTax(bd("107.88"));
        testOrder.setShippingFee(BigDecimal.ZERO);
        testOrder.setPointsRedeemed(50);
        testOrder.setPointsDiscount(bd("50.00"));
        testOrder.setPointsEarned(9);
        testOrder.setWalletAmountUsed(bd("956.88"));
        testOrder.setTotalAmount(bd("956.88"));
        testOrder.setEstimatedDeliveryDate(LocalDate.now().plusDays(2));
        testOrder.setCreatedAt(LocalDateTime.now().minusHours(2));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder);
        orderItem.setBook(testBook);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(bd("899.00"));
        orderItem.setTotalPrice(bd("899.00"));
        testOrder.addItem(orderItem);
    }

    private void stubCheckoutLookups() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    }

    private void stubOrderSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(200L);
            return o;
        });
    }

    private PlaceOrderRequest cardRequest(String number, int points) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setCardNumber(number);
        request.setCardName("Alex Rivera");
        request.setCardExpiry(YearMonth.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy")));
        request.setCardCvv("123");
        request.setPointsToRedeem(points);
        return request;
    }

    // ---- Placing orders ----

    @Test
    @DisplayName("Places an order with INR tax, free delivery, points redemption and earned points")
    void placeOrderWithPoints() {
        stubCheckoutLookups();
        stubOrderSave();

        OrderConfirmationResponse confirmation = orderService.placeOrder(1L, cardRequest("4242 4242 4242 4242", 50));

        // 899 + 12% tax (107.88) = 1006.88, minus 50 points (Rs 50) = 956.88 payable
        assertEquals(bd("956.88"), confirmation.getTotalAmount());
        assertEquals(50, confirmation.getPointsRedeemed());
        assertEquals(9, confirmation.getPointsEarned());          // floor(956.88 / 100)
        assertEquals(200 - 50 + 9, testUser.getGiftPointsBalance());
        assertEquals(19, testBook.getStockQuantity());
        assertEquals(101, testBook.getCopiesSold());
        assertTrue(confirmation.getPaymentReference().startsWith("CRD-"));
        assertEquals(1, confirmation.getItems().size());
        verify(cartService).clearCart(1L);
        verify(giftPointsService).recordTransaction(eq(testUser), eq(200L), eq(50), eq(PointsTransactionType.REDEEMED), anyString());
        verify(giftPointsService).recordTransaction(eq(testUser), eq(200L), eq(9), eq(PointsTransactionType.EARNED), anyString());
    }

    @Test
    @DisplayName("A coupon is applied before tax and recorded on the order")
    void placeOrderWithCoupon() {
        stubCheckoutLookups();
        stubOrderSave();
        Coupon coupon = new Coupon();
        coupon.setCode("WELCOME10");
        coupon.setDescription("10% off");
        coupon.setDiscountType(Coupon.DiscountType.PERCENT);
        coupon.setDiscountValue(bd("10"));
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxDiscount(bd("300"));
        coupon.setActive(true);
        when(couponRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(coupon));

        PlaceOrderRequest request = cardRequest("4242424242424242", 0);
        request.setCouponCode("WELCOME10");
        OrderConfirmationResponse confirmation = orderService.placeOrder(1L, request);

        // 899 - 89.90 = 809.10; tax 12% = 97.09; free delivery (>= 499); total 906.19
        assertEquals(bd("89.90"), confirmation.getCouponDiscount());
        assertEquals(bd("906.19"), confirmation.getTotalAmount());
    }

    @Test
    @DisplayName("A declined card leaves stock, points and cart untouched")
    void declinedCardChangesNothing() {
        stubCheckoutLookups();

        assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, cardRequest("4000 0000 0000 0002", 50)));

        assertEquals(20, testBook.getStockQuantity());
        assertEquals(200, testUser.getGiftPointsBalance());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyLong());
    }

    @Test
    @DisplayName("Wallet payment debits the wallet and fails cleanly when the balance is short")
    void walletPayment() {
        stubCheckoutLookups();
        stubOrderSave();
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);
        request.setPaymentMethod(PaymentMethod.WALLET);

        OrderConfirmationResponse confirmation = orderService.placeOrder(1L, request);
        assertEquals(bd("1006.88"), confirmation.getWalletAmountUsed());
        verify(walletService).debit(eq(testUser), eq(bd("1006.88")), eq(200L), anyString());
    }

    @Test
    @DisplayName("Wallet payment is rejected when the balance is insufficient")
    void walletInsufficient() {
        stubCheckoutLookups();
        testUser.setWalletBalance(bd("100.00"));
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);
        request.setPaymentMethod(PaymentMethod.WALLET);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, request));
        assertTrue(ex.getMessage().contains("wallet"));
        assertEquals(20, testBook.getStockQuantity());
        verify(walletService, never()).debit(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Rejects an order that asks for more points than the customer has")
    void tooManyPoints() {
        stubCheckoutLookups();
        assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, cardRequest("4242424242424242", 500)));
        assertEquals(20, testBook.getStockQuantity());
    }

    @Test
    @DisplayName("Rejects an order when stock is short, without charging")
    void outOfStock() {
        stubCheckoutLookups();
        testBook.setStockQuantity(0);
        assertThrows(BadRequestException.class, () -> orderService.placeOrder(1L, cardRequest("4242424242424242", 0)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ---- Cancelling orders ----

    @Test
    @DisplayName("Cancels inside 48 hours: restores stock, returns points and wallet money, revokes earned points")
    void cancelWithin48Hours() {
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderConfirmationResponse response = orderService.cancelOrder(1L, 100L, new CancelOrderRequest("Changed my mind"));

        assertEquals(OrderStatus.CANCELLED, testOrder.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
        assertEquals(21, testBook.getStockQuantity());
        // 200 + 50 redeemed returned - 9 earned revoked
        assertEquals(241, testUser.getGiftPointsBalance());
        assertNotNull(response.getMessage());
        verify(giftPointsService).recordTransaction(eq(testUser), eq(100L), eq(50), eq(PointsTransactionType.REFUNDED), anyString());
        verify(giftPointsService).recordTransaction(eq(testUser), eq(100L), eq(9), eq(PointsTransactionType.REVOKED), anyString());
        verify(walletService).credit(eq(testUser), eq(bd("956.88")), eq(100L), anyString());
    }

    @Test
    @DisplayName("Rejects cancellation after the 48 hour window")
    void cancelAfter48Hours() {
        testOrder.setCreatedAt(LocalDateTime.now().minusHours(50));
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testOrder));

        assertThrows(OrderCancellationException.class, () -> orderService.cancelOrder(1L, 100L, new CancelOrderRequest("Too late")));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Rejects cancelling an order that is already cancelled")
    void cancelTwice() {
        testOrder.setOrderStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testOrder));
        assertThrows(OrderCancellationException.class, () -> orderService.cancelOrder(1L, 100L, null));
    }
}
