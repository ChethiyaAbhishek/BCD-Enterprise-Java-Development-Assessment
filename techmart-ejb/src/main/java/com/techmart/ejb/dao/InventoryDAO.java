package com.techmart.ejb.dao;

import com.techmart.common.entity.Inventory;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Stateless
public class InventoryDAO extends GenericDAO<Inventory> {

    public InventoryDAO() {
        super(Inventory.class);
    }

    public Optional<Inventory> findByProductAndWarehouse(Long productId, Long warehouseId) {
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId",
                Inventory.class);
        query.setParameter("productId", productId);
        query.setParameter("warehouseId", warehouseId);
        List<Inventory> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Inventory> findByProduct(Long productId) {
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE i.product.id = :productId", Inventory.class);
        query.setParameter("productId", productId);
        return query.getResultList();
    }

    public List<Inventory> findLowStock(int threshold) {
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= :threshold " +
                        "AND i.warehouse.active = true", Inventory.class);
        query.setParameter("threshold", threshold);
        return query.getResultList();
    }

    public List<Inventory> findOutOfStock() {
        return entityManager.createQuery("SELECT i FROM Inventory i WHERE i.quantity = 0", Inventory.class)
                .getResultList();
    }

//    public Integer calculateTotalStock(Long productId) {
//        Integer result = entityManager.createQuery(
//                        "SELECT SUM(i.quantity - i.reservedQuantity) FROM Inventory i " +
//                                "WHERE i.product.id = :productId AND i.warehouse.active = true", Integer.class)
//                .setParameter("productId", productId)
//                .getSingleResult();
//        return result != null ? result : 0;
//    }

    public Long calculateTotalStock(Long productId) {
        Object result = entityManager.createQuery(
                        "SELECT SUM(i.quantity - i.reservedQuantity) FROM Inventory i " +
                                "WHERE i.product.id = :productId AND i.warehouse.active = true")
                .setParameter("productId", productId)
                .getSingleResult();
        return result != null ? (Long) result : 0L;
    }

    public List<Inventory> findByWarehouse(Long warehouseId) {
        TypedQuery<Inventory> query = entityManager.createQuery(
                "SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId", Inventory.class);
        query.setParameter("warehouseId", warehouseId);
        return query.getResultList();
    }
}
