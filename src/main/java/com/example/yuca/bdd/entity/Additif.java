package com.example.yuca.bdd.entity;

import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "additif")
public class Additif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "varchar(50)", nullable = false)
    private String nom_additif;

    @Column(columnDefinition = "double", nullable = false)
    private double qteMilligrammes;

    @ManyToMany
    @JoinTable(
            name = "produit_additif",
            joinColumns = @JoinColumn(name = "nom_additif"),
            inverseJoinColumns = @JoinColumn(name = "nom_produit"))
    Set<Produit> linkedProduits;

}

