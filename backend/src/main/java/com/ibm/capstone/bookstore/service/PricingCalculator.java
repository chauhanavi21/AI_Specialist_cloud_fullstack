package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.config.PricingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for tax, delivery and loyalty maths, used by both the
 * cart estimate and order placement so the two can never disagree.
 */
@Component
public class PricingCalculator {

    private final PricingProperties props;
    private final ShippingCalculator shippingRates;

    public PricingCalculator(PricingProperties props) {
        this.props = props;
        this.shippingRates = new ShippingCalculator(props);
    }

    public record Breakdown(BigDecimal subtotal, BigDecimal couponDiscount, BigDecimal tax,
                            BigDecimal shippingFee, BigDecimal grossTotal) {}

    /** Prices an order for the default (regional) zone, used by the basket before an address is chosen. */
    public Breakdown price(BigDecimal subtotal, BigDecimal couponDiscount) {
        return price(subtotal, couponDiscount, com.ibm.capstone.bookstore.domain.entities.ShippingZone.REGIONAL);
    }

    /** Prices an order. Tax and the free-delivery test apply to the subtotal after the coupon; the charge depends on the zone. */
    public Breakdown price(BigDecimal subtotal, BigDecimal couponDiscount,
                           com.ibm.capstone.bookstore.domain.entities.ShippingZone zone) {
        BigDecimal discount = couponDiscount == null ? BigDecimal.ZERO : couponDiscount;
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(props.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = (subtotal.signum() == 0 || taxable.compareTo(props.getFreeShippingThreshold()) >= 0)
                ? BigDecimal.ZERO
                : shippingRates.feeFor(zone);
        return new Breakdown(subtotal, discount, tax, shipping, taxable.add(tax).add(shipping));
    }

    /** Rupee value of the given number of points. */
    public BigDecimal pointsValue(int points) {
        return props.getRupeesPerPoint().multiply(BigDecimal.valueOf(points)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fewest points needed to cover the given rupee amount. */
    public int pointsNeededFor(BigDecimal amount) {
        return amount.divide(props.getRupeesPerPoint(), 0, RoundingMode.CEILING).intValue();
    }

    /** Points earned on a paid amount. */
    public int pointsEarned(BigDecimal amountPaid) {
        return amountPaid.divide(props.getEarnRupeesPerPoint(), 0, RoundingMode.FLOOR).intValue();
    }

    public BigDecimal getFreeShippingThreshold() {
        return props.getFreeShippingThreshold();
    }
}
