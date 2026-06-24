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

//    /**
//     * Refresh entity state from database.
//     */
//    public void refresh(T entity) {
//        entityManager.refresh(entity);
//    }

//    /**
//     * Flush pending changes to database.
//     */
//    public void flush() {
//        entityManager.flush();
//    }
//
//    /**
//     * Detach entity from persistence context.
//     */
//    public void detach(T entity) {
//        entityManager.detach(entity);
//    }


//    /**
//     * Batch create multiple entities.
//     * Uses JDBC batching configured in persistence.xml.
//     *
//     * @param entities list of entities to persist
//     * @param batchSize number of entities per batch flush
//     */
//    public void batchCreate(List<T> entities, int batchSize) {
//        int count = 0;
//        for (T entity : entities) {
//            entityManager.persist(entity);
//            if (++count % batchSize == 0) {
//                entityManager.flush();
//                entityManager.clear();
//            }
//        }
//        // Flush remaining
//        if (count > 0) {
//            entityManager.flush();
//            entityManager.clear();
//        }
//        log.info("Batch created " + count + " entities of type " + entityClass.getSimpleName());
//    }

//    /**
//     * Batch update multiple entities.
//     */
//    public void batchUpdate(List<T> entities, int batchSize) {
//        int count = 0;
//        for (T entity : entities) {
//            entityManager.merge(entity);
//            if (++count % batchSize == 0) {
//                entityManager.flush();
//                entityManager.clear();
//            }
//        }
//        if (count > 0) {
//            entityManager.flush();
//            entityManager.clear();
//        }
//    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected Class<T> getEntityClass() {
        return entityClass;
    }

}
