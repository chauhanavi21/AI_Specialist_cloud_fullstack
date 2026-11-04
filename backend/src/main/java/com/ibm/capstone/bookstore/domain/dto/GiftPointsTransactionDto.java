package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.PointsTransactionType;
import java.time.LocalDateTime;

public class GiftPointsTransactionDto {
    private Long id;
    private Integer points;
    private PointsTransactionType type;
    private String description;
    private LocalDateTime createdAt;

    public GiftPointsTransactionDto() {}

    public GiftPointsTransactionDto(Long id, Integer points, PointsTransactionType type, String description, LocalDateTime createdAt) {
        this.id = id;
        this.points = points;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public PointsTransactionType getType() { return type; }
    public void setType(PointsTransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
