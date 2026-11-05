package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.config.PricingProperties;
import com.ibm.capstone.bookstore.domain.entities.Coupon;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.repository.CouponRepository;
import com.ibm.capstone.bookstore.service.CouponService;
import com.ibm.capstone.bookstore.service.PricingCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PricingAndCouponTest {

    private PricingCalculator pricing;
    private CouponRepository couponRepository;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        pricing = new PricingCalculator(new PricingProperties());
        couponRepository = Mockito.mock(CouponRepository.class);
        couponService = new CouponService(couponRepository);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("Below the threshold: 12% tax plus Rs 49 delivery")
    void belowThreshold() {
        PricingCalculator.Breakdown b = pricing.price(bd("400"), BigDecimal.ZERO);
        assertEquals(bd("48.00"), b.tax());
        assertEquals(bd("49"), b.shippingFee());
        assertEquals(bd("497.00"), b.grossTotal());
    }

    @Test
    @DisplayName("At or above Rs 499 delivery is free")
    void freeDelivery() {
        PricingCalculator.Breakdown b = pricing.price(bd("899"), BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, b.shippingFee());
        assertEquals(bd("107.88"), b.tax());
        assertEquals(bd("1006.88"), b.grossTotal());
    }

    @Test
    @DisplayName("A coupon reduces the taxable amount and can remove free delivery")
    void couponAffectsTaxAndDelivery() {
        PricingCalculator.Breakdown b = pricing.price(bd("550"), bd("100"));
        assertEquals(bd("54.00"), b.tax());          // 12% of 450
        assertEquals(bd("49"), b.shippingFee());     // 450 < 499
        assertEquals(bd("553.00"), b.grossTotal());
    }

    @Test
    @DisplayName("Gift points: 1 point = Rs 1, 1 point earned per Rs 100")
    void points() {
        assertEquals(bd("50.00"), pricing.pointsValue(50));
        assertEquals(10, pricing.pointsNeededFor(bd("9.10")));
        assertEquals(9, pricing.pointsEarned(bd("950.88")));
    }

    @Test
    @DisplayName("Percent coupon honours its cap; flat coupon never exceeds the subtotal")
    void couponDiscount() {
        Coupon percent = coupon("WELCOME10", Coupon.DiscountType.PERCENT, "10", "0", "300");
        assertEquals(bd("300.00"), percent.discountFor(bd("5000")));
        assertEquals(bd("189.80"), percent.discountFor(bd("1898")));

        Coupon flat = coupon("BOOKS100", Coupon.DiscountType.FLAT, "100", "999", null);
        assertEquals(bd("60.00"), flat.discountFor(bd("60")));
    }

    @Test
    @DisplayName("Coupon validation rejects unknown, expired and under-minimum baskets")
    void couponRules() {
        when(couponRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> couponService.requireApplicable("NOPE", bd("1000")));

        Coupon expired = coupon("OLD", Coupon.DiscountType.FLAT, "50", "0", null);
        expired.setExpiresAt(LocalDate.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(expired));
        assertThrows(BadRequestException.class, () -> couponService.requireApplicable("OLD", bd("1000")));

        Coupon min = coupon("BOOKS100", Coupon.DiscountType.FLAT, "100", "999", null);
        when(couponRepository.findByCodeIgnoreCase("books100")).thenReturn(Optional.of(min));
        assertThrows(BadRequestException.class, () -> couponService.requireApplicable("books100", bd("500")));
        assertSame(min, couponService.requireApplicable("books100", bd("1500")));
    }

    private Coupon coupon(String code, Coupon.DiscountType type, String value, String min, String max) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(code);
        c.setDiscountType(type);
        c.setDiscountValue(bd(value));
        c.setMinOrderAmount(bd(min));
        c.setMaxDiscount(max == null ? null : bd(max));
        c.setActive(true);
        return c;
    }
}
