package com.ibm.capstone.bookstore.domain.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A return request (RMA): the reverse shipment plus the refund it triggers. */
@Entity
@Table(name = "order_returns")
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rma_number", nullable = false, unique = true, length = 40)
    private String rmaNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "estimated_refund", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedRefund = BigDecimal.ZERO;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reference", length = 60)
    private String refundReference;

    @Column(name = "points_refunded", nullable = false)
    private Integer pointsRefunded = 0;

    @Column(name = "points_revoked", nullable = false)
    private Integer pointsRevoked = 0;

    @Column(name = "return_carrier", nullable = false, length = 100)
    private String returnCarrier;

    @Column(name = "return_tracking_number", nullable = false, length = 40)
    private String returnTrackingNumber;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "admin_note", length = 300)
    private String adminNote;

    @OneToMany(mappedBy = "orderReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    public OrderReturn() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRmaNumber() { return rmaNumber; }
    public void setRmaNumber(String rmaNumber) { this.rmaNumber = rmaNumber; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getEstimatedRefund() { return estimatedRefund; }
    public void setEstimatedRefund(BigDecimal estimatedRefund) { this.estimatedRefund = estimatedRefund; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String refundReference) { this.refundReference = refundReference; }
    public Integer getPointsRefunded() { return pointsRefunded; }
    public void setPointsRefunded(Integer pointsRefunded) { this.pointsRefunded = pointsRefunded; }
    public Integer getPointsRevoked() { return pointsRevoked; }
    public void setPointsRevoked(Integer pointsRevoked) { this.pointsRevoked = pointsRevoked; }
    public String getReturnCarrier() { return returnCarrier; }
    public void setReturnCarrier(String returnCarrier) { this.returnCarrier = returnCarrier; }
    public String getReturnTrackingNumber() { return returnTrackingNumber; }
    public void setReturnTrackingNumber(String returnTrackingNumber) { this.returnTrackingNumber = returnTrackingNumber; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public List<ReturnItem> getItems() { return items; }

    public void addItem(ReturnItem item) {
        items.add(item);
        item.setOrderReturn(this);
    }
}
