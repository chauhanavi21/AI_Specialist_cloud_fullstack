package com.ibm.capstone.bookstore.domain.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    public enum Type { TOP_UP, PAYMENT, REFUND }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_id")
    private Long orderId;

    /** Positive for money in (top-up, refund), negative for money out (payment). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {}

    public WalletTransaction(User user, Long orderId, BigDecimal amount, Type type, String description) {
        this.user = user;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public Type getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
