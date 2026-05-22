package com.example.yuca.bdd.entity;

import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "varchar(50)", nullable = false)
    private String nom_ingredient;

    @Column(columnDefinition = "double", nullable = false)
    private double qteMilligrammes;

    @ManyToMany
    @JoinTable(
            name = "produit_ingredient",
            joinColumns = @JoinColumn(name = "nom_ingredient"),
            inverseJoinColumns = @JoinColumn(name = "nom_produit"))
    Set<Produit> linkedProduits;

}
