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
}
