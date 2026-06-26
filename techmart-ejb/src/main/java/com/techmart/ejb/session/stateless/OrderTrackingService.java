package com.techmart.ejb.session.stateless;

import com.techmart.common.dto.TrackingResult;
import com.techmart.ejb.async.DeliveryPartnerService;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Stateless
@Log
public class OrderTrackingService {

    @Inject
    private DeliveryPartnerService deliveryPartnerService;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public TrackingResult getTrackingStatus(String orderNumber) {

        Future<TrackingResult> future =
                deliveryPartnerService.fetchTrackingInfoAsync(orderNumber);

        try {
            return future.get(3, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            future.cancel(true);
            log.warning("Courier API timed out for tracking number: " + orderNumber);
            return new TrackingResult("DELAYED", "Location Unavailable", "Please try again later");

        } catch (ExecutionException e) {
            log.severe("Courier API execution error: " + e.getCause().getMessage());
            return new TrackingResult("ERROR", "System Error", "Contact support");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TrackingResult("ERROR", "Interrupted", "System busy");
        }
    }
}
