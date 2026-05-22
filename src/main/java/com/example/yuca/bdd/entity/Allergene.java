package com.example.yuca.bdd.entity;

import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "allergene")
public class Allergene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "varchar(50)", nullable = false)
    private String nom_allergene;

    @Column(columnDefinition = "double", nullable = false)
    private double qteMilligrammes;

        @ManyToMany
    @JoinTable(
            name = "produit_allergene",
            joinColumns = @JoinColumn(name = "nom_allergene"),
            inverseJoinColumns = @JoinColumn(name = "nom_produit"))
    Set<Produit> linkedProduits;

}
