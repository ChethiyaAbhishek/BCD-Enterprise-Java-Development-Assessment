package com.techmart.common.dto;

import com.techmart.common.entity.InventoryBatch;
import com.techmart.common.entity.OrderItem;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllocationResult implements Serializable {
    private OrderItem orderItem;
    private InventoryBatch batch;
    private Integer quantity;
    private BigDecimal costPrice;
}
