package com.example.yuca.bdd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenFoodFactsImportServiceTest {

    private final OpenFoodFactsImportService service = new OpenFoodFactsImportService();

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
        assertEquals(List.of("Sucre", "farine", "Maïs", "Pâte", "miel"), ingredients);
    }

    @Test
    void shouldCleanReplacementCharactersAndParasiticMarkers() {
        String cleaned = service.cleanText("_Orge_ mond\uFFFD", 50);
        assertEquals("Orge mond", cleaned);
    }

    @Test
    void shouldKeepAdditiveDescriptionHyphen() {
        List<String> additifs = service.parseAdditifs("E500 - Carbonates de sodium,E500ii - Carbonate acide de sodium");
        assertEquals(List.of("E500 - Carbonates de sodium", "E500ii - Carbonate acide de sodium"), additifs);
    }

    @Test
    void shouldCleanAllergenLanguagePrefixes() {
        List<String> allergenes = service.parseAllergenes("en:gluten,en:milk,_lait_");
        assertEquals(List.of("gluten", "milk", "lait"), allergenes);
    }

    @Test
    void shouldDetectWindows1252CsvEncoding(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("off.csv");
        Files.writeString(csv, "categorie;marque;nom\nC\u00E9r\u00E9ales;Celnat;Orge mond\u00E9\n", Charset.forName("windows-1252"));

        assertEquals(Charset.forName("windows-1252"), service.detectCharset(csv));
    }

    @Test
    void shouldDetectPipeSeparatorFromPdfFormat(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("off.csv");
        Files.writeString(csv, "categorie|marque|nom\nA|B|C\n", StandardCharsets.UTF_8);

        assertEquals('|', service.detectSeparator(csv, StandardCharsets.UTF_8));
    }

    @Test
    void shouldParseNutritionNumbers() {
        assertEquals(0.499999999999999, service.parseDoubleValue("0.499999999999999"));
        assertEquals(1.25, service.parseDoubleValue("1,25"));
        assertEquals(null, service.parseDoubleValue(""));
        assertEquals(null, service.parseDoubleValue("non numeric"));
    }

    @Test
    void shouldParsePalmOilPresence() {
        assertEquals(Boolean.TRUE, service.parseBooleanValue("1"));
        assertEquals(Boolean.FALSE, service.parseBooleanValue("0"));
        assertEquals(null, service.parseBooleanValue(""));
    }

    @Test
    void shouldExtractGramQuantityFromIngredientName() {
        OpenFoodFactsImportService.ParsedListItem item = service.parseListItem("Polenta 89.9 g");

        assertEquals("Polenta", item.name());
        assertEquals(89_900.0, item.quantityMilligrammes());
    }

    @Test
    void shouldExtractPercentageAsMilligrammesForOneHundredGrams() {
        OpenFoodFactsImportService.ParsedListItem item = service.parseListItem("Farine de riz* 82 %");

        assertEquals("Farine de riz", item.name());
        assertEquals(82_000.0, item.quantityMilligrammes());
    }

    @Test
    void shouldExtractMilligramQuantityFromAdditiveOrAllergenName() {
        OpenFoodFactsImportService.ParsedListItem item = service.parseListItem("E300 - Acide ascorbique 1000 mg");

        assertEquals("E300 - Acide ascorbique", item.name());
        assertEquals(1_000.0, item.quantityMilligrammes());
    }

    @Test
    void shouldExtractQuantityAtBeginningOfIngredientName() {
        OpenFoodFactsImportService.ParsedListItem item = service.parseListItem("100%viande de boeuf");

        assertEquals("viande de boeuf", item.name());
        assertEquals(100_000.0, item.quantityMilligrammes());
    }
}
