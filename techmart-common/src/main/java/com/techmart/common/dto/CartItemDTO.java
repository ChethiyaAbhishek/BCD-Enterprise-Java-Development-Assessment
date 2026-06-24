package com.techmart.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CartItemDTO(
        Long productId,
        String productName,
        String productSku,
        BigDecimal unitPrice,
        Integer quantity,
        String imageUrl,
        String categoryName
) implements Serializable {

    public CartItemDTO {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
