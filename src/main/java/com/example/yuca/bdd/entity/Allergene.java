package com.example.yuca.bdd.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "allergene")
public class Allergene {

    @Id
    @Column(name = "nom_allergene", nullable = false, length = 50)
    private String nomAllergene;

    @Column(name = "qte_milligrammes", nullable = false)
    private double qteMilligrammes;

    @ManyToMany(mappedBy = "allergenes")
    private Set<Produit> produits = new HashSet<>();

    public Allergene() {
    }

    public Allergene(String nomAllergene, double qteMilligrammes) {
        this.nomAllergene = nomAllergene;
        this.qteMilligrammes = qteMilligrammes;
    }

    public String getNomAllergene() {
        return nomAllergene;
    }

    public void setNomAllergene(String nomAllergene) {
        this.nomAllergene = nomAllergene;
    }

    public double getQteMilligrammes() {
        return qteMilligrammes;
    }

    public void setQteMilligrammes(double qteMilligrammes) {
        this.qteMilligrammes = qteMilligrammes;
    }

    public Set<Produit> getProduits() {
        return produits;
    }

    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }
}
