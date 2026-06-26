package com.techmart.ejb.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Interceptor
@Retryable
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@Log
public class AsyncExceptionHandler {

    @AroundInvoke
    public Object handleAsyncException(InvocationContext ctx) throws Exception {

        Retryable retryable = ctx.getMethod().getAnnotation(Retryable.class);
        if (retryable == null) {
            retryable = ctx.getTarget().getClass().getAnnotation(Retryable.class);
        }

        int maxRetries = (retryable != null) ? retryable.maxRetries() : 3;
        long delayMs = (retryable != null) ? retryable.delayMs() : 1000;
        double multiplier = (retryable != null) ? retryable.backoffMultiplier() : 2.0;

        Exception lastException = null;
        long currentDelay = delayMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.warning("Retrying method " + ctx.getMethod().getName() +
                            " (Attempt " + attempt + "/" + maxRetries + "). Waiting " + currentDelay + "ms...");
                    Thread.sleep(currentDelay);
                    currentDelay = (long) (currentDelay * multiplier);
                }

                return ctx.proceed();

            } catch (Exception e) {
                lastException = e;
                log.log(Level.WARNING, "Attempt " + attempt + " failed for " +
                        ctx.getMethod().getName() + ": " + e.getMessage());
            }
        }

        log.severe("All " + maxRetries + " retries exhausted for " + ctx.getMethod().getName() + ".");

        throw lastException;
    }
}
