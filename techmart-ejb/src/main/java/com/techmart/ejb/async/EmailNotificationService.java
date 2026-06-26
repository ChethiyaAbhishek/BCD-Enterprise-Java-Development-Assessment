package com.techmart.ejb.async;

import com.techmart.common.dto.NotificationDTO;
import com.techmart.common.dto.NotificationResult;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import lombok.extern.java.Log;

import java.util.concurrent.Future;

@Stateless
@Log
public class EmailNotificationService {

    @Asynchronous
    public Future<NotificationResult> sendEmailAsync(NotificationDTO notification) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Sending email to " + notification.customerEmail() + " (Type: " + notification.type() + ")");

            // Simulate external SMTP gateway latency (1.5 seconds)
            Thread.sleep(1500);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Email sent successfully in " + duration + "ms.");

            return new AsyncResult<>(new NotificationResult(true, "Email sent", duration));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AsyncResult<>(new NotificationResult(false, "Thread interrupted", 0));
        } catch (Exception e) {
            log.severe("Failed to send email: " + e.getMessage());
            return new AsyncResult<>(new NotificationResult(false, e.getMessage(), 0));
        }
    }
}
