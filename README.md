# Le dîner des philosophes en microservices

![CI](https://github.com/epice95/dining-philosophers-microservices/actions/workflows/ci.yml/badge.svg)

Projet web : du comportement naïf à l'exclusion
mutuelle distribuée, puis calcul distribué sur graphes (Master-Worker).

## Prérequis

- JDK 21 (Temurin recommandé)
- Git
- Docker (à partir du jalon M2)

Maven n'est pas nécessaire : le projet utilise le Maven Wrapper.

## Commandes

Windows (PowerShell) :

```powershell
.\mvnw.cmd verify            # compile, teste, vérifie le format, analyse, couverture
.\mvnw.cmd spotless:apply    # reformate le code automatiquement
```

Linux / macOS :

```bash
./mvnw verify
./mvnw spotless:apply
```

Le rapport de couverture est généré dans `common/target/site/jacoco/index.html`.

## Structure

| Module | Rôle |
|---|---|
| `common` | Briques partagées (horloge logique de Lamport, etc.) |

## Avancement

| Jalon | Contenu | État |
|---|---|---|
| M1 | Socle de build, qualité, CI | en cours |
| M2 | Philosophes gâtés, monitor, Docker | à faire |

## Lancer la partie 1 (philosophes gâtés)

Prérequis : Java 21, Maven (ou `mvnw`).

```powershell
mvn clean package
```

Un terminal par service, depuis la racine du dépôt.

**Monitor** (port 8080, tableau de bord sur http://localhost:8080) :

```powershell
java -jar monitor-service\target\monitor-service-0.1.0-SNAPSHOT.jar
```

**Philosophes** (un ID et un port différents chacun) :

```powershell
$env:PHILOSOPHER_ID="philosopher-1"; $env:SERVER_PORT="8081"; java -jar philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar
$env:PHILOSOPHER_ID="philosopher-2"; $env:SERVER_PORT="8082"; java -jar philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar
$env:PHILOSOPHER_ID="philosopher-3"; $env:SERVER_PORT="8083"; java -jar philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar
```

| Variable | Rôle | Défaut |
|---|---|---|
| `PHILOSOPHER_ID` | Identifiant du philosophe | `philosopher-1` |
| `SERVER_PORT` | Port HTTP du service | `8081` |
| `MONITOR_URL` | URL du monitor | `http://localhost:8080` |

Attention : `mvn clean` échoue sous Windows si un service tourne encore (jar verrouillé). Arrêter les services (Ctrl+C) avant de reconstruire.

### Limites connues

- Un événement non livré au monitor n'est pas renvoyé : le monitor affiche une « horloge manquante ».
- Redémarrer un philosophe sans redémarrer le monitor remet son horloge à 1 : ses nouveaux événements sont vus comme des doublons ou des conflits.