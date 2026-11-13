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

Le rapport de couverture de chaque module est généré dans `<module>/target/site/jacoco/index.html`.

Avant chaque push : `spotless:apply` puis `verify`. C'est ce que fait la CI.

## Structure

| Module | Rôle |
|---|---|
| `common` | Briques partagées : horloge logique de Lamport, événements des philosophes |
| `philosopher-core` | Logique pure, sans Spring : cycle du philosophe, durées aléatoires, calcul distribué sur graphes (`core.graph`) |
| `philosopher-service` | Microservice Spring Boot d'un philosophe. Selon `PHILOSOPHER_GRAPH_ROLE`, il peut aussi être Master ou Worker du calcul sur graphes |
| `monitor-service` | Reçoit les événements des philosophes et expose le tableau de bord |
| `Philosopher_supervised` | Philosophes sous la supervision d'un superviseur (partie 2) |
| `supervisor-unlocked` | Superviseur qui attribue les deux fourchettes d'un coup, sans verrou (partie 3.1) |

## Avancement

| Partie | Contenu | État |
|---|---|---|
| 1 | Philosophes gâtés, monitor, horloges logiques | fait |
| 2 | Superviseur naïf | en cours |
| 3 | Verrou, jetons distribués, ZooKeeper | à faire |
| 4 | Tests unitaires, intégration, concurrence, résilience | transversal, en cours |
| 5 | Master-Worker : degrés des nœuds, accessibilité | fait (tests et rapport en cours) |
| 6 | Docker, Kafka, Kubernetes, CI/CD | à faire |

## Lancer la partie 1 (philosophes gâtés)

Prérequis : Java 21, Maven (ou `mvnw`).

```powershell
mvn clean package
```

Un terminal par service, depuis la racine du dépôt. **Le monitor se lance toujours en premier**, sinon les premiers événements des philosophes sont perdus.

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

Variante : lancer les trois philosophes d'un coup, depuis un seul terminal (le monitor tourne déjà) :

```powershell
1..3 | ForEach-Object {
    $env:PHILOSOPHER_ID = "philosopher-$_"
    $env:SERVER_PORT = 8080 + $_
    Start-Process java -ArgumentList "-jar", "philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar"
}
Remove-Item Env:SERVER_PORT, Env:PHILOSOPHER_ID
```

Le `Remove-Item` final évite que le terminal garde `SERVER_PORT` : un monitor lancé ensuite depuis ce terminal hériterait du mauvais port.

| Variable | Rôle | Défaut |
|---|---|---|
| `PHILOSOPHER_ID` | Identifiant du philosophe | `philosopher-1` |
| `SERVER_PORT` | Port HTTP du service | `8081` |
| `MONITOR_URL` | URL du monitor | `http://localhost:8080` |

Pour tout arrêter : `Stop-Process -Name java -Force` (tue **tous** les processus Java de la machine).

Attention : `mvn clean` échoue sous Windows si un service tourne encore (jar verrouillé). Arrêter les services (Ctrl+C) avant de reconstruire.

### Limites connues

- Un événement non livré au monitor n'est pas renvoyé : le monitor affiche une « horloge manquante ».
- Redémarrer un philosophe sans redémarrer le monitor remet son horloge à 1 : ses nouveaux événements sont vus comme des doublons ou des conflits.

## Partie 3.1 : superviseur sans verrou

Le philosophe `i` demande ses **deux** fourchettes en une seule requête : `F_i` et `F_(i+1) mod N` (N = 5 par défaut). Le superviseur (`supervisor-unlocked`, port 8090) vérifie qu'elles sont libres, puis les réserve. Il n'y a **aucun verrou** : c'est voulu.

```powershell
.\mvnw.cmd -pl supervisor-unlocked -am package -DskipTests
java -jar supervisor-unlocked\target\supervisor-unlocked-0.1.0-SNAPSHOT.jar
```

| Requête | Réponse |
|---|---|
| `POST /forks/{i}/acquire` | `true` si les deux fourchettes sont accordées, `false` sinon |
| `POST /forks/{i}/release` | 200, rend les fourchettes de `i` |

**Faille** : Tomcat traite chaque requête dans un thread. « Vérifier puis réserver » n'est pas atomique : deux voisins (P0 et P1 partagent `F1`) peuvent passer le test avant que l'un des deux réserve, et obtiennent tous deux `F1`. `UnlockedForkTableRaceTest` et `SupervisorConcurrencyTest` le reproduisent avec une barrière qui force cet entrelacement.

## Partie 5 : calcul distribué sur graphe (Master-Worker)

Le même jar `philosopher-service` peut jouer trois rôles, choisis par la variable `PHILOSOPHER_GRAPH_ROLE` :

| Valeur | Rôle |
|---|---|
| absente ou `none` | philosophe seul (partie 1) |
| `worker` | Worker : calcule pour le Master |
| `master` | Master : découpe le graphe, répartit les tâches, fusionne |

| Variable | Rôle | Défaut |
|---|---|---|
| `PHILOSOPHER_GRAPH_ROLE` | Rôle (voir ci-dessus) | `none` |
| `PHILOSOPHER_GRAPH_WORKERS` | URLs des Workers, séparées par des virgules (Master uniquement) | aucune |

Le monitor n'est pas nécessaire pour cette partie. Sans lui, les philosophes affichent des avertissements « Event ... not delivered », sans conséquence.

### Lancer 2 Workers et 1 Master

