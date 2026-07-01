package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Produit;

@Repository
public class ProduitDao extends AbstractDao<Produit> {

    public ProduitDao() {
        super(Produit.class);
    }
}
