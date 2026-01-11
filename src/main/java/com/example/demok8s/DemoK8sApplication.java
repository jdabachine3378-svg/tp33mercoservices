package com.example.demok8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de l'application Spring Boot
 * 
 * Cette application expose une API REST simple qui sera déployée sur Kubernetes.
 * L'annotation @SpringBootApplication active la configuration automatique de Spring Boot.
 */
@SpringBootApplication
public class DemoK8sApplication {
    
    /**
     * Point d'entrée de l'application
     * 
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoK8sApplication.class, args);
    }
}

