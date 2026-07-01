package com.example.yuca.bdd.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.yuca.bdd.dao.ProduitDao;
import com.example.yuca.bdd.entity.Produit;

@Service
@Transactional(readOnly = true)
public class ProduitServiceImpl implements ProduitService {

    private final ProduitDao produitDao;

    public ProduitServiceImpl(ProduitDao produitDao) {
        this.produitDao = produitDao;
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "produits", key = "#result.id")
    @CacheEvict(cacheNames = "produitsList", allEntries = true)
    public Produit save(Produit entity) {
        return produitDao.save(entity);
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "produits", key = "#result.id")
    @CacheEvict(cacheNames = "produitsList", allEntries = true)
    public Produit update(Produit entity) {
        return produitDao.update(entity);
    }

    @Override
    @Cacheable(cacheNames = "produits", key = "#id")
    public Optional<Produit> findById(Long id) {
        return produitDao.findById(id);
    }

    @Override
    @Cacheable(cacheNames = "produitsList")
    public List<Produit> findAll() {
        return produitDao.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"produits", "produitsList"}, key = "#entity.id")
    public void delete(Produit entity) {
        produitDao.delete(entity);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"produits", "produitsList"}, key = "#id")
    public void deleteById(Long id) {
        produitDao.deleteById(id);
    }
}
