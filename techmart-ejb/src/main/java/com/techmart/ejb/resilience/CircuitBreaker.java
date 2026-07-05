package com.techmart.ejb.resilience;

import lombok.extern.java.Log;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Log
public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final long timeoutMillis;
    private final int successThreshold;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile Instant lastFailureTime;

    // monitoring
    private volatile long totalRequests = 0;
    private volatile long totalFailures = 0;
    private volatile long totalRejected = 0;

    public CircuitBreaker(String name, int failureThreshold, long timeoutMillis, int successThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMillis = timeoutMillis;
        this.successThreshold = successThreshold;
    }

    public boolean allowRequest() {
        totalRequests++;

        State currentState = state.get();

        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            if (lastFailureTime != null &&
                    Instant.now().toEpochMilli() - lastFailureTime.toEpochMilli() > timeoutMillis) {

                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("CircuitBreaker [" + name + "] transitioning from OPEN to HALF_OPEN");
                    successCount.set(0);
                    return true;
                }
            }

            totalRejected++;
            return false;
        }

        return true;
    }

    public void recordSuccess() {
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= successThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    log.info("CircuitBreaker [" + name + "] transitioning from HALF_OPEN to CLOSED (recovered)");
                    failureCount.set(0);
                    successCount.set(0);
                }
            }
        } else if (currentState == State.CLOSED) {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        totalFailures++;
        lastFailureTime = Instant.now();

        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                log.warning("CircuitBreaker [" + name + "] transitioning from HALF_OPEN to OPEN (still failing)");
            }
        } else if (currentState == State.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    log.severe("CircuitBreaker [" + name + "] transitioning from CLOSED to OPEN (threshold reached: " + failures + ")");
                }
            }
        }
    }

    public State getState() { return state.get(); }
    public String getName() { return name; }
    public long getTotalRequests() { return totalRequests; }
    public long getTotalFailures() { return totalFailures; }
    public long getTotalRejected() { return totalRejected; }
    public int getFailureCount() { return failureCount.get(); }
}
