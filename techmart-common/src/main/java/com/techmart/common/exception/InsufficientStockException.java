package com.techmart.common.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, Integer requestedQty, Integer remainingQty) {
        super("Insufficient Stock. ProductId: " + productId + ", requestedQty: " + requestedQty
                + ", remainingQty: " + remainingQty);
    }
}
