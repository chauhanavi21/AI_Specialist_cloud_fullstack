package com.ibm.capstone.bookstore.security;

import com.ibm.capstone.bookstore.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Slows down password guessing: after {@code maxFailures} wrong passwords for one email within the
 * window, further attempts for that email are refused until the window passes. A successful login clears the count.
 *
 * The state is in memory, so it is per instance and resets on restart. That is enough for a single-replica demo;
 * a multi-replica deployment would keep the counters in a shared store such as Redis.
 */
@Component
public class LoginAttemptLimiter {

    private final int maxFailures;
    private final Duration window;
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public LoginAttemptLimiter(@Value("${app.security.login-max-failures:5}") int maxFailures,
                               @Value("${app.security.login-window-seconds:300}") long windowSeconds) {
        this.maxFailures = maxFailures;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public synchronized void checkAllowed(String key) {
        if (!failures.containsKey(key)) {
            return; // never failed: do not create an entry for every email that is merely tried
        }
        Deque<Instant> recent = recentFailures(key);
        if (recent.isEmpty()) {
            failures.remove(key);
            return;
        }
        if (recent.size() >= maxFailures) {
            long wait = Math.max(1, Duration.between(Instant.now(), recent.peekFirst().plus(window)).toSeconds());
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again in " + wait + " seconds.");
        }
    }

    public synchronized void recordFailure(String key) {
        recentFailures(key).addLast(Instant.now());
    }

    public synchronized void reset(String key) {
        failures.remove(key);
    }

    /** The failures still inside the window for this key (the deque is created on first use). */
    private Deque<Instant> recentFailures(String key) {
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.removeFirst();
        }
        return recent;
    }
}
