# TP 33 - Déploiement d'une application Spring Boot sur Kubernetes

Ce TP vous guide à travers le processus de conteneurisation et de déploiement d'une application Spring Boot sur un cluster Kubernetes local (Minikube).

##  Objectifs pédagogiques

À la fin de ce lab, l'étudiant est capable de :

1. Conteneuriser une application Spring Boot avec Docker
2. Créer les manifests Kubernetes de base : **Deployment** et **Service**
3. Déployer l'application sur un cluster Kubernetes local (par exemple Minikube)
4. Exposer l'API Spring Boot vers l'extérieur du cluster
5. Vérifier le fonctionnement et observer les pods

##  Scénario

Une petite API REST Spring Boot expose un endpoint `/api/hello` qui retourne un message JSON.

**Objectif :** déployer cette API sur Kubernetes et l'exposer via un Service de type NodePort.

##  Pré-requis techniques

- Java 17 ou 21 installé
- Maven installé
- Docker installé et en fonctionnement
- Minikube ou autre cluster Kubernetes local (kind, k3d, etc.)
- **kubectl** configuré pour accéder au cluster

Les exemples ci-dessous utilisent Minikube.

---

##  Étapes du TP

### Étape 1 - Création d'un petit projet Spring Boot

#### 1. Structure minimale

Le projet Maven est déjà créé avec le groupe `com.example` et l'artifact `demo-k8s`.

Le fichier `pom.xml` contient :
- Spring Boot 3.3.0
- Dépendance `spring-boot-starter-web` pour l'API REST
- Dépendance `spring-boot-starter-actuator` pour les health checks

#### 2. Classe principale

La classe principale `DemoK8sApplication.java` se trouve dans :
```
src/main/java/com/example/demok8s/DemoK8sApplication.java
```

#### 3. Contrôleur REST

Le contrôleur `HelloController.java` expose l'endpoint `/api/hello` :
```
src/main/java/com/example/demok8s/api/HelloController.java
```

#### 4. Configuration de port

Le fichier `application.properties` configure le port 8080 :
```
src/main/resources/application.properties
```

#### Test local (optionnel)

Pour tester l'application localement avant la conteneurisation :

```bash
# Dans le dossier du projet
mvn spring-boot:run

# Dans un autre terminal, appeler l'API
curl http://localhost:8080/api/hello
```

Vous devriez recevoir une réponse JSON :
```json
{
  "message": "Hello from Spring Boot on Kubernetes",
  "status": "OK"
}
```

---

### Étape 2 - Création de l'image Docker

#### 1. Construction du JAR

Construire le fichier JAR de l'application :

```bash
mvn clean package -DskipTests
```

Le JAR se trouve généralement dans `target/demo-k8s-0.0.1-SNAPSHOT.jar`.

#### 2. Dockerfile

Un fichier `Dockerfile` est présent à la racine du projet. Il utilise :
- Image de base : `eclipse-temurin:17-jdk-alpine` (Java 17)
- Copie le JAR dans le conteneur
- Expose le port 8080
- Lance l'application avec `java -jar`

#### 3. Construction de l'image

Construire l'image Docker avec un tag :

```bash
docker build -t demo-k8s:1.0.0 .
```

#### 4. Test de l'image en local (optionnel)

Tester l'image Docker localement :

```bash
# Lancer le conteneur
docker run -p 8080:8080 demo-k8s:1.0.0

# Dans un autre terminal, tester l'API
curl http://localhost:8080/api/hello
```

---

### Étape 3 - Préparation de Minikube

#### 1. Démarrage du cluster

Démarrer le cluster Minikube :

```bash
minikube start
```

#### 2. Utilisation de l'image Docker locale avec Minikube

Avec Minikube, une pratique simple consiste à construire l'image *dans* l'environnement Docker de Minikube :

```bash
# Configurer l'environnement Docker pour utiliser celui de Minikube
eval $(minikube docker-env)

# Construire l'image dans l'environnement Docker de Minikube
docker build -t demo-k8s:1.0.0 .
```

**Note :** À partir de ce moment, le cluster peut voir l'image `demo-k8s:1.0.0`.

**Important :** Pour revenir à votre environnement Docker local, exécutez :
```bash
eval $(minikube docker-env -u)
```

---

### Étape 4 - Création d'un namespace dédié

Créer un namespace pour ce lab :

```bash
kubectl create namespace lab-k8s
```

