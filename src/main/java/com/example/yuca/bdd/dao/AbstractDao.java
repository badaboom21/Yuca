package com.example.yuca.bdd.dao;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractDao<T> implements Dao<T> {

    @PersistenceContext
    protected EntityManager em;

    private final Class<T> entityClass;

    protected AbstractDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> get(long id) {
        T entity = em.find(entityClass, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<T> getAll() {
        return em.createQuery("from " + entityClass.getSimpleName(), entityClass)
                .getResultList();
    }

    @Override
    @Transactional
    public void save(T t) {
        em.persist(t);
    }

    @Override
    @Transactional
    public void update(T t, String[] params) {
        em.merge(t);
    }

    @Override
    @Transactional
    public void delete(T t) {
        T attached = em.contains(t) ? t : em.merge(t);
        em.remove(attached);
    }
}
