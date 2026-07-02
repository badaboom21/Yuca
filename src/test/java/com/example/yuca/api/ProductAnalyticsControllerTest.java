package com.example.yuca.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.yuca.api.dto.FrequencyDto;
import com.example.yuca.api.dto.ProductSummaryDto;
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
import com.example.yuca.bdd.service.ProductAnalyticsService;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:yuca;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProductAnalyticsControllerTest {

    @Autowired
    private ProductAnalyticsService analyticsService;

    @Autowired
    private ProduitDao produitDao;

    @Autowired
    private CategorieDao categorieDao;

    @Autowired
    private MarqueDao marqueDao;

    @Autowired
    private IngredientDao ingredientDao;

    @Autowired
    private AllergeneDao allergeneDao;

    @Autowired
    private AdditifDao additifDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            produitDao.findAll().forEach(produitDao::delete);
            categorieDao.findAll().forEach(categorieDao::delete);
            marqueDao.findAll().forEach(marqueDao::delete);
            ingredientDao.findAll().forEach(ingredientDao::delete);
            allergeneDao.findAll().forEach(allergeneDao::delete);
            additifDao.findAll().forEach(additifDao::delete);

            Categorie categorie = new Categorie("Boissons");
            Categorie categorieEaux = new Categorie("Eaux");
            categorieDao.save(categorie);
            categorieDao.save(categorieEaux);

            Marque marque = new Marque("Coca-Cola");
            Marque marqueVolvic = new Marque("Volvic");
            marqueDao.save(marque);
            marqueDao.save(marqueVolvic);

            Produit produit1 = new Produit("Coca-Cola Zero", "A", categorie, marque);
            Produit produit2 = new Produit("Sprite", "B", categorie, marque);
            Produit produit3 = new Produit("Eau plate", "C", categorieEaux, marqueVolvic);

            Ingredient eau = new Ingredient("eau", 1000);
            Ingredient sucre = new Ingredient("sucre", 500);
            Ingredient sel = new Ingredient("sel", 100);
            ingredientDao.save(eau);
            ingredientDao.save(sucre);
            ingredientDao.save(sel);
            produit1.addIngredient(eau);
            produit1.addIngredient(sucre);
            produit2.addIngredient(eau);
            produit2.addIngredient(sel);
            produit3.addIngredient(eau);

            Allergene lait = new Allergene("lait", 10);
            Allergene soja = new Allergene("soja", 20);
            allergeneDao.save(lait);
            allergeneDao.save(soja);
            produit1.addAllergene(lait);
            produit2.addAllergene(soja);
            produit3.addAllergene(lait);

            Additif colorant = new Additif("colorant", 5);
            Additif conservateur = new Additif("conservateur", 2);
            additifDao.save(colorant);
            additifDao.save(conservateur);
            produit1.addAdditif(colorant);
            produit2.addAdditif(conservateur);
            produit3.addAdditif(colorant);

            produitDao.save(produit1);
            produitDao.save(produit2);
            produitDao.save(produit3);
        });
    }

    @Test
    void shouldListTopProductsForBrand() {
        List<ProductSummaryDto> products = analyticsService.findTopProductsByBrand("Coca-Cola", 2);

        assertThat(products).hasSize(2);
        assertThat(products.get(0).name()).isEqualTo("Coca-Cola Zero");
    }

    @Test
    void shouldListTopIngredientsByFrequency() {
        List<FrequencyDto> ingredients = analyticsService.findTopIngredients(2);

        assertThat(ingredients).isNotEmpty();
        assertThat(ingredients.get(0).name()).isEqualTo("eau");
        assertThat(ingredients.get(0).count()).isEqualTo(3);
    }
}
