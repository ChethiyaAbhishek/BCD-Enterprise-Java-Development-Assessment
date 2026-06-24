package com.techmart.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record InventoryUpdateDTO(
        Long productId,
        String productName,
        Long warehouseId,
        String warehouseCode,
        Integer previousQuantity,
        Integer newQuantity,
        Integer changeAmount,
        String reason,
        String batchNumber,
        LocalDateTime timestamp
) implements Serializable {

    public InventoryUpdateDTO {
        if (productId == null || warehouseId == null) {
            throw new IllegalArgumentException("Product and warehouse IDs required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason for update required");
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public InventoryUpdateDTO(Long productId, Long warehouseId,
                              Integer changeAmount, String reason, String batchNumber) {
        this(productId, null, warehouseId, null, null, null,
                changeAmount, reason, batchNumber, LocalDateTime.now());
    }

//    /**
//     * Check if this is a stock reduction (order-related).
//     */
//    public boolean isStockReduction() {
//        return changeAmount < 0;
//    }
//
//    /**
//     * Check if this is a stock increase (batch received).
//     */
//    public boolean isStockIncrease() {
//        return changeAmount > 0;
//    }
}
