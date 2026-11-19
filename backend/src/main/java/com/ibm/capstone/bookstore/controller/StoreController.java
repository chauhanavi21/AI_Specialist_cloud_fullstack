package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.AdminDto;
import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.service.ShipmentService;
import com.ibm.capstone.bookstore.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public, read-only: the store front, its policies, and what delivery costs. No sign-in needed. */
@RestController
@Tag(name = "Stores & Shipping", description = "Store information, store policies and delivery rate quotes (public)")
public class StoreController {

    private final StoreService storeService;
    private final ShipmentService shipmentService;

    public StoreController(StoreService storeService, ShipmentService shipmentService) {
        this.storeService = storeService;
        this.shipmentService = shipmentService;
    }

    @GetMapping("/stores")
    @Operation(summary = "List the open stores with their policies")
    public ResponseEntity<List<AdminDto.StoreResponse>> stores() {
        return ResponseEntity.ok(storeService.activeStores());
    }

    @GetMapping("/stores/{id}")
    @Operation(summary = "One store with its policies")
    public ResponseEntity<AdminDto.StoreResponse> store(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getActive(id));
    }

    @PostMapping("/shipping/quote")
    @Operation(summary = "Delivery charge, zone and extra transit time for a city and state")
    public ResponseEntity<LifecycleDto.ShippingQuoteResponse> shippingQuote(@Valid @RequestBody LifecycleDto.ShippingQuoteRequest request) {
        return ResponseEntity.ok(shipmentService.quote(request));
    }
}
