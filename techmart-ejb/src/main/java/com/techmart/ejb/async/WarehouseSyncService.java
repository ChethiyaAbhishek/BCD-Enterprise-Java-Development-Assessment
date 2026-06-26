package com.techmart.ejb.async;

import com.techmart.common.dto.InventoryUpdateDTO;
import com.techmart.ejb.interceptor.Retryable;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import lombok.extern.java.Log;

@Stateless
@Log
public class WarehouseSyncService {

    @Asynchronous
    @Retryable(maxRetries = 3, delayMs = 1000, backoffMultiplier = 2.0)
    public void syncWithExternalWMS(InventoryUpdateDTO update) {
        try {
            log.info("Syncing with external WMS for Product " + update.productId());

            // Simulate REST API call to legacy ERP
            Thread.sleep(1200);

            log.info("WMS sync successful for batch " + update.batchNumber());
        } catch (Exception e) {
            log.severe("WMS sync failed. Will be retried by message redelivery: " + e.getMessage());
        }
    }
}