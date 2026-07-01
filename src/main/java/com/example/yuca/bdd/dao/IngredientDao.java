package com.example.yuca.bdd.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.yuca.bdd.entity.Ingredient;

@Repository
public class IngredientDao extends AbstractDao<Ingredient> {

    public IngredientDao() {
        super(Ingredient.class);
    }

    @Override
    public Optional<Ingredient> get(long id) {
        throw new UnsupportedOperationException("Ingredient uses a String id (nomIngredient). Use a custom lookup method.");
    }
}
