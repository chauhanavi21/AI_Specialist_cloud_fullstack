package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.domain.dto.CancelOrderRequest;
import com.ibm.capstone.bookstore.domain.dto.OrderConfirmationResponse;
import com.ibm.capstone.bookstore.domain.dto.PlaceOrderRequest;
import com.ibm.capstone.bookstore.domain.entities.*;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.OrderCancellationException;
import com.ibm.capstone.bookstore.repository.*;
import com.ibm.capstone.bookstore.service.CartService;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import com.ibm.capstone.bookstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private GiftPointsService giftPointsService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Book testBook;
    private Address testAddress;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "cancellationWindowHours", 48L);
        ReflectionTestUtils.setField(orderService, "dollarsPerPointRedeemed", 0.10);

        testUser = new User("alex.dev@ibm.com", "hashedpass", "Alex Rivera", "+1-555-0144");
        testUser.setId(1L);
        testUser.setGiftPointsBalance(200);

        testBook = new Book();
        testBook.setId(10L);
        testBook.setTitle("Cloud Native Spring in Action");
        testBook.setAuthor("Thomas Vitale");
        testBook.setPrice(BigDecimal.valueOf(50.00));
        testBook.setOriginalPrice(BigDecimal.valueOf(60.00));
        testBook.setStockQuantity(20);
        testBook.setTentativeDeliveryDays(2);

        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setRecipientName("Alex Rivera");
        testAddress.setStreetAddress("100 IBM Innovation Way");
        testAddress.setCity("Armonk");
        testAddress.setState("NY");
        testAddress.setPostalCode("10504");
        testAddress.setCountry("United States");

        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setOrderNumber("BK-20261104-1234");
        testOrder.setUser(testUser);
        testOrder.setDeliveryAddress(testAddress);
        testOrder.setOrderStatus(OrderStatus.CONFIRMED);
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        testOrder.setSubtotal(BigDecimal.valueOf(50.00));
        testOrder.setTax(BigDecimal.valueOf(4.00));
        testOrder.setShippingFee(BigDecimal.ZERO);
        testOrder.setPointsRedeemed(50);
        testOrder.setPointsDiscount(BigDecimal.valueOf(5.00));
        testOrder.setPointsEarned(5);
        testOrder.setTotalAmount(BigDecimal.valueOf(49.00));
        testOrder.setEstimatedDeliveryDate(LocalDate.now().plusDays(2));
        testOrder.setCreatedAt(LocalDateTime.now().minusHours(2)); // 2 hours old

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder);
        orderItem.setBook(testBook);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(BigDecimal.valueOf(50.00));
        orderItem.setTotalPrice(BigDecimal.valueOf(50.00));
        testOrder.addItem(orderItem);
    }

    @Test
    @DisplayName("Should successfully cancel order within 48-hour window and restore points & stock")
    void testCancelOrder_SuccessWithin48Hours() {
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CancelOrderRequest req = new CancelOrderRequest("Changed my mind");
        OrderConfirmationResponse response = orderService.cancelOrder(1L, 100L, req);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, testOrder.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, testOrder.getPaymentStatus());
        // Verify stock was restored (+1)
        assertEquals(21, testBook.getStockQuantity());
        // Verify points were restored
        verify(giftPointsService, times(1)).recordTransaction(eq(testUser), eq(100L), eq(50), eq(PointsTransactionType.REFUNDED), anyString());
    }

    @Test
    @DisplayName("Should reject order cancellation when order is older than 48 hours")
    void testCancelOrder_ThrowsExceptionWhenOver48Hours() {
        // Set creation time to 50 hours ago
        testOrder.setCreatedAt(LocalDateTime.now().minusHours(50));
        when(orderRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testOrder));

        CancelOrderRequest req = new CancelOrderRequest("Too late cancellation");
        assertThrows(OrderCancellationException.class, () -> orderService.cancelOrder(1L, 100L, req));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should place order and apply gift points discount correctly")
    void testPlaceOrder_SuccessWithPointsRedemption() {
        Cart cart = new Cart(testUser);
        cart.setId(5L);
        CartItem cartItem = new CartItem(cart, testBook, 1, testBook.getPrice());
        cart.addItem(cartItem);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testAddress));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(200L);
            return o;
        });

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(1L);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPointsToRedeem(50); // $5 discount

        OrderConfirmationResponse confirmation = orderService.placeOrder(1L, request);

        assertNotNull(confirmation);
        assertEquals(19, testBook.getStockQuantity()); // Stock decremented by 1
        verify(cartService, times(1)).clearCart(1L);
        verify(giftPointsService, atLeastOnce()).recordTransaction(eq(testUser), any(), anyInt(), any(), anyString());
    }
}
