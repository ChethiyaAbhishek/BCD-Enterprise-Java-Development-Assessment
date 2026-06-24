package com.techmart.ejb.dao;

import com.techmart.common.entity.OrderItemAllocation;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.util.List;

@Stateless
public class OrderItemAllocationDAO extends GenericDAO<OrderItemAllocation> {

    public OrderItemAllocationDAO() {
        super(OrderItemAllocation.class);
    }

    public List<OrderItemAllocation> findByOrderItem(Long orderItemId) {
        TypedQuery<OrderItemAllocation> query = entityManager.createQuery(
                "SELECT a FROM OrderItemAllocation a WHERE a.orderItem.id = :orderItemId",
                OrderItemAllocation.class);
        query.setParameter("orderItemId", orderItemId);
        return query.getResultList();
    }

    public List<OrderItemAllocation> findByBatch(Long batchId) {
        TypedQuery<OrderItemAllocation> query = entityManager.createQuery(
                "SELECT a FROM OrderItemAllocation a WHERE a.batch.id = :batchId " +
                        "ORDER BY a.createdAt DESC", OrderItemAllocation.class);
        query.setParameter("batchId", batchId);
        return query.getResultList();
    }

    public BigDecimal calculateOrderCost(Long orderId) {
        BigDecimal result = entityManager.createQuery(
                        "SELECT SUM(a.costPrice * a.quantity) FROM OrderItemAllocation a " +
                                "WHERE a.orderItem.order.id = :orderId", BigDecimal.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}
