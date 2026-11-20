package com.ibm.capstone.bookstore.security;

import com.ibm.capstone.bookstore.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Slows down password guessing: after {@code maxFailures} wrong passwords for one email within the
 * window, further attempts for that email are refused until the window passes. A successful login clears the count.
 *
 * Memory is bounded no matter what an attacker sends:
 * <ul>
 *   <li>each email keeps at most {@code maxFailures} timestamps, however many wrong passwords arrive;</li>
 *   <li>emails whose failures have all aged out are dropped;</li>
 *   <li>at most {@link #MAX_TRACKED} emails are tracked, and the least recently used is evicted first.</li>
 * </ul>
 * The limit is a trade-off: a flood of failing logins for {@code MAX_TRACKED} different emails can push out an older
 * lockout. That is acceptable here because it only ever relaxes the limit for one email, never locks a real user out.
 *
 * The state is in memory, so it is per instance and resets on restart. That is enough for a single-replica demo;
 * a multi-replica deployment would keep the counters in a shared store such as Redis.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_TRACKED = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    /** Access-ordered, so iteration starts at the least recently used email. */
    private final Map<String, Deque<Instant>> failures = new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public LoginAttemptLimiter(@Value("${app.security.login-max-failures:5}") int maxFailures,
                               @Value("${app.security.login-window-seconds:300}") long windowSeconds) {
        this(maxFailures, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    /** For tests: lets time be moved without sleeping. */
    LoginAttemptLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public synchronized void checkAllowed(String key) {
        Deque<Instant> recent = failures.get(key);
        if (recent == null) {
            return; // never failed: do not create an entry for every email that is merely tried
        }
        expire(recent);
        if (recent.isEmpty()) {
            failures.remove(key);
            return;
        }
        if (recent.size() >= maxFailures) {
            long wait = Math.max(1, Duration.between(clock.instant(), recent.peekFirst().plus(window)).toSeconds());
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again in " + wait + " seconds.");
        }
    }

    public synchronized void recordFailure(String key) {
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        expire(recent);
        recent.addLast(clock.instant());
        while (recent.size() > maxFailures) {
            recent.removeFirst();          // an email being hammered cannot grow its own queue
        }
        if (failures.size() > MAX_TRACKED) {
            sweep();
        }
    }

    public synchronized void reset(String key) {
        failures.remove(key);
    }

    /** Number of emails currently tracked (for tests). */
    synchronized int tracked() {
        return failures.size();
    }

    private void expire(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.removeFirst();
        }
    }

    /** Drop emails whose failures have all aged out, then the least recently used until the map is back under the cap. */
    private void sweep() {
        for (Iterator<Map.Entry<String, Deque<Instant>>> it = failures.entrySet().iterator(); it.hasNext(); ) {
            Deque<Instant> d = it.next().getValue();
            expire(d);
            if (d.isEmpty()) it.remove();
        }
        Iterator<String> oldest = failures.keySet().iterator();
        while (failures.size() > MAX_TRACKED && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }
}
