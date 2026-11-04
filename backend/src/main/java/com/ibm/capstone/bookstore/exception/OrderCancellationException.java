package com.ibm.capstone.bookstore.exception;

public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(String message) {
        super(message);
    }
}
