package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PlaceOrderRequest {
    @NotNull(message = "Delivery address ID is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String cardLast4 = "4242";

    @Min(value = 0, message = "Redeemed points cannot be negative")
    private Integer pointsToRedeem = 0;

    public PlaceOrderRequest() {}

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public Integer getPointsToRedeem() { return pointsToRedeem; }
    public void setPointsToRedeem(Integer pointsToRedeem) { this.pointsToRedeem = pointsToRedeem; }
}
