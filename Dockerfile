# Étape 1 : Build
FROM maven:3.8.7-openjdk-17-slim AS build
WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Télécharger les dépendances
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler l'application
RUN mvn clean package -DskipTests

# Étape 2 : Runtime
FROM openjdk:17-slim
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port 8081
EXPOSE 8081

# Démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]