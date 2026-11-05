package com.ibm.capstone.bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Store-wide pricing rules. All money is in Indian rupees (INR).
 * Bound from {@code app.pricing.*} in application.yml; the defaults apply when a key is absent.
 */
@Component
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    /** GST-style tax applied to the discounted subtotal. */
    private BigDecimal taxRate = new BigDecimal("0.12");

    /** Orders at or above this discounted subtotal ship free. */
    private BigDecimal freeShippingThreshold = new BigDecimal("499");

    /** Flat delivery charge below the free-shipping threshold. */
    private BigDecimal shippingFee = new BigDecimal("49");

    /** Rupees knocked off per gift point redeemed. */
    private BigDecimal rupeesPerPoint = BigDecimal.ONE;

    /** One gift point is earned for every this-many rupees paid. */
    private BigDecimal earnRupeesPerPoint = new BigDecimal("100");

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
    public void setFreeShippingThreshold(BigDecimal freeShippingThreshold) { this.freeShippingThreshold = freeShippingThreshold; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getRupeesPerPoint() { return rupeesPerPoint; }
    public void setRupeesPerPoint(BigDecimal rupeesPerPoint) { this.rupeesPerPoint = rupeesPerPoint; }

    public BigDecimal getEarnRupeesPerPoint() { return earnRupeesPerPoint; }
    public void setEarnRupeesPerPoint(BigDecimal earnRupeesPerPoint) { this.earnRupeesPerPoint = earnRupeesPerPoint; }
}
