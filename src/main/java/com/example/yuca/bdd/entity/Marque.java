package com.example.yuca.bdd.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "marque")
public class Marque {

    @Id
    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @OneToMany(mappedBy = "marque")
    private Set<Produit> produits = new HashSet<>();

    public Marque() {
    }

    public Marque(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Set<Produit> getProduits() {
        return produits;
    }

    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }

    public void addProduit(Produit produit) {
        this.produits.add(produit);
        produit.setMarque(this);
    }

    public void removeProduit(Produit produit) {
        this.produits.remove(produit);
        produit.setMarque(null);
    }
}
