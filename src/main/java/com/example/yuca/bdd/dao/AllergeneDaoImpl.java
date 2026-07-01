package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Allergene;

@Repository
public class AllergeneDaoImpl extends AbstractJpaDao<Allergene, String> implements AllergeneDao {

    public AllergeneDaoImpl() {
        super(Allergene.class);
    }
}
