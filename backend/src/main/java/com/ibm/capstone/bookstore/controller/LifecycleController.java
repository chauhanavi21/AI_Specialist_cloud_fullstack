package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.domain.dto.OrderConfirmationResponse;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.OrderService;
import com.ibm.capstone.bookstore.service.ReturnService;
import com.ibm.capstone.bookstore.service.ShipmentService;
import com.ibm.capstone.bookstore.service.UpsellService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** What happens to an order after it is placed: edit it, track it, return it. Plus up-sell and cross-sell. */
@RestController
@Tag(name = "Order Lifecycle", description = "Modify an order, track the shipment, return books and get refunds; up-sell and cross-sell suggestions")
public class LifecycleController {

    private final OrderService orderService;
    private final ShipmentService shipmentService;
    private final ReturnService returnService;
    private final UpsellService upsellService;

    public LifecycleController(OrderService orderService, ShipmentService shipmentService,
                               ReturnService returnService, UpsellService upsellService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
        this.returnService = returnService;
        this.upsellService = upsellService;
    }

    // ---- Modify and track -------------------------------------------------------------------

    @PatchMapping("/orders/{orderId}")
    @Operation(summary = "Modify an order before it ships: change the delivery address and/or remove books (refunded)")
    public ResponseEntity<OrderConfirmationResponse> modifyOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody LifecycleDto.ModifyOrderRequest request) {
        return ResponseEntity.ok(orderService.modifyOrder(principal.getId(), orderId, request));
    }

    @GetMapping("/orders/{orderId}/shipment")
    @Operation(summary = "Track the shipment for an order")
    public ResponseEntity<LifecycleDto.ShipmentResponse> track(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.track(principal.getId(), orderId));
    }

    // ---- Returns ----------------------------------------------------------------------------

    @GetMapping("/orders/{orderId}/return-eligibility")
    @Operation(summary = "Which books on a delivered order can still be returned, and until when")
    public ResponseEntity<LifecycleDto.ReturnEligibility> eligibility(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ResponseEntity.ok(returnService.eligibility(principal.getId(), orderId));
    }

    @PostMapping("/orders/{orderId}/returns")
    @Operation(summary = "Return delivered books: creates a return with a collection tracking number")
    public ResponseEntity<LifecycleDto.ReturnResponse> requestReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody LifecycleDto.ReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.request(principal.getId(), orderId, request));
    }

    @GetMapping("/returns")
    @Operation(summary = "My returns")
    public ResponseEntity<List<LifecycleDto.ReturnResponse>> myReturns(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(returnService.mine(principal.getId()));
    }

    @GetMapping("/returns/{returnId}")
    @Operation(summary = "One of my returns")
    public ResponseEntity<LifecycleDto.ReturnResponse> myReturn(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long returnId) {
        return ResponseEntity.ok(returnService.get(principal.getId(), returnId));
    }

    @DeleteMapping("/returns/{returnId}")
    @Operation(summary = "Withdraw a return before the carrier has collected it")
    public ResponseEntity<LifecycleDto.ReturnResponse> withdrawReturn(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long returnId) {
        return ResponseEntity.ok(returnService.cancel(principal.getId(), returnId));
    }

    // ---- Up-sell and cross-sell -------------------------------------------------------------------

    @GetMapping("/books/{id}/suggestions")
    @Operation(summary = "Up-sell (a better alternative) and cross-sell (books that go with it) for a book")
    public ResponseEntity<LifecycleDto.UpsellResponse> suggestions(
            @PathVariable Long id, @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(upsellService.forBook(id, limit));
    }

    @GetMapping("/recommendations/basket")
    @Operation(summary = "Up-sell and cross-sell for the books currently in the basket")
    public ResponseEntity<LifecycleDto.UpsellResponse> basketSuggestions(
            @RequestParam List<Long> bookIds, @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(upsellService.forBasket(bookIds, limit));
    }
}
