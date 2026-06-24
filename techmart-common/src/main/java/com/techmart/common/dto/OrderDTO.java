package com.techmart.common.dto;

import com.techmart.common.enums.OrderStatus;
import com.techmart.common.enums.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Long id,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        List<OrderItemDTO> items,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String shippingAddress,
        String billingAddress,
        OrderStatus status,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt
) implements Serializable {

    public OrderDTO {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number cannot be null or blank");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount must be non-negative");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
    }

    /**
     * Nested record for order items within the order
     */
    public record OrderItemDTO(
            Long orderItemId,
            Long productId,
            String productName,
            String productSku,
            Integer quantity,
            BigDecimal sellingPrice,
            BigDecimal subtotal,
            List<AllocationDTO> allocations
    ) implements Serializable {}

    /**
     * Nested record for allocation details
     */
    public record AllocationDTO(
            Long batchId,
            String batchNumber,
            Long warehouseId,
            String warehouseCode,
            Integer quantity,
            BigDecimal costPrice
    ) implements Serializable {}
}
