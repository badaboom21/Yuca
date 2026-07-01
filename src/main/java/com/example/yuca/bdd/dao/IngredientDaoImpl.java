package com.example.yuca.bdd.dao;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Ingredient;

/**
 * Implémentation JPA du DAO Ingredient.
 */
@Repository
public class IngredientDaoImpl extends AbstractJpaDao<Ingredient, String> implements IngredientDao {

    public IngredientDaoImpl() {
        super(Ingredient.class);
    }
}
