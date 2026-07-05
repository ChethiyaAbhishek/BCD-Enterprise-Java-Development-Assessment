package com.techmart.ejb.session.singleton;

import com.techmart.common.dto.PerformanceMetricDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
@Log
public class MetricsCollectorBean {

    private final Map<String, MetricData> metricsMap = new ConcurrentHashMap<>();

    private long applicationStartTime;

    @PostConstruct
    public void init() {
        applicationStartTime = System.currentTimeMillis();
        log.info("MetricsCollectorBean initialized. Performance monitoring active.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("MetricsCollectorBean shutting down. Final metrics snapshot logged.");
        logAllMetrics();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Lock(LockType.WRITE)
    public void recordSuccess(String metricName, long executionTimeMs) {
        MetricData data = metricsMap.computeIfAbsent(metricName, k -> new MetricData(metricName));
        data.recordSuccess(executionTimeMs);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Lock(LockType.WRITE)
    public void recordFailure(String metricName, long executionTimeMs) {
        MetricData data = metricsMap.computeIfAbsent(metricName, k -> new MetricData(metricName));
        data.recordFailure(executionTimeMs);
    }

    public List<PerformanceMetricDTO> getAllMetrics() {
        List<PerformanceMetricDTO> dtos = new ArrayList<>();

        for (MetricData data : metricsMap.values()) {
            dtos.add(data.toDTO());
        }

        return dtos;
    }

    public PerformanceMetricDTO getMetric(String metricName) {
        MetricData data = metricsMap.get(metricName);
        return data != null ? data.toDTO() : null;
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - applicationStartTime) / 1000;
    }

    public int getMonitoredMethodCount() {
        return metricsMap.size();
    }

    @Lock(LockType.WRITE)
    public void resetAllMetrics() {
        metricsMap.clear();
        log.info("All metrics reset.");
    }

    private void logAllMetrics() {
        log.info("=== PERFORMANCE METRICS SNAPSHOT ===");
        for (MetricData data : metricsMap.values()) {
            log.info(data.toDTO().toString());
        }
        log.info("====================================");
    }

    private static class MetricData {
        private final String metricName;
        private final AtomicLong invocationCount = new AtomicLong(0);
        private final AtomicLong totalTimeMs = new AtomicLong(0);
        private final AtomicLong minTimeMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTimeMs = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long lastInvocationTime;

        public MetricData(String metricName) {
            this.metricName = metricName;
        }

        public synchronized void recordSuccess(long executionTimeMs) {
            invocationCount.incrementAndGet();
            successCount.incrementAndGet();
            totalTimeMs.addAndGet(executionTimeMs);
            minTimeMs.updateAndGet(current -> Math.min(current, executionTimeMs));
            maxTimeMs.updateAndGet(current -> Math.max(current, executionTimeMs));
            lastInvocationTime = System.currentTimeMillis();
        }

        public synchronized void recordFailure(long executionTimeMs) {
            invocationCount.incrementAndGet();
            errorCount.incrementAndGet();
            totalTimeMs.addAndGet(executionTimeMs);
            minTimeMs.updateAndGet(current -> Math.min(current, executionTimeMs));
            maxTimeMs.updateAndGet(current -> Math.max(current, executionTimeMs));
            lastInvocationTime = System.currentTimeMillis();
        }

        public PerformanceMetricDTO toDTO() {
            long count = invocationCount.get();
            long total = totalTimeMs.get();
            long errors = errorCount.get();
            long successes = successCount.get();

            double avgTime = count > 0 ? (double) total / count : 0;
            double errorRate = count > 0 ? (double) errors / count * 100 : 0;

            long uptimeSeconds = (System.currentTimeMillis() - lastInvocationTime) / 1000;
            double throughput = uptimeSeconds > 0 ? (double) count / uptimeSeconds : 0;

            String beanName = metricName.contains(".")
                    ? metricName.substring(0, metricName.lastIndexOf('.'))
                    : metricName;

            double p95 = maxTimeMs.get() == Long.MAX_VALUE ? 0 : maxTimeMs.get() * 0.9;
            double p99 = maxTimeMs.get() == Long.MAX_VALUE ? 0 : maxTimeMs.get() * 0.95;

            return new PerformanceMetricDTO(
                    metricName,
                    beanName,
                    count,
                    avgTime,
                    minTimeMs.get() == Long.MAX_VALUE ? 0 : minTimeMs.get(),
                    maxTimeMs.get() == Long.MAX_VALUE ? 0 : maxTimeMs.get(),
                    p95,
                    p99,
                    throughput,
                    successes,
                    errors,
                    errorRate,
                    java.time.LocalDateTime.now()
            );
        }
    }
}
