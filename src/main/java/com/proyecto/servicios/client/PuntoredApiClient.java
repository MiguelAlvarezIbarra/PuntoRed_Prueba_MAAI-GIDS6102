package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.PuntoredProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Locale;

/**
 * Cliente HTTP especializado para consumir las APIs REST de Puntored H2H V2.
 * Preserva el JSON devuelto incluso ante respuestas 4xx/5xx y distingue timeouts reales.
 */
@Component
@Slf4j
public class PuntoredApiClient {

    private final PuntoredProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public PuntoredApiClient(PuntoredProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, crearRestTemplate(properties));
    }

    public PuntoredApiClient(PuntoredProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate crearRestTemplate(PuntoredProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1000, props.getConnectTimeoutMs()));
        factory.setReadTimeout(Math.max(2000, props.getReadTimeoutMs()));
        return new RestTemplate(factory);
    }

    public PuntoredHttpResult postJson(String path, Object payload, String bearerToken) {
        String baseUrl = properties.getUrl() != null ? properties.getUrl().replaceAll("/+$", "") : "";
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }

        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String raw = response.getBody();
            return new PuntoredHttpResult(
                    response.getStatusCode().value(),
                    false,
                    false,
                    parsearJsonSilencioso(raw),
                    raw
            );
        } catch (HttpStatusCodeException ex) {
            String raw = ex.getResponseBodyAsString();
            int status = ex.getStatusCode().value();
            boolean timedOut = status == 504;
            log.warn("Respuesta HTTP {} desde PuntoRed en {}: {}", status, path, extraerResumen(raw));
            return new PuntoredHttpResult(status, timedOut, false, parsearJsonSilencioso(raw), raw);
        } catch (ResourceAccessException ex) {
            boolean timedOut = esTimeout(ex);
            log.warn("Error de acceso en PuntoRed ({}) timeout={}: {}", path, timedOut, ex.getMessage());
            return new PuntoredHttpResult(timedOut ? 504 : 503, timedOut, !timedOut, null, null);
        }
    }

    private JsonNode parsearJsonSilencioso(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean esTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase(Locale.ROOT).contains("timed out");
    }

    private String extraerResumen(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.length() > 220 ? raw.substring(0, 220) + "..." : raw;
    }
}
