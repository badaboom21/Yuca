package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Categorie;

@Repository
public class CategorieDaoImpl extends AbstractJpaDao<Categorie, String> implements CategorieDao {

    public CategorieDaoImpl() {
        super(Categorie.class);
    }
}