**Vérification :**

```bash
kubectl get namespaces
```

Vous devriez voir `lab-k8s` dans la liste.

---

### Étape 5 - Manifest Kubernetes : Deployment

#### 1. Fichier k8s-deployment.yaml

Le fichier `k8s-deployment.yaml` contient la définition du Deployment avec :
- 2 réplicas de l'application
- Image `demo-k8s:1.0.0`
- Port 8080
- Variable d'environnement `APP_MESSAGE`
- Readiness probe sur `/api/hello`
- Liveness probe sur `/actuator/health`

**Remarque :** Pour la livenessProbe qui appelle `/actuator/health, l'endpoint Actuator doit exister (dépendance spring-boot-starter-actuator + configuration). Sinon, retirer la livenessProbe ou la pointer vers `/api/hello`.

#### 2. Application du manifest

Appliquer le Deployment :

```bash
kubectl apply -f k8s-deployment.yaml
```

#### 3. Vérification des pods

Vérifier que les pods sont créés et en cours d'exécution :

```bash
# Lister les pods dans le namespace lab-k8s
kubectl get pods -n lab-k8s

# Obtenir plus de détails sur le Deployment
kubectl describe deployment demo-k8s-deployment -n lab-k8s
```

Attendre que les pods soient en état `Running` et `Ready` (1/1).

---

### Étape 6 - Manifest Kubernetes : Service (NodePort)

#### 1. Fichier k8s-service.yaml

Le fichier `k8s-service.yaml` définit un Service de type NodePort qui :
- Expose l'application sur le port 30080 du node
- Route le trafic vers les pods avec le label `app: demo-k8s`
- Utilise le port 8080 du conteneur

#### 2. Application du Service

Appliquer le Service :

```bash
kubectl apply -f k8s-service.yaml
```

#### 3. Vérification

Vérifier que le Service est créé :

```bash
kubectl get svc -n lab-k8s
```

Vous devriez voir le service `demo-k8s-service` avec le type `NodePort` et le port `30080`.

---

### Étape 7 - Test d'accès à l'API via Kubernetes

#### 1. Récupération de l'IP du node Minikube

Obtenir l'adresse IP du node Minikube :

```bash
minikube ip
```

Supposons que l'IP retournée soit `192.168.49.2` (votre IP peut être différente).

#### 2. Appel de l'API

Le service est exposé sur le port 30080 (défini dans le YAML) :

```bash
curl http://192.168.49.2:30080/api/hello
```

**Remplacez `192.168.49.2` par l'IP retournée par `minikube ip`.**

Une réponse en JSON est attendue, du type :

```json
{
  "message": "Hello from Kubernetes Deployment",
  "status": "OK"
}
```

---

### Étape 8 - Observation et diagnostic

#### 1. Liste des pods et services

```bash
# Lister les pods
kubectl get pods -n lab-k8s

# Lister les services
kubectl get svc -n lab-k8s
```

#### 2. Logs d'un pod

Récupérer le nom d'un pod, par exemple `demo-k8s-deployment-XXXXX` :

```bash
# Voir les logs d'un pod spécifique
kubectl logs demo-k8s-deployment-XXXXX -n lab-k8s

# Voir les logs en temps réel (suivre)
kubectl logs -f demo-k8s-deployment-XXXXX -n lab-k8s
```

#### 3. Accès inside cluster (optionnel)

Pour tester l'accès depuis l'intérieur du cluster :

```bash
# Créer un pod temporaire avec curl
kubectl run curl-pod -n lab-k8s --image=alpine/curl -it -- sh

# Dans le pod, appeler le service
curl http://demo-k8s-service:8080/api/hello

# Sortir du pod
exit

# Supprimer le pod de test
kubectl delete pod curl-pod -n lab-k8s
```

---

### Étape 9 - Variante avec ConfigMap (optionnel)

Cette étape montre comment externaliser la configuration dans une ConfigMap.

#### 1. ConfigMap

Créer la ConfigMap :

```bash
kubectl apply -f k8s-configmap.yaml
```

Vérifier :

```bash
kubectl get configmap -n lab-k8s
kubectl describe configmap demo-k8s-config -n lab-k8s
```

#### 2. Adapter le Deployment pour lire la ConfigMap

Le fichier `k8s-deployment-configmap.yaml` montre comment modifier le Deployment pour utiliser la ConfigMap.

**Option 1 :** Modifier directement `k8s-deployment.yaml` en remplaçant la section `env` :

```yaml
env:
  - name: APP_MESSAGE
    valueFrom:
      configMapKeyRef:
        name: demo-k8s-config
        key: app.message
