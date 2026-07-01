package com.example.yuca.bdd.service;

import java.util.List;
import java.util.Optional;

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
    public Produit save(Produit entity) {
        return produitDao.save(entity);
    }

    @Override
    @Transactional
    public Produit update(Produit entity) {
        return produitDao.update(entity);
    }

    @Override
    public Optional<Produit> findById(Long id) {
        return produitDao.findById(id);
    }

    @Override
    public List<Produit> findAll() {
        return produitDao.findAll();
    }

    @Override
    @Transactional
    public void delete(Produit entity) {
        produitDao.delete(entity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        produitDao.deleteById(id);
    }
}
