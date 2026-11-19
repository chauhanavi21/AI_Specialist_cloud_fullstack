package com.ibm.capstone.bookstore.domain.entities;

public enum OrderStatus {
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    /** Every book on the order has been returned and refunded. */
    RETURNED
}
