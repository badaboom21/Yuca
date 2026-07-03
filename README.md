# Yuca

## Démarrage rapide avec Docker

Pour démarrer le projet suivez les étapes suivante :

- Commencer par mettre en place l'image de MariaDB

```bash
docker compose up -d mariadb
```

- Puis build l'API

```bash
docker compose build app
```

- Puis faire lintégration des données

```bash
docker compose run --rm -e SPRING_JPA_SHOW_SQL=false -v "${PWD}\open-food-facts.csv:/app/open-food-facts.csv:ro" app ./mvnw spring-boot:run "-Dspring-boot.run.arguments=/app/open-food-facts.csv"
```

- Pour lancer le serveur

```bash
docker compose up --build
```

### Services disponibles

- MariaDB : localhost:3307
- API Spring Boot : http://localhost:8000
- Swagger UI : http://localhost:8000/swagger-ui.html
- OpenAPI JSON : http://localhost:8000/v3/api-docs

### Identifiants MariaDB

- port : 3307
- host : localhost
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
