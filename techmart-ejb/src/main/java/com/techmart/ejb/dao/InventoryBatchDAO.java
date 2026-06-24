package com.techmart.ejb.dao;

import com.techmart.common.entity.InventoryBatch;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Stateless
public class InventoryBatchDAO extends GenericDAO<InventoryBatch> {

    public InventoryBatchDAO() {
        super(InventoryBatch.class);
    }

    public List<InventoryBatch> findAvailableByProduct(Long productId) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b " +
                        "WHERE b.inventory.product.id = :productId AND b.availableQuantity > 0 " +
                        "ORDER BY b.costPrice ASC", InventoryBatch.class);
        query.setParameter("productId", productId);
        return query.getResultList();
    }

    public List<InventoryBatch> findByInventory(Long inventoryId) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b WHERE b.inventory.id = :inventoryId " +
                        "ORDER BY b.costPrice ASC, b.receivedDate ASC", InventoryBatch.class);
        query.setParameter("inventoryId", inventoryId);
        return query.getResultList();
    }

    public Optional<InventoryBatch> findByBatchNumber(String batchNumber) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b WHERE b.batchNumber = :batchNumber", InventoryBatch.class);
        query.setParameter("batchNumber", batchNumber);
        List<InventoryBatch> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<InventoryBatch> findByReceivedDateRange(LocalDate from, LocalDate to) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b WHERE b.receivedDate BETWEEN :from AND :to " +
                        "ORDER BY b.receivedDate DESC", InventoryBatch.class);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    public List<InventoryBatch> findLowAvailabilityBatches(int threshold) {
        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b WHERE b.availableQuantity <= :threshold " +
                        "AND b.availableQuantity > 0 ORDER BY b.availableQuantity ASC", InventoryBatch.class);
        query.setParameter("threshold", threshold);
        return query.getResultList();
    }
}
