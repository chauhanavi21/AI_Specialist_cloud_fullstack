package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.entities.Coupon;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /** Returns the coupon if it can be applied to this subtotal, otherwise explains why not. */
    @Transactional(readOnly = true)
    public Coupon requireApplicable(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Enter a coupon code");
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Coupon '" + code.trim() + "' is not valid"));
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BadRequestException("Coupon " + coupon.getCode() + " is no longer active");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDate.now())) {
            throw new BadRequestException("Coupon " + coupon.getCode() + " has expired");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Coupon " + coupon.getCode() + " needs a minimum order of ₹"
                    + coupon.getMinOrderAmount().stripTrailingZeros().toPlainString());
        }
        return coupon;
    }
}
