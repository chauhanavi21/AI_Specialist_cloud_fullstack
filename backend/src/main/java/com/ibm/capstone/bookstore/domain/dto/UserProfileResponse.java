package com.ibm.capstone.bookstore.domain.dto;

import java.time.LocalDateTime;

public class UserProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Integer giftPointsBalance;
    private String role;
    private LocalDateTime createdAt;
    private java.math.BigDecimal walletBalance = java.math.BigDecimal.ZERO;

    public UserProfileResponse() {}

    public java.math.BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(java.math.BigDecimal walletBalance) { this.walletBalance = walletBalance; }

    public UserProfileResponse(Long id, String email, String fullName, String phoneNumber, Integer giftPointsBalance, String role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.giftPointsBalance = giftPointsBalance;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Integer getGiftPointsBalance() { return giftPointsBalance; }
    public void setGiftPointsBalance(Integer giftPointsBalance) { this.giftPointsBalance = giftPointsBalance; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
