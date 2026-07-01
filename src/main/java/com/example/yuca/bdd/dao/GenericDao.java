package com.example.yuca.bdd.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrat générique du DAO.
 *
 * @param <T> type de l'entité métier
 * @param <ID> type de la clé primaire
 */
public interface GenericDao<T, ID> {

    /**
     * Enregistre une entité.
     *
     * @param entity entité à enregistrer
     * @return entité enregistrée
     */
    T save(T entity);

    /**
     * Met à jour une entité.
     *
     * @param entity entité à mettre à jour
     * @return entité mise à jour
     */
    T update(T entity);

    /**
     * Recherche une entité par identifiant.
     *
     * @param id identifiant recherché
     * @return entité trouvée si elle existe
     */
    Optional<T> findById(ID id);

    /**
     * Retourne toutes les entités.
     *
     * @return liste des entités
     */
    List<T> findAll();

    /**
     * Supprime une entité.
     *
     * @param entity entité à supprimer
     */
    void delete(T entity);

    /**
     * Supprime une entité à partir de son identifiant.
     *
     * @param id identifiant de l'entité à supprimer
     */
    void deleteById(ID id);
}
