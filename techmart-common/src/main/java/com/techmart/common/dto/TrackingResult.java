package com.techmart.common.dto;

public record TrackingResult(
        String status,
        String location,
        String estimatedDelivery
) {
}
