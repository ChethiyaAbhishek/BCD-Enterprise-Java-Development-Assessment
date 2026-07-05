package com.techmart.ejb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public abstract class GenericDAO<T extends Serializable> {

    @PersistenceContext(unitName = "techmartPU")
    protected EntityManager entityManager;

    private final Class<T> entityClass;

    protected GenericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T create(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    public T findById(Long id) {
        return entityManager.find(entityClass, id);
    }

    public Optional<T> findByIdOptional(Long id) {
        return Optional.ofNullable(findById(id));
    }

    public List<T> findAll() {
        CriteriaQuery<T> cq = entityManager.getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        return entityManager.createQuery(cq).getResultList();
    }

    public T update(T entity) {
        return entityManager.merge(entity);
    }

    public void delete(T entity) {
        T attached = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(attached);
    }

    public void deleteById(Long id) {
        T entity = findById(id);
        if (entity != null) {
            delete(entity);
        }
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected Class<T> getEntityClass() {
        return entityClass;
    }

}
