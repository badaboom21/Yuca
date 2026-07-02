package com.example.yuca.bdd.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.yuca.api.dto.FrequencyDto;
import com.example.yuca.api.dto.ProductSummaryDto;
import com.example.yuca.bdd.dao.ProduitDao;
import com.example.yuca.bdd.entity.Additif;
import com.example.yuca.bdd.entity.Allergene;
import com.example.yuca.bdd.entity.Ingredient;
import com.example.yuca.bdd.entity.Produit;

@Service
@Transactional(readOnly = true)
public class ProductAnalyticsService {

    private final ProduitDao produitDao;

    public ProductAnalyticsService(ProduitDao produitDao) {
        this.produitDao = produitDao;
    }

    public List<ProductSummaryDto> findTopProductsByBrand(String brand, int limit) {
        return filterProducts(brand, null, limit).stream()
                .map(this::toProductSummary)
                .toList();
    }

    public List<ProductSummaryDto> findTopProductsByCategory(String category, int limit) {
        return filterProducts(null, category, limit).stream()
                .map(this::toProductSummary)
                .toList();
    }

    public List<ProductSummaryDto> findTopProductsByBrandAndCategory(String brand, String category, int limit) {
        return filterProducts(brand, category, limit).stream()
                .map(this::toProductSummary)
                .toList();
    }

    public List<FrequencyDto> findTopIngredients(int limit) {
        return buildTopFrequencyList(
                produitDao.findAll().stream()
                        .flatMap(product -> product.getIngredients().stream())
                        .map(Ingredient::getNomIngredient),
                limit);
    }

    public List<FrequencyDto> findTopAllergens(int limit) {
        return buildTopFrequencyList(
                produitDao.findAll().stream()
                        .flatMap(product -> product.getAllergenes().stream())
                        .map(Allergene::getNomAllergene),
                limit);
    }

    public List<FrequencyDto> findTopAdditives(int limit) {
        return buildTopFrequencyList(
                produitDao.findAll().stream()
                        .flatMap(product -> product.getAdditifs().stream())
                        .map(Additif::getNomAdditif),
                limit);
    }

    private List<Produit> filterProducts(String brand, String category, int limit) {
        return produitDao.findAll().stream()
                .filter(product -> brand == null || brand.isBlank() || matchesBrand(product, brand))
                .filter(product -> category == null || category.isBlank() || matchesCategory(product, category))
                .sorted(Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(limit, 1))
                .toList();
    }

    private boolean matchesBrand(Produit product, String brand) {
        return product.getMarque() != null && brand.equalsIgnoreCase(product.getMarque().getNom());
    }

    private boolean matchesCategory(Produit product, String category) {
        return product.getCategorie() != null && category.equalsIgnoreCase(product.getCategorie().getNom());
    }

    private ProductSummaryDto toProductSummary(Produit product) {
        return new ProductSummaryDto(
                product.getId(),
                product.getNom(),
                product.getMarque() != null ? product.getMarque().getNom() : null,
                product.getCategorie() != null ? product.getCategorie().getNom() : null,
                product.getGrade());
    }

    private List<FrequencyDto> buildTopFrequencyList(java.util.stream.Stream<String> names, int limit) {
        Map<String, Long> counts = names.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(limit, 1))
                .map(entry -> new FrequencyDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
