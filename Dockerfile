# Étape 1 : Build de l'application
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copie l'intégralité du projet racine (pom.xml + tous les sous-modules)
COPY . .

# Build ciblé sur philosopher-core et ses dépendances directes
RUN mvn clean package -pl philosopher-core -am -DskipTests

# Étape 2 : Image d'exécution
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copie du JAR généré
COPY --from=builder /app/philosopher-core/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]