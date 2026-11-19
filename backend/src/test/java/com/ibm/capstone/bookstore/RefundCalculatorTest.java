package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.config.PricingProperties;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import com.ibm.capstone.bookstore.service.PricingCalculator;
import com.ibm.capstone.bookstore.service.RefundCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The refund maths moves money, so its invariants are tested directly rather than only through the API. */
class RefundCalculatorTest {

    private final PricingCalculator pricing = new PricingCalculator(new PricingProperties());
    private final RefundCalculator refunds = new RefundCalculator(pricing);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static OrderItem item(String price, int qty) {
        Book book = new Book();
        book.setId(1L);
        OrderItem item = new OrderItem();
        item.setBook(book);
        item.setQuantity(qty);
        item.setUnitPrice(bd(price));
        item.setTotalPrice(bd(price).multiply(BigDecimal.valueOf(qty)));
        return item;
    }

    /**
     * Two books, 899 + 749 = 1648. Coupon 100, so taxable 1548, tax 12% = 185.76, delivery free,
     * gross 1733.76. 200 points (Rs 200) were spent, so Rs 1533.76 was paid in cash.
     */
    private Order twoBookOrder() {
        Order order = new Order();
        order.addItem(item("899.00", 1));
        order.addItem(item("749.00", 1));
        order.setSubtotal(bd("1648.00"));
        order.setCouponDiscount(bd("100.00"));
        order.setTax(bd("185.76"));
        order.setShippingFee(BigDecimal.ZERO);
        order.setPointsRedeemed(200);
        order.setPointsDiscount(bd("200.00"));
        order.setTotalAmount(bd("1533.76"));
        order.setPointsEarned(15);
        return order;
    }

    @Test
    @DisplayName("A partial refund followed by the final one returns exactly what was paid, no more and no less")
    void partialThenFinalSettlesToTheRupee() {
        Order order = twoBookOrder();

        RefundCalculator.Refund first = refunds.refund(order, new int[]{1, 0}, false);
        assertTrue(first.cashBack().signum() > 0, "removing a book returns cash");
        assertTrue(first.pointsBack() > 0, "a share of the points comes back too");

        // Record the first refund as the service would, then refund what is left.
        order.getItems().get(0).setRefundedQuantity(1);
        order.setRefundedAmount(first.cashBack());
        order.setPointsRefunded(first.pointsBack());
        order.setPointsRevoked(first.pointsToRevoke());

        RefundCalculator.Refund last = refunds.refund(order, new int[]{0, 1}, true);

        assertEquals(0, order.getTotalAmount().compareTo(first.cashBack().add(last.cashBack())),
                "cash refunded across both steps equals cash paid");
        assertEquals(order.getPointsRedeemed(), first.pointsBack() + last.pointsBack(),
                "every redeemed point comes back across both steps");
        assertEquals(order.getPointsEarned(), first.pointsToRevoke() + last.pointsToRevoke(),
                "every earned point is taken back across both steps");
    }

    @Test
    @DisplayName("A refund is never larger than the book's own price plus its tax")
    void refundIsBoundedByTheBookPlusTax() {
        Order order = twoBookOrder();
        RefundCalculator.Refund r = refunds.refund(order, new int[]{1, 0}, false);

        BigDecimal ceiling = bd("899.00").add(r.taxRefund());
        assertTrue(r.grossRefund().compareTo(ceiling) <= 0);
        assertEquals(0, bd("899.00").compareTo(r.bookValue()));
    }

    @Test
    @DisplayName("Delivery is only refunded when the last book goes")
    void deliveryOnlyRefundedOnTheFinalStep() {
        Order order = twoBookOrder();
        order.setShippingFee(bd("49.00"));
        order.setTotalAmount(order.getTotalAmount().add(bd("49.00")));

        assertEquals(0, BigDecimal.ZERO.compareTo(refunds.refund(order, new int[]{1, 0}, false).shippingRefund()));
        assertEquals(0, bd("49.00").compareTo(refunds.refund(order, new int[]{1, 1}, true).shippingRefund()));
    }

    @Test
    @DisplayName("A zero-value order refunds nothing and does not divide by zero")
    void zeroOrderIsSafe() {
        Order order = new Order();
        order.addItem(item("0.00", 1));
        order.setSubtotal(BigDecimal.ZERO);
        order.setCouponDiscount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setPointsDiscount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        RefundCalculator.Refund r = refunds.refund(order, new int[]{1}, true);
        assertEquals(0, r.cashBack().signum());
        assertEquals(0, r.pointsBack());
    }
}
