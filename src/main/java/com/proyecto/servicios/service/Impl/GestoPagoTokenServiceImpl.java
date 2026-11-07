package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
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
        log.info("Inicio renovarToken GestoPago - distribuidor={}, dispositivo={}", idDistribuidor, codigoDispositivo);

        if (idDistribuidor == null || codigoDispositivo == null || codigoDispositivo.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Credenciales de GestoPago incompletas en application.properties/yml " +
                            "(gestopago.auth.id-distribuidor / codigo-dispositivo / password). Revísalas.");
        }

        String rawResponse = invocarAutenticacion();
        JSONObject json = parsearRespuestaJson(rawResponse);

        log.debug("Claves recibidas en la respuesta de authenticate: {}", json.keySet());

        String token = json.optString("token", null);
        boolean pareceValido = token != null && !token.isBlank() && !"INVALID".equalsIgnoreCase(token);
        boolean success = json.optBoolean("success", pareceValido);

        if (!success || !pareceValido) {
            String motivo = json.optString("message", "sin motivo informado");
            throw new ValidacionException(MensajeError.GESTOPAGO_AUTENTICACION,
                    "GestoPago rechazo la autenticacion. Motivo: " + motivo);
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseGet(GestoPagoToken::new);

        tokenEntity.setIdDistribuidor(idDistribuidor);
        tokenEntity.setCodigoDispositivo(codigoDispositivo);
        tokenEntity.setToken(token);
        tokenEntity.setActivo(true);

        tokenRepository.save(tokenEntity);
        log.info("Fin renovarToken GestoPago - token actualizado correctamente (id={})", tokenEntity.getId());
    }

    private String invocarAutenticacion() {
        try {
            return gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);
        } catch (RetryableException e) {
            log.error("Fallo de red al llamar authenticate de GestoPago: {}", e.getClass().getSimpleName());
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new ValidacionException(MensajeError.GESTOPAGO_TIMEOUT,
                        "Timeout al llamar authenticate de GestoPago", e);
            }
            throw new ValidacionException(MensajeError.GESTOPAGO_COMUNICACION,
                    "Error de comunicacion al llamar authenticate de GestoPago", e);
        } catch (FeignException e) {
            log.error("GestoPago respondio HTTP {} en authenticate", e.status());
            throw new ValidacionException(MensajeError.GESTOPAGO_AUTENTICACION,
                    "GestoPago rechazo authenticate con HTTP " + e.status(), e);
        }
    }

    private JSONObject parsearRespuestaJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "GestoPago regreso una respuesta vacia al autenticar.");
        }
        try {
            return new JSONObject(rawResponse);
        } catch (Exception e) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "La respuesta de authenticate no es JSON valido.", e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidorParam, String codigoDispositivoParam) {
        return tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidorParam, codigoDispositivoParam)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()));
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActual() {
        return obtenerTokenActivo(idDistribuidor, codigoDispositivo);
    }
}
