package com.techmart.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

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
