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
    private final Map<String, Boolean> ingredientCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> allergeneCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> additifCache = new ConcurrentHashMap<>();

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
        produitDao.save(produit);

        for (String ingredientName : parseIngredients(ingredientsText)) {
            Ingredient ingredient = saveIngredient(ingredientName);
            if (ingredient != null) {
                produit.addIngredient(ingredient);
            }
        }

        for (String allergeneName : parseAllergenes(allergenesText)) {
            Allergene allergene = saveAllergene(allergeneName);
            if (allergene != null) {
                produit.addAllergene(allergene);
            }
        }

        for (String additifName : parseAdditifs(additifsText)) {
            Additif additif = saveAdditif(additifName);
            if (additif != null) {
                produit.addAdditif(additif);
            }
        }

        importedCount.incrementAndGet();
    }

    List<String> parseIngredients(String rawIngredients) {
        return parseListValues(rawIngredients, true);
    }

    List<String> parseAllergenes(String rawAllergenes) {
        return parseListValues(rawAllergenes, false);
    }

    List<String> parseAdditifs(String rawAdditifs) {
        return parseListValues(rawAdditifs, false);
    }

    private List<String> parseListValues(String rawValue, boolean splitOnDash) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String sanitized = rawValue
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\b\\d+(?:[.,]\\d+)?\\s*%", "")
                .replaceAll("\\b[a-z]{2}:", "")
                .trim();

        String splitPattern = splitOnDash ? "[,;/|]+|\\s+-\\s+" : "[,;/|]+";
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (String candidate : sanitized.split(splitPattern)) {
            String cleaned = cleanText(candidate, ENTITY_NAME_MAX_LENGTH);
            if (!cleaned.isEmpty()) {
                parts.add(cleaned);
            }
        }

        return List.copyOf(parts);
    }

    private Ingredient saveIngredient(String name) {
        String normalized = cleanText(name, ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        ingredientCache.computeIfAbsent(normalized, key -> {
            ingredientDao.findById(key).orElseGet(() -> {
                Ingredient ingredient = new Ingredient(key, 0.0);
                ingredientDao.save(ingredient);
                return ingredient;
            });
            return Boolean.TRUE;
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

    private Allergene saveAllergene(String name) {
        String normalized = cleanText(name, ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        allergeneCache.computeIfAbsent(normalized, key -> {
            allergeneDao.findById(key).orElseGet(() -> {
                Allergene allergene = new Allergene(key, 0.0);
                allergeneDao.save(allergene);
                return allergene;
            });
            return Boolean.TRUE;
        });

        return entityManager.getReference(Allergene.class, normalized);
    }

    private Additif saveAdditif(String name) {
        String normalized = cleanText(name, ENTITY_NAME_MAX_LENGTH);
        if (normalized.isBlank()) {
            return null;
        }

        additifCache.computeIfAbsent(normalized, key -> {
            additifDao.findById(key).orElseGet(() -> {
                Additif additif = new Additif(key, 0.0);
                additifDao.save(additif);
                return additif;
            });
            return Boolean.TRUE;
        });

        return entityManager.getReference(Additif.class, normalized);
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
