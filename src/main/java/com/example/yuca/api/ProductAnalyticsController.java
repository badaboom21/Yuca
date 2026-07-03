package com.example.yuca.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.yuca.api.dto.FrequencyDto;
import com.example.yuca.api.dto.ProductSummaryDto;
import com.example.yuca.bdd.service.ProductAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Analytics produits", description = "Statistiques sur les produits, ingredients, allergenes et additifs.")
public class ProductAnalyticsController {

    private final ProductAnalyticsService analyticsService;

    public ProductAnalyticsController(ProductAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/products/top-by-brand")
    @Operation(summary = "Top produits par marque", description = "Retourne les produits les mieux notes pour une marque donnee.")
    public List<ProductSummaryDto> getTopProductsByBrand(
            @Parameter(description = "Nom de la marque", example = "Coca-Cola") @RequestParam String brand,
            @Parameter(description = "Nombre maximal de produits a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByBrand(brand, limit);
    }

    @GetMapping("/products/top-by-category")
    @Operation(summary = "Top produits par categorie", description = "Retourne les produits les mieux notes pour une categorie donnee.")
    public List<ProductSummaryDto> getTopProductsByCategory(
            @Parameter(description = "Nom de la categorie", example = "Sodas") @RequestParam String category,
            @Parameter(description = "Nombre maximal de produits a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByCategory(category, limit);
    }

    @GetMapping("/products/top-by-brand-category")
    @Operation(summary = "Top produits par marque et categorie", description = "Retourne les produits les mieux notes pour une marque et une categorie donnees.")
    public List<ProductSummaryDto> getTopProductsByBrandAndCategory(
            @Parameter(description = "Nom de la marque", example = "Coca-Cola") @RequestParam String brand,
            @Parameter(description = "Nom de la categorie", example = "Sodas") @RequestParam String category,
            @Parameter(description = "Nombre maximal de produits a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByBrandAndCategory(brand, category, limit);
    }

    @GetMapping("/ingredients/top")
    @Operation(summary = "Ingredients les plus frequents", description = "Retourne les ingredients les plus presents dans les produits importes.")
    public List<FrequencyDto> getTopIngredients(
            @Parameter(description = "Nombre maximal d'ingredients a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopIngredients(limit);
    }

    @GetMapping("/allergens/top")
    @Operation(summary = "Allergenes les plus frequents", description = "Retourne les allergenes les plus presents dans les produits importes.")
    public List<FrequencyDto> getTopAllergens(
            @Parameter(description = "Nombre maximal d'allergenes a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopAllergens(limit);
    }

    @GetMapping("/additives/top")
    @Operation(summary = "Additifs les plus frequents", description = "Retourne les additifs les plus presents dans les produits importes.")
    public List<FrequencyDto> getTopAdditives(
            @Parameter(description = "Nombre maximal d'additifs a retourner", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopAdditives(limit);
    }
}
