package com.example.yuca.bdd.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Categorie;

@Repository
public class CategorieDao extends AbstractDao<Categorie> {

    public CategorieDao() {
        super(Categorie.class);
    }

    @Override
    public Optional<Categorie> get(long id) {
        throw new UnsupportedOperationException("Categorie uses a String id (nom). Use a custom lookup method.");
    }
}
