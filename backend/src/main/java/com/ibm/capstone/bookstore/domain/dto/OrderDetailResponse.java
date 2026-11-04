package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.PaymentMethod;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailResponse extends OrderSummaryResponse {
    private AddressResponse deliveryAddress;
    private PaymentMethod paymentMethod;
    private String cardLast4;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingFee;
    private String cancelReason;
    private List<OrderItemResponse> items = new ArrayList<>();

    public OrderDetailResponse() {}

    public AddressResponse getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(AddressResponse deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }
}
