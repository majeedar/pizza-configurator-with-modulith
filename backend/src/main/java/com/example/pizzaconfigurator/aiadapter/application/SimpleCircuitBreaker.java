package com.example.pizzaconfigurator.aiadapter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deliberately minimal (agent.md §7.4 "circuit breaker/failure fallback"
 * per provider, agent.md §35 "do not build a distributed event architecture
 * before there is a real need" — same restraint applies here): once {@code
 * failureThreshold} consecutive failures occur, the breaker opens and
 * every call is short-circuited without even attempting the network call
 * until {@code openDuration} has passed, at which point one trial request
 * is allowed through (half-open) to test recovery.
 */
class SimpleCircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant openedAt;

    SimpleCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    boolean allowRequest() {
        return openedAt == null || Instant.now(clock).isAfter(openedAt.plus(openDuration));
    }

    void recordSuccess() {
        consecutiveFailures.set(0);
        openedAt = null;
    }

    void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openedAt = Instant.now(clock);
        }
    }

    void reset() {
        consecutiveFailures.set(0);
        openedAt = null;
    }
}
