package com.techmart.ejb.async;

import com.techmart.common.dto.TrackingResult;
import com.techmart.ejb.resilience.ExternalServiceProxy;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

import java.util.concurrent.Future;

@Stateless
@Log
public class DeliveryPartnerService {

    @Inject
    private ExternalServiceProxy externalServiceProxy;

    @Asynchronous
    public Future<TrackingResult> fetchTrackingInfoAsync(String trackingNumber) {
        try {
            log.info("Fetching tracking info from external courier API for: " + trackingNumber);

            // Simulate external API latency (500ms - 1500ms)
            long simulatedLatency = 500 + (long)(Math.random() * 1000);
            Thread.sleep(simulatedLatency);

            // 90% success rate
            if (Math.random() > 0.1) {
                String[] statuses = {"IN_TRANSIT", "SHIPPED", "OUT_FOR_DELIVERY"};
                String status = statuses[(int)(Math.random() * statuses.length)];

                String[] locations = {
                        "Colombo Distribution Center",
                        "Kandy Regional Hub",
                        "Galle Sorting Facility",
                        "Local Delivery Office - " + trackingNumber.substring(0, 4)
                };
                String location = locations[(int)(Math.random() * locations.length)];

                log.info("Successfully retrieved tracking info for " + trackingNumber +
                        " - Status: " + status + ", Location: " + location);

                return new AsyncResult<>(new TrackingResult(status, location, "1-2 business days"));
            } else {
                // 10% chance of API error
                log.warning("Courier API returned error for tracking: " + trackingNumber);
                throw new RuntimeException("Courier API timeout");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.severe("Tracking fetch interrupted for: " + trackingNumber);
            return new AsyncResult<>(new TrackingResult("ERROR", "System error", "Contact support"));
        }
    }

}