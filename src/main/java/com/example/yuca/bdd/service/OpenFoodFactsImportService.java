package com.example.yuca.bdd.service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.yuca.bdd.dao.AdditifDao;
import com.example.yuca.bdd.dao.AllergeneDao;
import com.example.yuca.bdd.dao.CategorieDao;
import com.example.yuca.bdd.dao.IngredientDao;
import com.example.yuca.bdd.dao.MarqueDao;
import com.example.yuca.bdd.dao.ProduitDao;
import com.example.yuca.bdd.entity.Additif;
import com.example.yuca.bdd.entity.Allergene;
import com.example.yuca.bdd.entity.Categorie;
import com.example.yuca.bdd.entity.Ingredient;
import com.example.yuca.bdd.entity.Marque;
import com.example.yuca.bdd.entity.Produit;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

@Service
public class OpenFoodFactsImportService {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsImportService.class);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final int PRODUCT_NAME_MAX_LENGTH = 100;
    private static final int ENTITY_NAME_MAX_LENGTH = 50;
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}])([0-9]+(?:[.,][0-9]+)?)\\s*(%|kg|mg|µg|ug|gr|grammes?|g)(?=$|[^\\p{L}\\p{N}]|(?<=%)[\\p{L}])");

    private final ProduitDao produitDao;
    private final IngredientDao ingredientDao;
    private final CategorieDao categorieDao;
    private final MarqueDao marqueDao;
    private final AdditifDao additifDao;
    private final AllergeneDao allergeneDao;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, Boolean> categorieCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> marqueCache = new ConcurrentHashMap<>();
    private final Map<String, Double> ingredientCache = new ConcurrentHashMap<>();
    private final Map<String, Double> allergeneCache = new ConcurrentHashMap<>();
    private final Map<String, Double> additifCache = new ConcurrentHashMap<>();

    record ParsedListItem(String name, double quantityMilligrammes) {
    }

    public OpenFoodFactsImportService() {
        this(null, null, null, null, null, null, null);
    }

    @Autowired
    public OpenFoodFactsImportService(
            ProduitDao produitDao,
            IngredientDao ingredientDao,
            CategorieDao categorieDao,
            MarqueDao marqueDao,
            AdditifDao additifDao,
            AllergeneDao allergeneDao,
            PlatformTransactionManager transactionManager) {
        this.produitDao = produitDao;
        this.ingredientDao = ingredientDao;
        this.categorieDao = categorieDao;
        this.marqueDao = marqueDao;
        this.additifDao = additifDao;
        this.allergeneDao = allergeneDao;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    public void importCsv(Path csvPath) throws IOException, CsvValidationException {
        long startTime = System.nanoTime();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long beforeUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int cpuCount = Runtime.getRuntime().availableProcessors();
        Charset charset = detectCharset(csvPath);
        char separator = detectSeparator(csvPath, charset);

        log.info("Démarrage de l'import CSV : {} | CPU disponibles = {}", csvPath, cpuCount);

        if (transactionTemplate == null) {
            throw new IllegalStateException("TransactionTemplate non initialisé. Le service doit être instancié par Spring.");
        }

        try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(csvPath, charset))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build();
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("Le fichier CSV est vide.");
            }

            AtomicInteger importedCount = new AtomicInteger();
            AtomicInteger failedCount = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();
            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length == 0) {
                    continue;
                }
                String[] rowCopy = row.clone();
                futures.add(executor.submit(() -> {
                    try {
                        transactionTemplate.execute(status -> {
                            processRow(rowCopy, header, importedCount);
                            return null;
                        });
                    } catch (RuntimeException e) {
                        failedCount.incrementAndGet();
                        log.error("Échec du traitement d'une ligne: {}", e.getMessage(), e);
                        throw e;
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Erreur pendant l'import parallélisé", e);
                }
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    log.warn("L'import CSV n'est pas termine apres 1 heure.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Import CSV interrompu pendant l'attente de fin des traitements.", e);
            }

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            long afterUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            int currentThreadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();

            log.info("Import terminé : {} produits traités, {} échecs", importedCount.get(), failedCount.get());
            log.info("Durée totale = {} ms | Threads actifs = {} | Threads pic = {} | Mémoire avant = {} MB | Mémoire après = {} MB",
                    elapsedMillis,
                    currentThreadCount,
                    peakThreadCount,
                    beforeUsedMemory / 1_048_576,
                    afterUsedMemory / 1_048_576);
            log.info("Vitesse moyenne = {} produits/s",
                    importedCount.get() * 1000.0 / Math.max(elapsedMillis, 1));

        } catch (IOException | CsvValidationException e) {
            log.error("Impossible de lire le fichier CSV", e);
            throw e;
        }
    }

    private void processRow(String[] row, String[] header, AtomicInteger importedCount) {
        String nom = cleanText(getValue(row, header, "nom", "product_name", "product_name_fr"), PRODUCT_NAME_MAX_LENGTH);
        String grade = cleanText(getValue(row, header, "nutritionGradeFr", "nutrition_grade_fr", "nutriscore_grade"), ENTITY_NAME_MAX_LENGTH);
        String categorieName = cleanText(getValue(row, header, "categorie", "categories", "categories_tags"), ENTITY_NAME_MAX_LENGTH);
        String marqueName = cleanText(getValue(row, header, "marque", "brands", "brands_tags"), ENTITY_NAME_MAX_LENGTH);
        String ingredientsText = getValue(row, header, "ingredients", "ingredients_text", "ingredients_text_en", "ingredients_text_with_allergens");
        String allergenesText = getValue(row, header, "allergenes", "allergens", "allergens_tags");
        String additifsText = getValue(row, header, "additifs", "additives", "additives_tags");

        if (nom.isBlank()) {
            return;
        }

        Categorie categorie = saveCategorie(categorieName);
        Marque marque = saveMarque(marqueName);

        Produit produit = new Produit(nom, grade, categorie, marque);
        fillNutritionFields(produit, row, header);
        produitDao.save(produit);

        for (ParsedListItem ingredientItem : parseIngredientItems(ingredientsText)) {
            Ingredient ingredient = saveIngredient(ingredientItem);
            if (ingredient != null) {
                produit.addIngredient(ingredient);
            }
        }

        for (ParsedListItem allergeneItem : parseAllergeneItems(allergenesText)) {
            Allergene allergene = saveAllergene(allergeneItem);
            if (allergene != null) {
                produit.addAllergene(allergene);
            }
        }

        for (ParsedListItem additifItem : parseAdditifItems(additifsText)) {
            Additif additif = saveAdditif(additifItem);
            if (additif != null) {
                produit.addAdditif(additif);
            }
        }

        importedCount.incrementAndGet();
    }

    private void fillNutritionFields(Produit produit, String[] row, String[] header) {
        produit.setEnergie100g(parseDoubleValue(getValue(row, header, "energie100g", "energy_100g", "energy-kj_100g")));
        produit.setGraisse100g(parseDoubleValue(getValue(row, header, "graisse100g", "fat_100g")));
        produit.setSucres100g(parseDoubleValue(getValue(row, header, "sucres100g", "sugars_100g")));
        produit.setFibres100g(parseDoubleValue(getValue(row, header, "fibres100g", "fiber_100g", "fibre_100g")));
        produit.setProteines100g(parseDoubleValue(getValue(row, header, "proteines100g", "proteins_100g")));
        produit.setSel100g(parseDoubleValue(getValue(row, header, "sel100g", "salt_100g")));
        produit.setVitA100g(parseDoubleValue(getValue(row, header, "vitA100g", "vitamin-a_100g")));
        produit.setVitD100g(parseDoubleValue(getValue(row, header, "vitD100g", "vitamin-d_100g")));
        produit.setVitE100g(parseDoubleValue(getValue(row, header, "vitE100g", "vitamin-e_100g")));
        produit.setVitK100g(parseDoubleValue(getValue(row, header, "vitK100g", "vitamin-k_100g")));
        produit.setVitC100g(parseDoubleValue(getValue(row, header, "vitC100g", "vitamin-c_100g")));
        produit.setVitB1100g(parseDoubleValue(getValue(row, header, "vitB1100g", "vitamin-b1_100g")));
        produit.setVitB2100g(parseDoubleValue(getValue(row, header, "vitB2100g", "vitamin-b2_100g")));
        produit.setVitPP100g(parseDoubleValue(getValue(row, header, "vitPP100g", "vitamin-pp_100g", "vitamin-b3_100g")));
        produit.setVitB6100g(parseDoubleValue(getValue(row, header, "vitB6100g", "vitamin-b6_100g")));
        produit.setVitB9100g(parseDoubleValue(getValue(row, header, "vitB9100g", "vitamin-b9_100g")));
        produit.setVitB12100g(parseDoubleValue(getValue(row, header, "vitB12100g", "vitamin-b12_100g")));
        produit.setCalcium100g(parseDoubleValue(getValue(row, header, "calcium100g", "calcium_100g")));
        produit.setMagnesium100g(parseDoubleValue(getValue(row, header, "magnesium100g", "magnesium_100g")));
        produit.setIron100g(parseDoubleValue(getValue(row, header, "iron100g", "iron_100g")));
        produit.setFer100g(parseDoubleValue(getValue(row, header, "fer100g")));
        produit.setBetaCarotene100g(parseDoubleValue(getValue(row, header, "betaCarotene100g", "beta-carotene_100g")));
        produit.setPresenceHuilePalme(parseBooleanValue(getValue(row, header, "presenceHuilePalme", "palm_oil", "palm-oil")));
    }

    Double parseDoubleValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace(',', '.')
                .replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(normalized);
        } catch (NumberFormatException e) {
            log.debug("Valeur numerique ignoree pendant l'import CSV : {}", value);
            return null;
        }
    }

    Boolean parseBooleanValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "1", "true", "oui", "yes" -> Boolean.TRUE;
            case "0", "false", "non", "no" -> Boolean.FALSE;
            default -> null;
        };
    }

    List<String> parseIngredients(String rawIngredients) {
        return parseIngredientItems(rawIngredients).stream()
                .map(ParsedListItem::name)
                .toList();
    }

    List<String> parseAllergenes(String rawAllergenes) {
        return parseAllergeneItems(rawAllergenes).stream()
                .map(ParsedListItem::name)
                .toList();
    }

    List<String> parseAdditifs(String rawAdditifs) {
        return parseAdditifItems(rawAdditifs).stream()
                .map(ParsedListItem::name)
                .toList();
    }

    List<ParsedListItem> parseIngredientItems(String rawIngredients) {
        return parseListItems(rawIngredients, true);
    }

    List<ParsedListItem> parseAllergeneItems(String rawAllergenes) {
        return parseListItems(rawAllergenes, false);
    }

    List<ParsedListItem> parseAdditifItems(String rawAdditifs) {
        return parseListItems(rawAdditifs, false);
    }

    private List<ParsedListItem> parseListItems(String rawValue, boolean splitOnDash) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String sanitized = rawValue
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\b[a-z]{2}:", "")
                .trim();

        String splitPattern = splitOnDash ? "[,;/|]+|\\s+-\\s+" : "[,;/|]+";
        Map<String, ParsedListItem> parts = new java.util.LinkedHashMap<>();
        for (String candidate : sanitized.split(splitPattern)) {
            ParsedListItem parsedItem = parseListItem(candidate);
            if (!parsedItem.name().isEmpty()) {
                parts.merge(parsedItem.name(), parsedItem, (current, incoming) ->
                        incoming.quantityMilligrammes() > current.quantityMilligrammes() ? incoming : current);
            }
        }

        return List.copyOf(parts.values());
    }

    ParsedListItem parseListItem(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new ParsedListItem("", 0.0);
        }

        String withoutNotes = removeQuantityContextNotes(rawValue);
        Matcher matcher = QUANTITY_PATTERN.matcher(withoutNotes);
        double quantityMilligrammes = 0.0;
        if (matcher.find()) {
            quantityMilligrammes = convertQuantityToMilligrammes(matcher.group(1), matcher.group(2));
        }

        String withoutQuantity = QUANTITY_PATTERN.matcher(withoutNotes).replaceAll(" ");
        String cleanedName = cleanText(withoutQuantity, ENTITY_NAME_MAX_LENGTH)
                .replaceAll("(?i)^(?:de|d['’])\\s+", "")
                .trim();

        if (isIgnoredListItemName(cleanedName)) {
            return new ParsedListItem("", 0.0);
        }

        return new ParsedListItem(cleanText(cleanedName, ENTITY_NAME_MAX_LENGTH), quantityMilligrammes);
    }

    private String removeQuantityContextNotes(String value) {
        return value
                .replaceAll("(?i)\\b(?:quantit[eé]s?\\s+)?mise?s?\\s+en\\s+(?:oeuvre|œuvre)\\b.*", "")
                .replaceAll("(?i)\\bmis(?:e|es)?\\s+en\\s+(?:oeuvre|œuvre)\\b.*", "")
                .replaceAll("(?i)\\bpr[eé]par[ée]e?\\s+avec\\b.*", "")
                .replaceAll("(?i)\\bteneur\\b.*", "")
                .replaceAll("(?i)\\b(?:pour|par)\\s+\\d+(?:[.,]\\d+)?\\s*(?:kg|g|mg|µg|ug)\\b.*", "");
    }

    private boolean isIgnoredListItemName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String normalized = name.toLowerCase();
        return normalized.equals("minimum")
                || normalized.equals("produit")
                || normalized.equals("produit fini")
                || normalized.contains("produit fini")
                || normalized.startsWith("pour ")
                || normalized.startsWith("par ")
                || normalized.startsWith("quantite ")
                || normalized.startsWith("quantité ");
    }

    private double convertQuantityToMilligrammes(String rawNumber, String rawUnit) {
        Double value = parseDoubleValue(rawNumber);
        if (value == null) {
            return 0.0;
        }

        String unit = rawUnit.toLowerCase();
        return switch (unit) {
            case "kg" -> value * 1_000_000.0;
            case "g", "gr", "gramme", "grammes" -> value * 1_000.0;
            case "mg" -> value;
            case "µg", "ug" -> value / 1_000.0;
            case "%" -> value * 1_000.0;
            default -> 0.0;
        };
    }

    private Ingredient saveIngredient(ParsedListItem item) {
        String normalized = cleanText(item.name(), ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        ingredientCache.compute(normalized, (key, cachedQuantity) -> {
            double quantity = item.quantityMilligrammes();
            if (cachedQuantity == null) {
                Ingredient ingredient = ingredientDao.findById(key).orElseGet(() -> {
                    Ingredient newIngredient = new Ingredient(key, quantity);
                    ingredientDao.save(newIngredient);
                    return newIngredient;
                });
                return updateIngredientQuantityIfHigher(ingredient, quantity);
            }
            if (quantity > cachedQuantity) {
                ingredientDao.findById(key).ifPresent(ingredient -> updateIngredientQuantityIfHigher(ingredient, quantity));
                return quantity;
            }
            return cachedQuantity;
        });

        return entityManager.getReference(Ingredient.class, normalized);
    }

    private Categorie saveCategorie(String name) {
        String normalized = cleanText(name, ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        categorieCache.computeIfAbsent(normalized, key -> {
            categorieDao.findById(key).orElseGet(() -> {
                Categorie categorie = new Categorie(key);
                categorieDao.save(categorie);
                return categorie;
            });
            return Boolean.TRUE;
        });

        return entityManager.getReference(Categorie.class, normalized);
    }

    private Marque saveMarque(String name) {
        String normalized = cleanText(name, ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        marqueCache.computeIfAbsent(normalized, key -> {
            marqueDao.findById(key).orElseGet(() -> {
                Marque marque = new Marque(key);
                marqueDao.save(marque);
                return marque;
            });
            return Boolean.TRUE;
        });

        return entityManager.getReference(Marque.class, normalized);
    }

    private Allergene saveAllergene(ParsedListItem item) {
        String normalized = cleanText(item.name(), ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        allergeneCache.compute(normalized, (key, cachedQuantity) -> {
            double quantity = item.quantityMilligrammes();
            if (cachedQuantity == null) {
                Allergene allergene = allergeneDao.findById(key).orElseGet(() -> {
                    Allergene newAllergene = new Allergene(key, quantity);
                    allergeneDao.save(newAllergene);
                    return newAllergene;
                });
                return updateAllergeneQuantityIfHigher(allergene, quantity);
            }
            if (quantity > cachedQuantity) {
                allergeneDao.findById(key).ifPresent(allergene -> updateAllergeneQuantityIfHigher(allergene, quantity));
                return quantity;
            }
            return cachedQuantity;
        });

        return entityManager.getReference(Allergene.class, normalized);
    }

    private Additif saveAdditif(ParsedListItem item) {
        String normalized = cleanText(item.name(), ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        additifCache.compute(normalized, (key, cachedQuantity) -> {
            double quantity = item.quantityMilligrammes();
            if (cachedQuantity == null) {
                Additif additif = additifDao.findById(key).orElseGet(() -> {
                    Additif newAdditif = new Additif(key, quantity);
                    additifDao.save(newAdditif);
                    return newAdditif;
                });
                return updateAdditifQuantityIfHigher(additif, quantity);
            }
            if (quantity > cachedQuantity) {
                additifDao.findById(key).ifPresent(additif -> updateAdditifQuantityIfHigher(additif, quantity));
                return quantity;
            }
            return cachedQuantity;
        });

        return entityManager.getReference(Additif.class, normalized);
    }

    private double updateIngredientQuantityIfHigher(Ingredient ingredient, double quantityMilligrammes) {
        if (quantityMilligrammes > ingredient.getQteMilligrammes()) {
            ingredient.setQteMilligrammes(quantityMilligrammes);
            ingredientDao.update(ingredient);
        }
        return Math.max(ingredient.getQteMilligrammes(), quantityMilligrammes);
    }

    private double updateAllergeneQuantityIfHigher(Allergene allergene, double quantityMilligrammes) {
        if (quantityMilligrammes > allergene.getQteMilligrammes()) {
            allergene.setQteMilligrammes(quantityMilligrammes);
            allergeneDao.update(allergene);
        }
        return Math.max(allergene.getQteMilligrammes(), quantityMilligrammes);
    }

    private double updateAdditifQuantityIfHigher(Additif additif, double quantityMilligrammes) {
        if (quantityMilligrammes > additif.getQteMilligrammes()) {
            additif.setQteMilligrammes(quantityMilligrammes);
            additifDao.update(additif);
        }
        return Math.max(additif.getQteMilligrammes(), quantityMilligrammes);
    }

    String cleanText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value
                .replace('\u00A0', ' ')
                .replace('\u200B', ' ')
                .replace('\uFFFD', ' ')
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("[_*\"`]+", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "")
                .trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength).trim();
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

    Charset detectCharset(Path csvPath) throws IOException {
        byte[] bytes = Files.readAllBytes(csvPath);
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return WINDOWS_1252;
        }
    }

    char detectSeparator(Path csvPath, Charset charset) throws IOException {
        try (var reader = Files.newBufferedReader(csvPath, charset)) {
            String header = reader.readLine();
            if (header == null) {
                return ';';
            }

            int semicolonCount = countOccurrences(header, ';');
            int pipeCount = countOccurrences(header, '|');
            return pipeCount > semicolonCount ? '|' : ';';
        }
    }

    private int countOccurrences(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }
}
