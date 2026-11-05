package com.ibm.capstone.bookstore.domain.entities;

public enum PointsTransactionType {
    EARNED,
    REDEEMED,
    REFUNDED,
    /** Points taken back because the order that earned them was cancelled. */
    REVOKED
}
