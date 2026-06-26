package com.techmart.ejb.async;

import com.techmart.common.dto.NotificationDTO;
import com.techmart.common.dto.NotificationResult;
import com.techmart.common.enums.NotificationType;
import com.techmart.ejb.interceptor.Retryable;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import lombok.extern.java.Log;

import java.util.concurrent.Future;

@Stateless
@Log
public class SMSServiceBean {

    @Asynchronous
    @Retryable(maxRetries = 3, delayMs = 1000, backoffMultiplier = 2.0)
    public Future<NotificationResult> sendSMSAsync(NotificationDTO notification) {
        try {
            if (notification.type() == NotificationType.ORDER_CONFIRMATION ||
                    notification.type() == NotificationType.ORDER_SHIPPED) {

                log.info("Sending SMS to " + notification.customerPhone());
                Thread.sleep(800); // Simulate SMS gateway latency
            }

            return new AsyncResult<>(new NotificationResult(true, "SMS sent", 800));
        } catch (Exception e) {
            log.severe("SMS dispatch failed: " + e.getMessage());
            return new AsyncResult<>(new NotificationResult(false, e.getMessage(), 0));
        }
    }
}
