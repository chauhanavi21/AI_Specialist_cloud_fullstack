package com.ibm.capstone.bookstore.domain.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "return_items")
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private OrderReturn orderReturn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    public ReturnItem() {}

    public ReturnItem(OrderItem orderItem, Integer quantity) {
        this.orderItem = orderItem;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public OrderReturn getOrderReturn() { return orderReturn; }
    public void setOrderReturn(OrderReturn orderReturn) { this.orderReturn = orderReturn; }
    public OrderItem getOrderItem() { return orderItem; }
    public Integer getQuantity() { return quantity; }
}
