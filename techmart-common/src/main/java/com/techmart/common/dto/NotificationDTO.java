package com.techmart.common.dto;

import com.techmart.common.enums.NotificationType;
import com.techmart.common.enums.Priority;

import java.io.Serializable;
import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        Long customerId,
        String customerEmail,
        String customerPhone,
        String customerName,
        Long orderId,
        String orderNumber,
        NotificationType type,
        Priority priority,
        String subject,
        String messageBody,
        LocalDateTime createdAt
) implements Serializable {

    public NotificationDTO {
        if (type == null) {
            throw new IllegalArgumentException("Notification type required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject cannot be blank");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("Message body cannot be blank");
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
