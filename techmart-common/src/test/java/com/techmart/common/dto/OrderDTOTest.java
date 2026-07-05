package com.techmart.common.dto;

import com.techmart.common.enums.OrderStatus;
import com.techmart.common.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class OrderDTOTest {

    @Test
    void testValidOrderCreation() {
        OrderDTO order = new OrderDTO(
                1L, "ORD-123", 10L, "test@test.com", "John Doe",
                Collections.singletonList(
                        new OrderDTO.OrderItemDTO(1L, 1L, "Laptop", "SKU1",
                                1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), Collections.emptyList())),
                BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1000),
                "123 Main St", "123 Main St", OrderStatus.PENDING, PaymentStatus.PENDING, LocalDateTime.now()
        );
        assertNotNull(order);
        assertEquals("ORD-123", order.orderNumber());
    }

    @Test
    void testThrowsExceptionWhenItemsAreEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderDTO(
                    1L, "ORD-123", 10L, "test@test.com", "John Doe",
                    Collections.emptyList(),
                    BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1000),
                    "123 Main St", "123 Main St", OrderStatus.PENDING,
                    PaymentStatus.PENDING, LocalDateTime.now()
            );
        });
    }

    @Test
    void testThrowsExceptionWhenTotalAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderDTO(
                    1L, "ORD-123", 10L, "test@test.com", "John Doe",
                    Collections.singletonList(new OrderDTO.OrderItemDTO(1L, 1L, "Laptop",
                            "SKU1", 1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), Collections.emptyList())),
                    BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(-500),
                    "123 Main St", "123 Main St", OrderStatus.PENDING, PaymentStatus.PENDING, LocalDateTime.now()
            );
        });
    }
}
