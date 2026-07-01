package com.example.yuca.bdd.service;

import java.util.List;
import java.util.Optional;

import com.example.yuca.bdd.dao.GenericDao;

/**
 * Implémentation de base des services métier avec des opérations CRUD communes.
 *
 * @param <T> type de l'entité métier
 * @param <ID> type de la clé primaire
 */
public abstract class AbstractService<T, ID> implements GenericService<T, ID> {

    private final GenericDao<T, ID> genericDao;

    protected AbstractService(GenericDao<T, ID> genericDao) {
        this.genericDao = genericDao;
    }

    @Override
    public T save(T entity) {
        return genericDao.save(entity);
    }

    @Override
    public T update(T entity) {
        return genericDao.update(entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        return genericDao.findById(id);
    }

    @Override
    public List<T> findAll() {
        return genericDao.findAll();
    }

    @Override
    public void delete(T entity) {
        genericDao.delete(entity);
    }

    @Override
    public void deleteById(ID id) {
        genericDao.deleteById(id);
    }
}
