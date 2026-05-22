package com.example.yuca.bdd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "marque")
public class Marque {

    @Id
    @Column(columnDefinition = "varchar(50)")
    private String nom;
}
