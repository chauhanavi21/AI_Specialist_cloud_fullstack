package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.exception.TooManyRequestsException;
import com.ibm.capstone.bookstore.security.LoginAttemptLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Uses a movable clock so the five-minute window can be tested without waiting five minutes. */
class LoginAttemptLimiterTest {

    /** A clock the test can move. */
    static class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T12:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static LoginAttemptLimiter limiter(MovableClock clock) throws Exception {
        Constructor<LoginAttemptLimiter> c = LoginAttemptLimiter.class.getDeclaredConstructor(int.class, Duration.class, Clock.class);
        c.setAccessible(true);
        return c.newInstance(5, Duration.ofMinutes(5), clock);
    }

    private static int tracked(LoginAttemptLimiter l) throws Exception {
        Method m = LoginAttemptLimiter.class.getDeclaredMethod("tracked");
        m.setAccessible(true);
        return (int) m.invoke(l);
    }

    @Test
    @DisplayName("The fifth wrong password is allowed, the sixth attempt is refused, and it frees up after the window")
    void blocksAfterFiveFailuresAndRecovers() throws Exception {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter l = limiter(clock);

        for (int i = 0; i < 5; i++) {
            l.checkAllowed("a@x.com");
            l.recordFailure("a@x.com");
        }
        assertThrows(TooManyRequestsException.class, () -> l.checkAllowed("a@x.com"));
        assertDoesNotThrow(() -> l.checkAllowed("someone.else@x.com"), "other emails are unaffected");

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertDoesNotThrow(() -> l.checkAllowed("a@x.com"), "the block ends once the window has passed");
    }

    @Test
    @DisplayName("A successful sign-in clears the count")
    void successResets() throws Exception {
        LoginAttemptLimiter l = limiter(new MovableClock());
        for (int i = 0; i < 4; i++) l.recordFailure("a@x.com");
        l.reset("a@x.com");
        for (int i = 0; i < 4; i++) l.recordFailure("a@x.com");
        assertDoesNotThrow(() -> l.checkAllowed("a@x.com"), "4 + 4 failures with a success between is not 8 in a row");
    }

    @Test
    @DisplayName("Merely trying many emails records nothing")
    void checkingAloneDoesNotAllocate() throws Exception {
        LoginAttemptLimiter l = limiter(new MovableClock());
        for (int i = 0; i < 5_000; i++) l.checkAllowed("user" + i + "@x.com");
        assertEquals(0, tracked(l));
    }

    @Test
    @DisplayName("Memory stays bounded: one email hammered a million times, and a flood of different failing emails")
    void memoryIsBounded() throws Exception {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter l = limiter(clock);

        for (int i = 0; i < 1_000_000; i++) l.recordFailure("victim@x.com");
        // no exception and a normal heap: the queue was capped at 5. The next check is simply refused.
        assertThrows(TooManyRequestsException.class, () -> l.checkAllowed("victim@x.com"));

        for (int i = 0; i < 30_000; i++) l.recordFailure("flood" + i + "@x.com");
        assertTrue(tracked(l) <= 10_000, "tracked emails are capped, but " + tracked(l) + " were kept");
    }

    @Test
    @DisplayName("Entries for emails that stopped trying are dropped once their failures age out")
    void oldEntriesAreSwept() throws Exception {
        MovableClock clock = new MovableClock();
        LoginAttemptLimiter l = limiter(clock);
        for (int i = 0; i < 9_000; i++) l.recordFailure("old" + i + "@x.com");
        clock.advance(Duration.ofMinutes(10));
        for (int i = 0; i < 2_000; i++) l.recordFailure("new" + i + "@x.com");   // pushes the map past the cap and triggers a sweep
        assertEquals(2_000, tracked(l), "the 9,000 stale emails are gone, only the recent ones remain");
    }
}