```

**Option 2 :** Utiliser le fichier `k8s-deployment-configmap.yaml` :

```bash
kubectl apply -f k8s-deployment-configmap.yaml
```

#### 3. Utiliser APP_MESSAGE côté Spring Boot

Le contrôleur `HelloController.java` utilise déjà `@Value("${APP_MESSAGE:...}")` pour lire la variable d'environnement.

**Reconstruire et redéployer :**

```bash
# 1. Reconstruire le JAR
mvn clean package -DskipTests

# 2. Reconstruire l'image Docker dans Minikube
eval $(minikube docker-env)
docker build -t demo-k8s:1.0.0 .

# 3. Redéployer (forcer le redémarrage des pods)
kubectl rollout restart deployment demo-k8s-deployment -n lab-k8s

# 4. Attendre que les pods redémarrent
kubectl get pods -n lab-k8s -w
```

**Tester à nouveau l'endpoint :**

```bash
curl http://$(minikube ip):30080/api/hello
```

Le message doit maintenant refléter la valeur de la ConfigMap : `"Hello from ConfigMap in Kubernetes"`.

---

### Étape 10 – Nettoyage du lab

Pour nettoyer le cluster :

```bash
# Supprimer les ressources créées
kubectl delete -f k8s-service.yaml
kubectl delete -f k8s-deployment.yaml
kubectl delete -f k8s-configmap.yaml

# Supprimer le namespace (supprime toutes les ressources dans le namespace)
kubectl delete namespace lab-k8s
```

Pour arrêter Minikube :

```bash
minikube stop
```

Pour supprimer complètement le cluster Minikube :

```bash
minikube delete
```

---

## 📁 Structure du projet

```
.
├── pom.xml                              # Configuration Maven
├── Dockerfile                           # Image Docker
├── k8s-deployment.yaml                  # Manifest Deployment Kubernetes
├── k8s-service.yaml                     # Manifest Service Kubernetes
├── k8s-configmap.yaml                   # Manifest ConfigMap (optionnel)
├── k8s-deployment-configmap.yaml        # Deployment avec ConfigMap (optionnel)
├── README.md                            # Ce fichier
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── demok8s/
        │               ├── DemoK8sApplication.java
        │               └── api/
        │                   └── HelloController.java
        └── resources/
            └── application.properties
```

---

## 🔍 Commandes utiles

### Kubernetes

```bash
# Lister les ressources
kubectl get all -n lab-k8s

# Décrire une ressource
kubectl describe deployment demo-k8s-deployment -n lab-k8s
kubectl describe service demo-k8s-service -n lab-k8s

# Voir les événements
kubectl get events -n lab-k8s --sort-by='.lastTimestamp'

# Exécuter une commande dans un pod
kubectl exec -it <pod-name> -n lab-k8s -- sh

# Port-forward (alternative à NodePort pour le développement)
kubectl port-forward svc/demo-k8s-service 8080:8080 -n lab-k8s
```

### Docker

```bash
# Lister les images
docker images

# Voir les conteneurs en cours d'exécution
docker ps

# Voir les logs d'un conteneur
docker logs <container-id>
```

### Minikube

```bash
# Voir le statut de Minikube
minikube status

# Ouvrir le dashboard Kubernetes
minikube dashboard

# Voir les services exposés
minikube service list
```

---

##  Dépannage

### Les pods ne démarrent pas

1. Vérifier les logs : `kubectl logs <pod-name> -n lab-k8s`
2. Vérifier les événements : `kubectl get events -n lab-k8s`
3. Vérifier que l'image existe : `docker images` (dans l'environnement Minikube)

### L'API ne répond pas

1. Vérifier que les pods sont `Ready` : `kubectl get pods -n lab-k8s`
2. Vérifier que le Service existe : `kubectl get svc -n lab-k8s`
3. Vérifier l'IP de Minikube : `minikube ip`
4. Tester depuis l'intérieur du cluster (étape 8.3)

### Erreur "ImagePullBackOff"

L'image n'est pas trouvée. Vérifier que vous avez construit l'image dans l'environnement Docker de Minikube (étape 3.2).

---



