package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Marque;

@Repository
public class MarqueDaoImpl extends AbstractJpaDao<Marque, String> implements MarqueDao {

    public MarqueDaoImpl() {
        super(Marque.class);
    }
}
