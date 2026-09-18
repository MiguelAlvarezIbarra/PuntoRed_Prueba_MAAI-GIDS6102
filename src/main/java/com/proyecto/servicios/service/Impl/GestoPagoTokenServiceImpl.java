package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}, dispositivo={}", idDistribuidor, codigoDispositivo);

        if (idDistribuidor == null || codigoDispositivo == null || codigoDispositivo.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Credenciales de GestoPago incompletas en application.properties/yml " +
                            "(gestopago.auth.id-distribuidor / codigo-dispositivo / password). Revísalas.");
        }

        String rawResponse;
        try {
            rawResponse = gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);
        } catch (Exception e) {
            log.error("Falló la llamada al endpoint authenticate de GestoPago: {}", e.getMessage(), e);
            throw new IllegalStateException("Falló la autenticación con GestoPago: " + e.getMessage(), e);
        }

        log.info("Respuesta cruda de authenticate de GestoPago:\n{}", rawResponse);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("GestoPago regresó una respuesta vacía al autenticar.");
        }

        JSONObject json;
        try {
            json = new JSONObject(rawResponse);
        } catch (Exception e) {
            throw new IllegalStateException("La respuesta de authenticate no es JSON válido: " + rawResponse, e);
        }

        log.info("Claves recibidas en la respuesta de authenticate: {}", json.keySet());

        String token = json.optString("token", null);
        boolean pareceValido = token != null && !token.isBlank() && !"INVALID".equalsIgnoreCase(token);
        boolean success = json.optBoolean("success", pareceValido);

        if (!success || !pareceValido) {
            String motivo = json.optString("message", json.toString());
            throw new IllegalStateException("GestoPago rechazó la autenticación. Motivo: " + motivo);
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseGet(GestoPagoToken::new);

        tokenEntity.setIdDistribuidor(idDistribuidor);
        tokenEntity.setCodigoDispositivo(codigoDispositivo);
        tokenEntity.setToken(token);
        tokenEntity.setTokenType(json.optString("token_type", null));
        if (json.has("expires_in")) {
            tokenEntity.setExpiresIn(json.optLong("expires_in"));
        }
        tokenEntity.setActivo(true);

        tokenRepository.save(tokenEntity);
        log.info("Token GestoPago renovado correctamente");
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .filter(t -> t.getToken() != null
                        && !t.getToken().isBlank()
                        && !"INVALID".equalsIgnoreCase(t.getToken()));
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActual() {
        return obtenerTokenActivo(idDistribuidor, codigoDispositivo);
    }
}