package com.techmart.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductDTO(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal sellingPrice,
        String categoryName,
        String imageUrl,
        Long totalStock,
        Boolean isActive
) implements Serializable {

    public ProductDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Selling price must be positive");
        }
    }

//    /**
//     * Check if product is in stock (any warehouse).
//     */
//    public boolean isInStock() {
//        return totalStock != null && totalStock > 0;
//    }
//
//    /**
//     * Check if product is low stock (for UI warnings).
//     */
//    public boolean isLowStock(int threshold) {
//        return totalStock != null && totalStock > 0 && totalStock <= threshold;
//    }
}
