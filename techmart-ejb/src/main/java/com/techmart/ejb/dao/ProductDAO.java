package com.techmart.ejb.dao;

import com.techmart.common.entity.Product;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;

@Stateless
public class ProductDAO extends GenericDAO<Product> {

    public ProductDAO() {
        super(Product.class);
    }

    public List<Product> findActiveProducts(int offset, int limit) {
        TypedQuery<Product> query = entityManager.createQuery(
                "SELECT p FROM Product p WHERE p.isActive = true",
                Product.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Product> findByCategory(String categoryName, int offset, int limit) {
        TypedQuery<Product> query = entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.isActive = true AND p.category.name = :categoryName", Product.class)
                .setParameter("categoryName", categoryName);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Product> searchByName(String name, int offset, int limit) {
        TypedQuery<Product> query = entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.isActive = true AND p.name = :name", Product.class)
                .setParameter("name", name);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public Product findBySku(String sku) {
        List<Product> results = entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.isActive = true AND p.sku = :sku", Product.class)
                .setParameter("sku", sku).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(p) FROM Product p", Long.class);
        return query.getSingleResult();
    }
}
