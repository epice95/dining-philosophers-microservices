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
