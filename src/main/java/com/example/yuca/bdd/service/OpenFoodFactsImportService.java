package com.example.yuca.bdd.service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.yuca.bdd.dao.CategorieDao;
import com.example.yuca.bdd.dao.IngredientDao;
import com.example.yuca.bdd.dao.MarqueDao;
import com.example.yuca.bdd.dao.ProduitDao;
import com.example.yuca.bdd.entity.Categorie;
import com.example.yuca.bdd.entity.Ingredient;
import com.example.yuca.bdd.entity.Marque;
import com.example.yuca.bdd.entity.Produit;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

@Service
public class OpenFoodFactsImportService {

    private final ProduitDao produitDao;
    private final IngredientDao ingredientDao;
    private final CategorieDao categorieDao;
    private final MarqueDao marqueDao;

    public OpenFoodFactsImportService() {
        this(null, null, null, null);
    }

    @Autowired
    public OpenFoodFactsImportService(
            ProduitDao produitDao,
            IngredientDao ingredientDao,
            CategorieDao categorieDao,
            MarqueDao marqueDao) {
        this.produitDao = produitDao;
        this.ingredientDao = ingredientDao;
        this.categorieDao = categorieDao;
        this.marqueDao = marqueDao;
    }

    @Transactional
    public void importCsv(Path csvPath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile(), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("Le fichier CSV est vide.");
            }

            String[] row;
            int imported = 0;
            while ((row = reader.readNext()) != null) {
                if (row.length == 0) {
                    continue;
                }

                String nom = getValue(row, header, "product_name", "product_name_fr");
                String categorieName = getValue(row, header, "categories", "categories_tags");
                String marqueName = getValue(row, header, "brands", "brands_tags");
                String ingredientsText = getValue(row, header, "ingredients_text", "ingredients_text_en", "ingredients_text_with_allergens");

                if (nom == null || nom.isBlank()) {
                    continue;
                }

                Categorie categorie = saveCategorie(categorieName);
                Marque marque = saveMarque(marqueName);

                Produit produit = new Produit(nom, null, categorie, marque);
                produitDao.save(produit);

                for (String ingredientName : parseIngredients(ingredientsText)) {
                    Ingredient ingredient = saveIngredient(ingredientName);
                    produit.addIngredient(ingredient);
                }

                imported++;
            }

            System.out.println("Import terminé : " + imported + " produits traités.");
        }
    }

    List<String> parseIngredients(String rawIngredients) {
        if (rawIngredients == null || rawIngredients.isBlank()) {
            return List.of();
        }

        String sanitized = rawIngredients
            .replaceAll("\\([^)]*\\)", "")
            .replaceAll("\\b\\d+\\s*%", "")
            .replaceAll("[^\\p{L}\\p{N}\\s,;\\/\\-\\|]", "")
            .replaceAll("[_*]+", "")
            .trim();

        List<String> parts = new ArrayList<>();
        for (String candidate : sanitized.split("[;,/\\-\\|]+")) {
            String cleaned = candidate.trim();
            if (!cleaned.isEmpty()) {
                parts.add(cleaned.replaceAll("\\s+", " ").trim());
            }
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String part : parts) {
            String normalized = part.replaceAll("\\s+", " ").trim();
            if (!normalized.isEmpty()) {
                unique.add(normalized);
            }
        }

        return new ArrayList<>(unique);
    }

    private Ingredient saveIngredient(String name) {
        String normalized = normalizeValue(name);
        if (normalized.isBlank()) {
            return null;
        }
        Ingredient existing = ingredientDao.findAll().stream()
                .filter(ingredient -> normalized.equalsIgnoreCase(ingredient.getNomIngredient()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Ingredient ingredient = new Ingredient(normalized, 0.0);
        ingredientDao.save(ingredient);
        return ingredient;
    }

    private Categorie saveCategorie(String name) {
        String normalized = normalizeValue(name);
        if (normalized.isBlank()) {
            return null;
        }
        Categorie existing = categorieDao.findAll().stream()
                .filter(categorie -> normalized.equalsIgnoreCase(categorie.getNom()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Categorie categorie = new Categorie(normalized);
        categorieDao.save(categorie);
        return categorie;
    }

    private Marque saveMarque(String name) {
        String normalized = normalizeValue(name);
        if (normalized.isBlank()) {
            return null;
        }
        Marque existing = marqueDao.findAll().stream()
                .filter(marque -> normalized.equalsIgnoreCase(marque.getNom()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Marque marque = new Marque(normalized);
        marqueDao.save(marque);
        return marque;
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String getValue(String[] row, String[] header, String... columns) {
        for (String column : columns) {
            for (int i = 0; i < header.length; i++) {
                if (column.equalsIgnoreCase(header[i])) {
                    return row.length > i ? row[i] : null;
                }
            }
        }
        return null;
    }
}
