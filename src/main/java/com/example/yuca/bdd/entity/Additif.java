package com.example.yuca.bdd.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "additif")
public class Additif {

    @Id
    @Column(name = "nom_additif", nullable = false, length = 50)
    private String nomAdditif;

    @Column(name = "qte_milligrammes", nullable = false)
    private double qteMilligrammes;

    @ManyToMany(mappedBy = "additifs")
    private Set<Produit> produits = new HashSet<>();

    public Additif() {
    }

    public Additif(String nomAdditif, double qteMilligrammes) {
        this.nomAdditif = nomAdditif;
        this.qteMilligrammes = qteMilligrammes;
    }

    public String getNomAdditif() {
        return nomAdditif;
    }

    public void setNomAdditif(String nomAdditif) {
        this.nomAdditif = nomAdditif;
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

