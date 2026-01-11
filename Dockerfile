# Étape 1 : Utiliser une image de base Java 17 (Alpine pour réduire la taille)
# eclipse-temurin est une distribution OpenJDK maintenue par la fondation Eclipse
FROM eclipse-temurin:17-jdk-alpine

# Étape 2 : Définir le répertoire de travail dans le conteneur
# Tous les fichiers seront copiés dans ce répertoire
WORKDIR /app

# Étape 3 : Copier le fichier JAR construit par Maven dans le conteneur
# Le JAR doit être construit avant avec : mvn clean package -DskipTests
# On le renomme en app.jar pour simplifier la commande d'exécution
COPY target/demo-k8s-0.0.1-SNAPSHOT.jar app.jar

# Étape 4 : Exposer le port 8080
# Cela indique que le conteneur écoute sur ce port (documentation uniquement)
EXPOSE 8080

# Étape 5 : Définir la commande d'exécution au démarrage du conteneur
# Cette commande lance l'application Spring Boot
ENTRYPOINT ["java","-jar", "app.jar"]

