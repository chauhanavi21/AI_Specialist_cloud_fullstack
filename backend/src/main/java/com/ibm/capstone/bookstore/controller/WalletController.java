package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.domain.entities.Coupon;
import com.ibm.capstone.bookstore.repository.CartRepository;
import com.ibm.capstone.bookstore.repository.CouponRepository;
import com.ibm.capstone.bookstore.domain.entities.CartItem;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.CouponService;
import com.ibm.capstone.bookstore.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Wallet & Coupons", description = "Stored-value wallet and checkout coupons")
public class WalletController {

    private final WalletService walletService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;

    public WalletController(WalletService walletService, CouponService couponService,
                            CouponRepository couponRepository, CartRepository cartRepository) {
        this.walletService = walletService;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
        this.cartRepository = cartRepository;
    }

    @GetMapping("/wallet")
    @Operation(summary = "Wallet balance and transactions")
    public ResponseEntity<StoreDto.WalletResponse> getWallet(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getWallet(principal.getId()));
    }

    @PostMapping("/wallet/top-up")
    @Operation(summary = "Add money to the wallet (simulated; no real payment is taken)")
    public ResponseEntity<StoreDto.WalletResponse> topUp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoreDto.TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(principal.getId(), request.amount()));
    }

    @GetMapping("/coupons")
    @Operation(summary = "Active coupon codes")
    public ResponseEntity<List<StoreDto.CouponResponse>> listCoupons() {
        return ResponseEntity.ok(couponRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(c -> new StoreDto.CouponResponse(c.getCode(), c.getDescription(), null))
                .toList());
    }

    @PostMapping("/coupons/validate")
    @Operation(summary = "Check a coupon against the current basket and return the discount it gives")
    public ResponseEntity<StoreDto.CouponResponse> validate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoreDto.CouponValidateRequest request) {
        BigDecimal subtotal = cartRepository.findByUserId(principal.getId())
                .map(cart -> cart.getItems().stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
        if (subtotal.signum() == 0) {
            throw new BadRequestException("Add books to your basket before applying a coupon");
        }
        Coupon coupon = couponService.requireApplicable(request.code(), subtotal);
        return ResponseEntity.ok(new StoreDto.CouponResponse(coupon.getCode(), coupon.getDescription(), coupon.discountFor(subtotal)));
    }
}
