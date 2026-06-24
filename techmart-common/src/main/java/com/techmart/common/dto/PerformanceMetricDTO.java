package com.techmart.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PerformanceMetricDTO(
        String methodName,
        String beanName,
        long invocationCount,
        double avgResponseTimeMs,
        double minResponseTimeMs,
        double maxResponseTimeMs,
        double p95ResponseTimeMs,
        double p99ResponseTimeMs,
        double throughputPerSecond,
        long successCount,
        long errorCount,
        double errorRatePercent,
        LocalDateTime lastUpdated
) implements Serializable {

    public PerformanceMetricDTO {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Method name required");
        }
        if (invocationCount < 0) {
            throw new IllegalArgumentException("Invocation count cannot be negative");
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    /**
     * Check if this method has acceptable performance (< 100ms avg).
     */
    public boolean isPerformant() {
        return avgResponseTimeMs < 100.0;
    }

    /**
     * Check if error rate is within acceptable limits (< 1%).
     */
    public boolean hasAcceptableErrorRate() {
        return errorRatePercent < 1.0;
    }

    /**
     * Get health status based on performance and error rate.
     */
    public String getHealthStatus() {
        if (errorRatePercent > 5.0 || avgResponseTimeMs > 1000.0) {
            return "CRITICAL";
        } else if (errorRatePercent > 1.0 || avgResponseTimeMs > 500.0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
}
