package com.techmart.ejb.unit.resilience;

import com.techmart.ejb.resilience.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Circuit Breaker pattern
 * Proves that the system fails fast and recovers automatically
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker("TestService", 3, 1000, 2);
    }

    @Test
    void testInitialStateIsClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowRequest());
    }

    @Test
    void testTransitionsToOpenAfterFailures() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        assertFalse(circuitBreaker.allowRequest());
    }

    @Test
    void testTransitionsToHalfOpenAfterTimeout() throws InterruptedException {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        Thread.sleep(1100);

        assertTrue(circuitBreaker.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    void testRecoversToClosedAfterSuccesses() throws InterruptedException {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        Thread.sleep(1100);

        circuitBreaker.allowRequest();

        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowRequest());
    }
}