```powershell
foreach ($p in 2,3) {
    $env:PHILOSOPHER_ID = "philosopher-$p"; $env:SERVER_PORT = "808$p"; $env:PHILOSOPHER_GRAPH_ROLE = "worker"
    Start-Process java -ArgumentList "-jar", "philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar"
}
$env:PHILOSOPHER_ID = "philosopher-1"; $env:SERVER_PORT = "8081"; $env:PHILOSOPHER_GRAPH_ROLE = "master"
$env:PHILOSOPHER_GRAPH_WORKERS = "http://localhost:8082,http://localhost:8083"
Start-Process java -ArgumentList "-jar", "philosopher-service\target\philosopher-service-0.1.0-SNAPSHOT.jar"
Remove-Item Env:PHILOSOPHER_ID, Env:SERVER_PORT, Env:PHILOSOPHER_GRAPH_ROLE, Env:PHILOSOPHER_GRAPH_WORKERS
```

Attendre une dizaine de secondes que les services démarrent.

### Degré de chaque nœud

Le graphe est non orienté : chaque arête ajoute 1 au degré de ses deux extrémités (une boucle compte 2).

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/graph/jobs -ContentType "application/json" -Body '{"edges":[{"from":1,"to":2},{"from":2,"to":3},{"from":3,"to":1},{"from":3,"to":4},{"from":4,"to":4}]}' | ConvertTo-Json
```

Résultat attendu : nœud 1 = 2, nœud 2 = 2, nœud 3 = 3, nœud 4 = 3.

**Principe.** Les **arêtes** sont réparties en plus de tâches que de Workers (4 par Worker par défaut). Chaque arête va à un seul Worker, ce qui exclut tout double comptage même si ses extrémités sont « possédées » par des Workers différents. Le Master additionne les degrés partiels. Les Workers sont sans état : si l'un tombe ou dépasse le timeout, sa tâche est relancée sur un autre Worker, et un résultat tardif n'est jamais compté.

### Accessibilité (chemin de `source` vers `target`)

Le graphe est **orienté** : 1→2 ne signifie pas 2→1.

```powershell
$body = '{"edges":[{"from":1,"to":2},{"from":2,"to":3},{"from":3,"to":4},{"from":5,"to":6}]}'
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/graph/reach?source=1&target=4" -ContentType "application/json" -Body $body | ConvertTo-Json
```

La réponse contient `reachable`, `levels` (niveaux du parcours), `visited` et `releaseFailures` (doit rester à 0).

**Principe.** Parcours en largeur parallèle, par niveaux. Les **nœuds** sont répartis entre les Workers (propriétaire = nœud modulo nombre de Workers), chacun garde la liste d'adjacence de ses nœuds. À chaque niveau, le Master envoie à chaque Worker les nœuds de la frontière qu'il possède, récupère leurs voisins, retire ceux déjà visités et forme la frontière suivante. Le Master est le seul à tenir l'ensemble des nœuds visités.

### Pannes

| Cas | Degrés | Accessibilité |
|---|---|---|
| Un Worker en panne | Résultat correct, tâches relancées sur un autre Worker | Erreur 502 (les nœuds du Worker sont perdus) |
| Tous les Workers en panne | Erreur 502 | Erreur 502 |

Pour simuler une panne sans tuer les autres services :

```powershell
$worker = (Get-NetTCPConnection -State Listen -LocalPort 8083).OwningProcess
Stop-Process -Id $worker -Force
```

### Benchmark

Le Master peut générer lui-même un graphe aléatoire reproductible (même graine, même graphe), pour mesurer le calcul sans le coût d'envoi du graphe depuis le client :

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/graph/benchmark?nodes=10000&edges=100000&seed=42"
```

La somme des degrés renvoyée (`degreeSum`) doit valoir exactement 2 × `edges` : contrôle d'absence de double comptage.

Le script `scripts\benchmark.ps1` lance tout seul 1, 2 puis 4 Workers, mesure trois tailles de graphe (médiane de 5 mesures après un passage de chauffe) et écrit `benchmark.csv`. Il arrête **tous** les processus Java : fermer ce qui doit être conservé avant de le lancer.

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\benchmark.ps1
```

Résultats obtenus (médiane, en ms, Master et Workers sur la même machine) :

| Arêtes | 1 Worker | 2 Workers | 4 Workers |
|---|---|---|---|
| 10 000 | 101 | 119 | 198 |
| 100 000 | 147 | 149 | 138 |
| 1 000 000 | 1294 | 1237 | 956 |

Pour les petits graphes, le surcoût de communication (HTTP, JSON) domine et ajouter des Workers ralentit. Le gain n'apparaît que pour les gros graphes et reste modeste (x1,35 avec 4 Workers) : partie séquentielle du Master (génération, découpage, sérialisation, fusion, loi d'Amdahl) et Master et Workers qui se partagent le même processeur.

### Limites connues

- Les mesures viennent d'une seule machine et d'une seule série : pas d'écart-type, les petites différences sont dans le bruit.
- Un Worker de l'accessibilité garde le graphe en mémoire pendant un calcul : s'il redémarre en plein parcours, le calcul échoue (502), il n'y a pas de reprise.
- Le parcours demande un aller-retour réseau par niveau : un graphe de grand diamètre est lent.
- Le Master garde le graphe entier en mémoire.

## Workflow Git

- `main` n'est jamais modifié directement.
- Une branche `feature/...` par partie, puis une Pull Request, la CI verte, et le merge.
- Avant de créer une branche : `git checkout main` puis `git pull`.
- Avant chaque push : `mvn spotless:apply` puis `mvn clean verify`.
- On prévient le groupe avant de modifier `PhilosopherCycle` ou `PhilosopherLoop`.