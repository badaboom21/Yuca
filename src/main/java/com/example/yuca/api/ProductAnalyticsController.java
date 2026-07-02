package com.example.yuca.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.yuca.api.dto.FrequencyDto;
import com.example.yuca.api.dto.ProductSummaryDto;
import com.example.yuca.bdd.service.ProductAnalyticsService;

@RestController
public class ProductAnalyticsController {

    private final ProductAnalyticsService analyticsService;

    public ProductAnalyticsController(ProductAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/products/top-by-brand")
    public List<ProductSummaryDto> getTopProductsByBrand(
            @RequestParam String brand,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByBrand(brand, limit);
    }

    @GetMapping("/products/top-by-category")
    public List<ProductSummaryDto> getTopProductsByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByCategory(category, limit);
    }

    @GetMapping("/products/top-by-brand-category")
    public List<ProductSummaryDto> getTopProductsByBrandAndCategory(
            @RequestParam String brand,
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopProductsByBrandAndCategory(brand, category, limit);
    }

    @GetMapping("/ingredients/top")
    public List<FrequencyDto> getTopIngredients(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopIngredients(limit);
    }

    @GetMapping("/allergens/top")
    public List<FrequencyDto> getTopAllergens(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopAllergens(limit);
    }

    @GetMapping("/additives/top")
    public List<FrequencyDto> getTopAdditives(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.findTopAdditives(limit);
    }
}
