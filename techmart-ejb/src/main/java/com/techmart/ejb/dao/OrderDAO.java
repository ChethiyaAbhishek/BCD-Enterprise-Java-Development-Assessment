package com.techmart.ejb.dao;

import com.techmart.common.entity.Order;
import com.techmart.common.enums.OrderStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class OrderDAO extends GenericDAO<Order> {

    public OrderDAO() {
        super(Order.class);
    }

    public List<Order> findByCustomer(Long customerId, int offset, int limit) {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC", Order.class);
        query.setParameter("customerId", customerId);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC",Order.class
        ).setParameter("status", status).getResultList();
    }

    public List<Order> findPendingOrders() {
        return entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt ASC",Order.class
        ).setParameter("status", OrderStatus.PENDING).getResultList();
    }

    public Order findByOrderNumber(String orderNumber) {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.orderNumber = :orderNumber", Order.class);
        query.setParameter("orderNumber", orderNumber);
        List<Order> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Order> findByDateRange(LocalDateTime from, LocalDateTime to) {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC", Order.class);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    public long countByStatus(OrderStatus status) {
        return entityManager.createQuery(
                        "SELECT COUNT(o) FROM Order o WHERE o.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    public java.math.BigDecimal calculateRevenue(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery(
                        "SELECT SUM(o.totalAmount) FROM Order o " +
                                "WHERE o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED'",
                        java.math.BigDecimal.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public List<Order> findRecentOrders(int limit) {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o ORDER BY o.createdAt DESC", Order.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}
