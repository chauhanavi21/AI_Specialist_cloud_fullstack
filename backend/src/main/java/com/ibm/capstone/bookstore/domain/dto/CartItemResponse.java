package com.ibm.capstone.bookstore.domain.dto;

import java.math.BigDecimal;

public class CartItemResponse {
    private Long id;
    private BookSummaryResponse book;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public CartItemResponse() {}

    public CartItemResponse(Long id, BookSummaryResponse book, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BookSummaryResponse getBook() { return book; }
    public void setBook(BookSummaryResponse book) { this.book = book; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
