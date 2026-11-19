package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderConfirmationResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal couponDiscount;
    private BigDecimal walletAmountUsed;
    private String paymentReference;
    private Integer pointsRedeemed;
    private Integer pointsEarned;
    private Integer newGiftPointsBalance;
    private BigDecimal newWalletBalance;
    private LocalDate estimatedDeliveryDate;
    private String message;
    private List<OrderItemResponse> items = new ArrayList<>();

    public OrderConfirmationResponse() {}

    public static OrderItemResponse itemFrom(OrderItem i) {
        OrderItemResponse dto = new OrderItemResponse(
                i.getId(),
                i.getBook() != null ? i.getBook().getId() : null,
                i.getBookTitle(),
                i.getBookAuthor(),
                i.getCoverImageUrl(),
                i.getQuantity(),
                i.getUnitPrice(),
                i.getTotalPrice());
        dto.setRefundedQuantity(i.getRefundedQuantity());
        return dto;
    }

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

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getWalletAmountUsed() { return walletAmountUsed; }
    public void setWalletAmountUsed(BigDecimal walletAmountUsed) { this.walletAmountUsed = walletAmountUsed; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public Integer getNewGiftPointsBalance() { return newGiftPointsBalance; }
    public void setNewGiftPointsBalance(Integer newGiftPointsBalance) { this.newGiftPointsBalance = newGiftPointsBalance; }

    public BigDecimal getNewWalletBalance() { return newWalletBalance; }
    public void setNewWalletBalance(BigDecimal newWalletBalance) { this.newWalletBalance = newWalletBalance; }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }
}
