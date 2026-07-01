# Yuca

Pour le projet optimisation performance backend

## Démarrage rapide avec Docker

Vous pouvez démarrer MariaDB et l’API en une seule commande :

```bash
docker compose up --build
```

### Services disponibles

- MariaDB : localhost:3306
- API Spring Boot : http://localhost:8080

### Identifiants MariaDB

- base : yuca
- utilisateur : yuca
- mot de passe : yuca

### Arrêter les services

```bash
docker compose down
```

### Nettoyer les volumes

```bash
docker compose down -v
```

## Version locale sans Docker

Si vous préférez lancer l’application sans Docker, vous pouvez toujours utiliser MariaDB localement.

### 1. Créer la base

```sql
CREATE DATABASE yuca;
```

### 2. Vérifier les identifiants

Les paramètres par défaut dans [src/main/resources/application.properties](src/main/resources/application.properties) sont :

- utilisateur : root
- mot de passe : root

### 3. Démarrer l’application

```bash
./mvnw spring-boot:run
```
