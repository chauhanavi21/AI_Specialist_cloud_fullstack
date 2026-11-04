package com.ibm.capstone.bookstore.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponse {
    private Long id;
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer itemCount = 0;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal estimatedTax = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public CartResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getEstimatedTax() { return estimatedTax; }
    public void setEstimatedTax(BigDecimal estimatedTax) { this.estimatedTax = estimatedTax; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
