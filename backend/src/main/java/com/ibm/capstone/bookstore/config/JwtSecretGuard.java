package com.ibm.capstone.bookstore.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Refuses to start a production-like deployment (the {@code postgres} profile) with a token-signing secret that anyone
 * reading the repository already knows.
 *
 * The default secret in {@code application.yml} exists so that {@code ./mvnw spring-boot:run} works with no setup. It is
 * public, so any deployment that keeps it would accept forged tokens, including administrator ones. The demo profile (H2)
 * is allowed to use it; a deployment that talks to a real database is not.
 */
@Component
@Profile("postgres")
public class JwtSecretGuard {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    /** The value shipped in application.yml and docker-compose.yml. */
    static final String PUBLISHED_DEMO_SECRET =
            "4c617073746f6e6553757065725365637572654a57545369676e696e674b65793230323649424d426f625370656369616c697374";

    /** HS512 needs at least 64 bytes to be as strong as the algorithm; 32 is the floor the JWT library accepts. */
    static final int MIN_SECRET_BYTES = 32;

    /** A run of this many identical characters cannot be random: the chance in 64 random base64 characters is about 1 in 10^7. */
    static final int MAX_RUN = 8;

    private final String secret;

    public JwtSecretGuard(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret == null ? "" : secret.strip();
    }

    @PostConstruct
    void check() {
        if (PUBLISHED_DEMO_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still the demo value published in the repository. Set JWT_SECRET to your own random value, "
                            + "for example: openssl rand -base64 64");
        }
        int length;
        try {
            length = Base64.getDecoder().decode(secret).length;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET must be base64 text. Generate one with: openssl rand -base64 64", e);
        }
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET is too short (" + length + " bytes). Use at least " + MIN_SECRET_BYTES
                    + " bytes, for example: openssl rand -base64 64");
        }
        if (looksGuessable(secret)) {
            throw new IllegalStateException("JWT_SECRET looks guessable (it repeats a short pattern or uses very few different characters), "
                    + "so anyone could forge tokens. Use random bytes, for example: openssl rand -base64 64");
        }
        log.info("JWT signing secret accepted ({} bytes)", length);
    }

    /**
     * A length check alone accepts "ChangeMeChangeMeChangeMe...". Real random base64 uses most of its alphabet and does not repeat,
     * so a secret that uses few different characters, or is one short block repeated, is a placeholder, not a key.
     */
    static boolean looksGuessable(String secret) {
        long distinct = secret.chars().distinct().count();
        if (distinct < 16) {
            return true;
        }
        // Is the whole string one short block repeated? Try every block length that divides it, up to half its length.
        int n = secret.length();
        for (int block = 1; block <= n / 2; block++) {
            if (n % block != 0) continue;
            String unit = secret.substring(0, block);
            if (unit.repeat(n / block).equals(secret)) {
                return true;
            }
        }
        // A long run of one character ("...00000000") means a padded placeholder. Random base64 almost never has a run this long.
        int run = 1;
        for (int i = 1; i < n; i++) {
            run = secret.charAt(i) == secret.charAt(i - 1) ? run + 1 : 1;
            if (run >= MAX_RUN) {
                return true;
            }
        }
        return false;
    }
}
