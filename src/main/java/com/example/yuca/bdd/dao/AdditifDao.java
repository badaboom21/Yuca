package com.example.yuca.bdd.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Additif;

@Repository
public class AdditifDao extends AbstractDao<Additif> {

    public AdditifDao() {
        super(Additif.class);
    }

    @Override
    public Optional<Additif> get(long id) {
        throw new UnsupportedOperationException("Additif uses a String id (nomAdditif). Use a custom lookup method.");
    }
}
