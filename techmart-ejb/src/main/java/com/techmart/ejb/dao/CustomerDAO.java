package com.techmart.ejb.dao;

import com.techmart.common.entity.Customer;
import jakarta.ejb.Stateless;

import java.util.List;

@Stateless
public class CustomerDAO extends GenericDAO<Customer> {

    public CustomerDAO() {
        super(Customer.class);
    }

    public Customer findByEmail(String email) {
        List<Customer> results = entityManager.createQuery(
                        "SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                .setParameter("email", email).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean emailExists(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(c) FROM Customer c WHERE c.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    public void updateLastLogin(Long customerId) {
        entityManager.createQuery(
                        "UPDATE Customer c SET c.lastLogin = :now WHERE c.id = :id")
                .setParameter("now", java.time.LocalDateTime.now())
                .setParameter("id", customerId)
                .executeUpdate();
    }
}
