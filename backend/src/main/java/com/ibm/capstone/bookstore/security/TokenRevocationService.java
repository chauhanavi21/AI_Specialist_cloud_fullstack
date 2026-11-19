package com.ibm.capstone.bookstore.security;

import com.ibm.capstone.bookstore.domain.entities.RevokedToken;
import com.ibm.capstone.bookstore.repository.RevokedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Makes logout real for stateless JWTs: a signed-out token id is remembered until the token would have
 * expired anyway, and the authentication filter refuses it in the meantime.
 */
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String tokenId, LocalDateTime expiresAt) {
        if (tokenId != null && !repository.existsById(tokenId)) {
            repository.save(new RevokedToken(tokenId, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return tokenId != null && repository.existsById(tokenId);
    }

    /** Expired tokens are rejected on their own, so their revocation rows can go. */
    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 3_600_000L)
    @Transactional
    public void purgeExpired() {
        repository.deleteExpired(LocalDateTime.now());
    }
}
