package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderSummaryResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private Integer pointsRedeemed;
    private BigDecimal pointsDiscount;
    private Integer pointsEarned;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDate estimatedDeliveryDate;
    private Boolean canCancel;
    private Double hoursRemainingToCancel;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private String shippingZone;
    private String shipmentStatus;
    private Boolean canModify = false;
    private Boolean canReturn = false;
    private LocalDate returnDeadline;

    public OrderSummaryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public BigDecimal getPointsDiscount() { return pointsDiscount; }
    public void setPointsDiscount(BigDecimal pointsDiscount) { this.pointsDiscount = pointsDiscount; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }

    public Boolean getCanCancel() { return canCancel; }
    public void setCanCancel(Boolean canCancel) { this.canCancel = canCancel; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public String getShippingZone() { return shippingZone; }
    public void setShippingZone(String shippingZone) { this.shippingZone = shippingZone; }

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String shipmentStatus) { this.shipmentStatus = shipmentStatus; }

    public Boolean getCanModify() { return canModify; }
    public void setCanModify(Boolean canModify) { this.canModify = canModify; }

    public Boolean getCanReturn() { return canReturn; }
    public void setCanReturn(Boolean canReturn) { this.canReturn = canReturn; }

    public LocalDate getReturnDeadline() { return returnDeadline; }
    public void setReturnDeadline(LocalDate returnDeadline) { this.returnDeadline = returnDeadline; }

    public Double getHoursRemainingToCancel() { return hoursRemainingToCancel; }
    public void setHoursRemainingToCancel(Double hoursRemainingToCancel) { this.hoursRemainingToCancel = hoursRemainingToCancel; }
}
