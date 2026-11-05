package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.*;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders & Checkout", description = "Order placement, order history, Buy It Again, and 48-hour order cancellation")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place new order and process payment")
    public ResponseEntity<OrderConfirmationResponse> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderConfirmationResponse confirmation = orderService.placeOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }

    @PostMapping("/quote")
    @Operation(summary = "Price the current basket with an optional coupon and gift points, without placing an order")
    public ResponseEntity<StoreDto.QuoteResponse> quote(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody StoreDto.QuoteRequest request) {
        return ResponseEntity.ok(orderService.quote(principal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "Get order history for customer")
    public ResponseEntity<List<OrderSummaryResponse>> getOrderHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getOrderHistory(principal.getId()));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<OrderDetailResponse> getOrderById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(principal.getId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order within 48 hours of purchase")
    public ResponseEntity<OrderConfirmationResponse> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getId(), orderId, request));
    }

    @PostMapping(value = {"/{orderId}/reorder", "/{orderId}/buy-again"})
    @Operation(summary = "Buy It Again: 1-click re-add order items to cart")
    public ResponseEntity<CartResponse> buyItAgain(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.buyItAgain(principal.getId(), orderId));
    }
}
