package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Additif;

/**
 * Implémentation JPA du DAO Additif.
 */
@Repository
public class AdditifDaoImpl extends AbstractJpaDao<Additif, String> implements AdditifDao {

    public AdditifDaoImpl() {
        super(Additif.class);
    }
}
