package com.example.yuca.bdd.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Marque;

@Repository
public class MarqueDao extends AbstractDao<Marque> {

    public MarqueDao() {
        super(Marque.class);
    }

    @Override
    public Optional<Marque> get(long id) {
        throw new UnsupportedOperationException("Marque uses a String id (nom). Use a custom lookup method.");
    }
}
