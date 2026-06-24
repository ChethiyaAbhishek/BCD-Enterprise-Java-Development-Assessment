package com.techmart.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inventory_product", columnList = "product_id"),
        @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_inventory_quantity", columnList = "quantity"),
        @Index(name = "idx_inventory_product_warehouse", columnList = "product_id, warehouse_id")
})
@Cacheable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 0;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Builder.Default
    @Column(name = "reorder_level")
    private Integer reorderLevel = 20;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Version
    private Integer version;

    @OneToMany(mappedBy = "inventory", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("costPrice ASC, receivedDate ASC")
    private List<InventoryBatch> batches = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean isOutOfStock() {
        return getAvailableQuantity() <= 0;
    }

    public synchronized void reserve(int quantity) {
        if (getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Insufficient aggregate stock");
        }
        this.reservedQuantity += quantity;
    }

    public synchronized void confirmReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved");
        }
        this.quantity -= quantity;
        this.reservedQuantity -= quantity;
    }

    public synchronized void releaseReservation(int quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.reservedQuantity -= quantity;
    }

    public void recalculateFromBatches() {
        this.quantity = batches.stream()
                .mapToInt(InventoryBatch::getReceivedQuantity)
                .sum();
        this.reservedQuantity = batches.stream()
                .mapToInt(InventoryBatch::getReservedQuantity)
                .sum();
    }

    public void addBatch(InventoryBatch batch) {
        batches.add(batch);
        batch.setInventory(this);
        recalculateFromBatches();
    }
}
