package com.techmart.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batch", indexes = {
        @Index(name = "idx_batch_inventory", columnList = "inventory_id"),
        @Index(name = "idx_batch_product", columnList = "product_id"),
        @Index(name = "idx_batch_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_batch_received", columnList = "received_date"),
        @Index(name = "idx_batch_cost", columnList = "cost_price")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "batch_number", nullable = false, length = 50, unique = true)
    private String batchNumber;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "cost_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (availableQuantity == null) {
            availableQuantity = receivedQuantity;
        }
    }

    public boolean canFulfill(int quantity) {
        return availableQuantity >= quantity;
    }

    public synchronized void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException(
                    "Cannot reserve " + quantity + " units. Only " + availableQuantity + " available in batch " + batchNumber
            );
        }
        this.reservedQuantity += quantity;
        this.availableQuantity -= quantity;
    }

    public synchronized void confirmReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Cannot confirm " + quantity + " units. Only " + reservedQuantity + " reserved in batch " + batchNumber
            );
        }
        this.receivedQuantity -= quantity;
        this.reservedQuantity -= quantity;
    }

    public synchronized void releaseReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "Cannot release " + quantity + " units. Only " + reservedQuantity + " reserved in batch " + batchNumber
            );
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }
}
