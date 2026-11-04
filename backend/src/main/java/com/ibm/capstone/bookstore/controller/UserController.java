package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.AddressRequest;
import com.ibm.capstone.bookstore.domain.dto.AddressResponse;
import com.ibm.capstone.bookstore.domain.dto.GiftPointsSummaryResponse;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import com.ibm.capstone.bookstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users & Loyalty", description = "Address management and gift points balance")
public class UserController {

    private final UserService userService;
    private final GiftPointsService giftPointsService;

    public UserController(UserService userService, GiftPointsService giftPointsService) {
        this.userService = userService;
        this.giftPointsService = giftPointsService;
    }

    @GetMapping("/addresses")
    @Operation(summary = "Get user delivery addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUserAddresses(principal.getId()));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add delivery address")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = userService.addAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/points")
    @Operation(summary = "Get gift points summary and transaction history")
    public ResponseEntity<GiftPointsSummaryResponse> getGiftPoints(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(giftPointsService.getGiftPointsSummary(principal.getId()));
    }
}
