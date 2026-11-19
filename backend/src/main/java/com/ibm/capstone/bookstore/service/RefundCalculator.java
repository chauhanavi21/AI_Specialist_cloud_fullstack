package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Works out how much of an order's price comes back when some of its books are removed or returned.
 *
 * The rule is proportional: the books being refunded carry their share of the coupon discount, the tax,
 * and the gift points that were spent on the order. Delivery is only refunded when the whole order is
 * cancelled or when no book is left on it. The last refund on an order always takes whatever is left, so
 * rounding can never leave pennies behind or pay out more than was charged.
 */
@Component
public class RefundCalculator {

    private final PricingCalculator pricing;

    public RefundCalculator(PricingCalculator pricing) {
        this.pricing = pricing;
    }

    /** What a refund of some units is worth, split into the pieces that are paid back. */
    public record Refund(BigDecimal bookValue, BigDecimal couponShare, BigDecimal taxRefund,
                         BigDecimal shippingRefund, BigDecimal grossRefund,
                         int pointsBack, BigDecimal cashBack, int pointsToRevoke) {}

    /**
     * @param order     the order being refunded from
     * @param units     for each order item, the number of units being refunded now (parallel to order.getItems())
     * @param finalStep true if nothing will remain on the order after this refund, so the remainder is settled in full
     */
    public Refund refund(Order order, int[] units, boolean finalStep) {
        BigDecimal bookValue = BigDecimal.ZERO;
        for (int i = 0; i < units.length; i++) {
            OrderItem item = order.getItems().get(i);
            bookValue = bookValue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(units[i])));
        }
        bookValue = bookValue.setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = order.getSubtotal();
        BigDecimal couponShare = subtotal.signum() == 0 ? BigDecimal.ZERO
                : order.getCouponDiscount().multiply(bookValue).divide(subtotal, 2, RoundingMode.HALF_UP);
        BigDecimal taxRefund = subtotal.signum() == 0 ? BigDecimal.ZERO
                : order.getTax().multiply(bookValue).divide(subtotal, 2, RoundingMode.HALF_UP);

        // Everything the customer paid for the order, including delivery, before points were applied.
        BigDecimal gross = subtotal.subtract(order.getCouponDiscount()).add(order.getTax()).add(order.getShippingFee());
        BigDecimal shippingRefund = finalStep ? order.getShippingFee() : BigDecimal.ZERO;

        BigDecimal grossRefund = bookValue.subtract(couponShare).add(taxRefund).add(shippingRefund);

        // Points and cash were both used to pay `gross`. Points come back first, in proportion to their share.
        int pointsBack;
        BigDecimal cashBack;
        if (finalStep) {
            // Settle exactly what is still outstanding: nothing more than was paid, nothing left behind.
            pointsBack = order.getPointsRedeemed() - order.getPointsRefunded();
            cashBack = order.getTotalAmount().subtract(order.getRefundedAmount()).max(BigDecimal.ZERO);
        } else {
            BigDecimal pointsValueUsed = order.getPointsDiscount();
            BigDecimal share = gross.signum() == 0 ? BigDecimal.ZERO
                    : grossRefund.divide(gross, 6, RoundingMode.HALF_UP);
            BigDecimal pointsValueBack = pointsValueUsed.multiply(share).setScale(2, RoundingMode.DOWN);
            pointsBack = Math.min(pricing.pointsNeededFor(pointsValueBack), order.getPointsRedeemed() - order.getPointsRefunded());
            pointsBack = Math.max(0, pointsBack);
            cashBack = grossRefund.subtract(pricing.pointsValue(pointsBack))
                    .min(order.getTotalAmount().subtract(order.getRefundedAmount()))
                    .max(BigDecimal.ZERO);
        }

        // Earned points shrink with the refunded value, never below zero and never beyond what is still unrevoked.
        int pointsToRevoke;
        if (finalStep) {
            pointsToRevoke = order.getPointsEarned() - order.getPointsRevoked();
        } else {
            BigDecimal paid = order.getTotalAmount();
            BigDecimal paidShare = paid.signum() == 0 ? BigDecimal.ZERO
                    : cashBack.divide(paid, 6, RoundingMode.HALF_UP);
            pointsToRevoke = BigDecimal.valueOf(order.getPointsEarned()).multiply(paidShare)
                    .setScale(0, RoundingMode.CEILING).intValue();
            pointsToRevoke = Math.min(pointsToRevoke, order.getPointsEarned() - order.getPointsRevoked());
        }
        pointsToRevoke = Math.max(0, pointsToRevoke);

        return new Refund(bookValue, couponShare, taxRefund, shippingRefund, grossRefund,
                pointsBack, cashBack, pointsToRevoke);
    }
}
