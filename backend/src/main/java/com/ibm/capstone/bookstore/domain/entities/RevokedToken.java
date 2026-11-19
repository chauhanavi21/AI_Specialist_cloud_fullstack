package com.ibm.capstone.bookstore.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A JWT that was signed out before it expired. Only the token id is kept, never the token. */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(name = "token_id", length = 64)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public RevokedToken() {}

    public RevokedToken(String tokenId, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
