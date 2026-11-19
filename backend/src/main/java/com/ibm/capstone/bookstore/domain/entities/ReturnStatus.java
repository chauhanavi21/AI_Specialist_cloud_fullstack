package com.ibm.capstone.bookstore.domain.entities;

/**
 * Lifecycle of a return. The customer asks (REQUESTED), the carrier collects the parcel (PICKED_UP),
 * then the store receives it and the refund is processed (REFUNDED). A return the store cannot accept is REJECTED.
 */
public enum ReturnStatus {
    REQUESTED,
    PICKED_UP,
    REFUNDED,
    REJECTED;

    /** A return in one of these states still holds its books, so they cannot be returned twice. */
    public boolean isOpen() {
        return this == REQUESTED || this == PICKED_UP;
    }
}
