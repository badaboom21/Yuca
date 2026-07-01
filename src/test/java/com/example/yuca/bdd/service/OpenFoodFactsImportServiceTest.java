package com.example.yuca.bdd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class OpenFoodFactsImportServiceTest {

    private final OpenFoodFactsImportService service = new OpenFoodFactsImportService(null, null, null, null);

    @Test
    void shouldNormalizeIngredientsFromVariousSeparatorsAndNoise() {
        List<String> ingredients = service.parseIngredients("Sucre*, farine, _Maïs_ ; banane - Pâte (Farine 50%, Sucre 20%, Œufs 30%)");

        assertEquals(List.of("Sucre", "farine", "Maïs", "banane", "Pâte"), ingredients);
    }

    @Test
    void shouldRemovePercentagesAndParenthesesContent() {
        List<String> ingredients = service.parseIngredients("Sucre 15%, farine 50%, Maïs 35%, Pâte (Farine 50%, Sucre 20%, Œufs 30%)");

        assertEquals(List.of("Sucre", "farine", "Maïs", "Pâte"), ingredients);
    }

    @Test
    void shouldSplitUsingVariousSeparators() {
        List<String> ingredients = service.parseIngredients("sucre;farine - sel / eau, miel|cacao");
        // pipe '|' will be removed as a special char but others split
        assertEquals(List.of("sucre", "farine", "sel", "eau", "miel", "cacao"), ingredients);
    }

    @Test
    void shouldRemoveParasiticCharacters() {
        List<String> ingredients = service.parseIngredients("*Sucre*, _Maïs_, \"farine\"");
        assertEquals(List.of("Sucre", "Maïs", "farine"), ingredients);
    }

    @Test
    void shouldHandleParenthesesOnly() {
        List<String> ingredients = service.parseIngredients("Pâte (Farine 50%, Sucre 20%)");
        assertEquals(List.of("Pâte"), ingredients);
    }

    @Test
    void combinedComplexCase() {
        String input = "Sucre*, farine 10% ; _Maïs_ - Pâte (Farine 50%, Sucre 20%) / miel";
        List<String> ingredients = service.parseIngredients(input);
        assertEquals(List.of("Sucre1", "farine", "Maïs", "Pâte", "miel"), ingredients);
    }
}
