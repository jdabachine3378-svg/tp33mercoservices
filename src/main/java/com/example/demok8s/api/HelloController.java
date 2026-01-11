package com.example.demok8s.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Contrôleur REST pour l'endpoint /api/hello
 * 
 * Ce contrôleur expose un endpoint simple qui retourne un message JSON.
 * Le message peut être configuré via une variable d'environnement APP_MESSAGE
 * ou via une ConfigMap Kubernetes (voir étape 9).
 */
@RestController
public class HelloController {

    /**
     * Message de l'application injecté depuis la variable d'environnement APP_MESSAGE
     * Si la variable n'est pas définie, utilise la valeur par défaut
     */
    @Value("${APP_MESSAGE:Hello from Spring Boot on Kubernetes}")
    private String appMessage;

    /**
     * Endpoint GET /api/hello
     * 
     * Retourne un message JSON avec le statut de l'application.
     * Ce endpoint est utilisé pour :
     * - Tester l'API
     * - La readiness probe du Deployment Kubernetes
     * 
     * @return Map contenant le message et le statut
     */
    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of(
            "message", appMessage,
            "status", "OK"
        );
    }
}

