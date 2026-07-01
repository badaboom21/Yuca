package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Produit;

/**
 * Implémentation JPA du DAO Produit.
 */
@Repository
public class ProduitDaoImpl extends AbstractJpaDao<Produit, Long> implements ProduitDao {

    public ProduitDaoImpl() {
        super(Produit.class);
    }
}
