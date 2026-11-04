package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public class OrderConfirmationResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private Integer pointsRedeemed;
    private Integer pointsEarned;
    private Integer newGiftPointsBalance;
    private LocalDate estimatedDeliveryDate;
    private String message;

    public OrderConfirmationResponse() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public Integer getNewGiftPointsBalance() { return newGiftPointsBalance; }
    public void setNewGiftPointsBalance(Integer newGiftPointsBalance) { this.newGiftPointsBalance = newGiftPointsBalance; }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
