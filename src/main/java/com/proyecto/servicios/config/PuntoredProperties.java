package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion externalizada para el consumo de Puntored Servicios H2H - V2.
 * Todas las credenciales se inyectan mediante variables de entorno o configuracion local no versionada.
 */
@Configuration
@ConfigurationProperties(prefix = "puntored.api")
@Getter
@Setter
public class PuntoredProperties {

    private String url = "https://services.preprodcxr.co/puntored/api/v1";
    private String commerce = "";
    private Integer pointOfSale;
    private String terminal = "";
    private String username = "";
    private String password = "";
    private String transactionalPassword = "";
    private long tokenRefreshMs = 3600000L;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 25000;

    public boolean tieneCredencialesConfiguradas() {
        return tieneTexto(url)
                && tieneTexto(commerce)
                && pointOfSale != null
                && tieneTexto(username)
                && tieneTexto(password);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
