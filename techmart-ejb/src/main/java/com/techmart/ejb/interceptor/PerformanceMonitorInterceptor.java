package com.techmart.ejb.interceptor;

import com.techmart.ejb.session.singleton.MetricsCollectorBean;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lombok.extern.java.Log;

@Interceptor
@PerformanceMonitored
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@Log
public class PerformanceMonitorInterceptor {

    @Inject
    private MetricsCollectorBean metricsCollector;

    @AroundInvoke
    public Object monitorPerformance(InvocationContext ctx) throws Exception {

        String metricName = getMetricName(ctx);

        PerformanceMonitored annotation = getAnnotation(ctx);
        boolean logSlow = annotation != null && annotation.logSlowExecution();
        long slowThreshold = annotation != null ? annotation.slowThresholdMs() : 1000;

        long startTime = System.nanoTime();

        try {
            Object result = ctx.proceed();

            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            metricsCollector.recordSuccess(metricName, executionTimeMs);

            if (logSlow && executionTimeMs > slowThreshold) {
                log.warning("SLOW EXECUTION: " + metricName + " took " + executionTimeMs + "ms (threshold: " + slowThreshold + "ms)");
            }

            return result;

        } catch (Exception e) {
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            metricsCollector.recordFailure(metricName, executionTimeMs);

            log.severe("METHOD FAILURE: " + metricName + " failed after " + executionTimeMs + "ms: " + e.getMessage());

            throw e;
        }
    }

    private String getMetricName(InvocationContext ctx) {
        PerformanceMonitored annotation = getAnnotation(ctx);

        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        return ctx.getTarget().getClass().getSimpleName() + "." + ctx.getMethod().getName();
    }

    private PerformanceMonitored getAnnotation(InvocationContext ctx) {

        PerformanceMonitored annotation = ctx.getMethod().getAnnotation(PerformanceMonitored.class);

        if (annotation == null) {
            annotation = ctx.getTarget().getClass().getAnnotation(PerformanceMonitored.class);
        }

        return annotation;
    }
}
