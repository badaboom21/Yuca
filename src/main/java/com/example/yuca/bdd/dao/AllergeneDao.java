package com.example.yuca.bdd.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Allergene;

@Repository
public class AllergeneDao extends AbstractDao<Allergene> {

    public AllergeneDao() {
        super(Allergene.class);
    }

    @Override
    public Optional<Allergene> get(long id) {
        throw new UnsupportedOperationException("Allergene uses a String id (nomAllergene). Use a custom lookup method.");
    }
}
