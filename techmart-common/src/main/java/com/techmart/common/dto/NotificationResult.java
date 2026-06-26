package com.techmart.common.dto;

public record NotificationResult(
        boolean success,
        String message,
        long durationMs
) {
}
