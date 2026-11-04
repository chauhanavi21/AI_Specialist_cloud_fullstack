package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.*;

import java.util.List;

public interface OrderService {
    OrderConfirmationResponse placeOrder(Long userId, PlaceOrderRequest request);
    List<OrderSummaryResponse> getOrderHistory(Long userId);
    OrderDetailResponse getOrderById(Long userId, Long orderId);
    OrderConfirmationResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request);
    CartResponse buyItAgain(Long userId, Long orderId);
}
