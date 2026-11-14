package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.proyecto.servicios.client.PuntoredApiClient;
import com.proyecto.servicios.client.PuntoredHttpResult;
import com.proyecto.servicios.config.PuntoredProperties;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.service.PuntoredTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementacion thread-safe para obtener y renovar automaticamente el token Bearer de Puntored /auth.
 */
@Service
@Slf4j
public class PuntoredTokenServiceImpl implements PuntoredTokenService {

    private static final String PATH_AUTH = "/auth";
    private static final long MARGEN_SEGURIDAD_SEGUNDOS = 60L;
    private static final long TTL_DEFECTO_SEGUNDOS = 3600L;

    private final PuntoredApiClient apiClient;
    private final PuntoredProperties properties;

    private final Object monitor = new Object();
    private String tokenActual;
    private Instant expiracionToken;

    public PuntoredTokenServiceImpl(PuntoredApiClient apiClient, PuntoredProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    @Override
    public String obtenerTokenValido() {
        synchronized (monitor) {
            if (tokenActual != null && expiracionToken != null
                    && Instant.now().plusSeconds(MARGEN_SEGURIDAD_SEGUNDOS).isBefore(expiracionToken)) {
                return tokenActual;
            }
            return renovarTokenInterno();
        }
    }

    @Override
    public String renovarToken() {
        synchronized (monitor) {
            return renovarTokenInterno();
        }
    }

    @Override
    public void invalidarToken() {
        synchronized (monitor) {
            this.tokenActual = null;
            this.expiracionToken = null;
        }
    }

    @Scheduled(fixedRateString = "${puntored.api.token-refresh-ms:3600000}")
    public void renovarPeriodicamente() {
        if (!properties.tieneCredencialesConfiguradas()) {
            log.debug("Credenciales Puntored no configuradas; se omite renovacion automatica");
            return;
        }
        try {
            renovarToken();
        } catch (Exception ex) {
            log.warn("No se pudo renovar automaticamente el token de PuntoRed: {}", ex.getMessage());
        }
    }

    private String renovarTokenInterno() {
        if (!properties.tieneCredencialesConfiguradas()) {
            throw new ValidacionException(MensajeError.PUNTORED_CONFIG_FALTANTE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", properties.getUsername());
        body.put("password", properties.getPassword());
        body.put("commerce", properties.getCommerce());

        PuntoredHttpResult result = apiClient.postJson(PATH_AUTH, body, null);
        if (result.timedOut()) {
            throw new ValidacionException(MensajeError.PUNTORED_TIMEOUT);
        }
        if (!result.esTransaccionExitosa() || result.body() == null) {
            log.error("Fallo autenticacion contra PuntoRed: status={}, msg={}",
                    result.httpStatus(), result.obtenerMensaje());
            throw new ValidacionException(MensajeError.PUNTORED_AUTENTICACION);
        }

        JsonNode data = result.body().path("data");
        String nuevoToken = data.path("token").asText("");
        if (nuevoToken.isBlank()) {
            throw new ValidacionException(MensajeError.PUNTORED_AUTENTICACION);
        }

        this.tokenActual = nuevoToken;
        this.expiracionToken = parsearExpiracion(data.path("expiration").asText(null));
        log.info("Token PuntoRed renovado correctamente (expira={})", this.expiracionToken);
        return this.tokenActual;
    }

    private Instant parsearExpiracion(String expirationRaw) {
        if (expirationRaw != null && !expirationRaw.isBlank()) {
            try {
                return OffsetDateTime.parse(expirationRaw).toInstant();
            } catch (Exception ignored) {
                try {
                    return LocalDateTime.parse(expirationRaw)
                            .atZone(ZoneId.systemDefault())
                            .toInstant();
                } catch (Exception alsoIgnored) {
                    log.debug("Formato de expiracion no estandar en /auth: {}", expirationRaw);
                }
            }
        }
        return Instant.now().plusSeconds(TTL_DEFECTO_SEGUNDOS);
    }
}
