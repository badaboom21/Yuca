package com.example.yuca.bdd.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.yuca.bdd.dao.ProduitDao;
import com.example.yuca.bdd.entity.Produit;

class ProduitServiceCacheTest {

    @Test
    void shouldCacheFindByIdResults() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            ProduitService service = context.getBean(ProduitService.class);
            StubProduitDao dao = context.getBean(StubProduitDao.class);

            Produit produit = new Produit();
            setId(produit, 42L);
            produit.setNom("Produit test");
            dao.store.put(42L, produit);

            Optional<Produit> first = service.findById(42L);
            Optional<Produit> second = service.findById(42L);

            assertTrue(first.isPresent());
            assertSame(produit, first.orElseThrow());
            assertSame(produit, second.orElseThrow());
            assertEquals(1, dao.findByIdCalls);
        }
    }

    private static void setId(Produit produit, Long id) throws Exception {
        Field idField = Produit.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(produit, id);
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        StubProduitDao stubProduitDao() {
            return new StubProduitDao();
        }

        @Bean
        ProduitService produitService(StubProduitDao dao) {
            return new ProduitServiceImpl(dao);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("produits", "produitsList");
        }
    }

    static class StubProduitDao implements ProduitDao {
        private final Map<Long, Produit> store = new HashMap<>();
        private int findByIdCalls;

        @Override
        public Produit save(Produit entity) {
            return entity;
        }

        @Override
        public Produit update(Produit entity) {
            return entity;
        }

        @Override
        public Optional<Produit> findById(Long id) {
            findByIdCalls++;
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Produit> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void delete(Produit entity) {
            store.remove(entity.getId());
        }

        @Override
        public void deleteById(Long id) {
            store.remove(id);
        }
    }
}
