package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;
import com.proyecto.servicios.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el envío de PIN 2FA por correo y ejecución de pagos/recargas
 * con GestoPago (SenTx / sendTx.do).
 * Cumple con las reglas de arquitectura:
 * - Sin @PathVariable ni @RequestParam.
 * - Validaciones de entrada con @Valid y @RequestBody.
 * - Manejo centralizado de errores en @RestControllerAdvice.
 */
@RestController
@RequestMapping("/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @PostMapping(value = "/enviar-pin", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Pin2faResponse> enviarPin2fa(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SolicitudPin2faRequest request) {
        return new ResponseEntity<>(pagoService.enviarPin2fa(request, authorization), HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoResponse> realizarPago(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PagoRequest request) {
        return new ResponseEntity<>(pagoService.realizarPago(request, authorization), HttpStatus.OK);
    }
}
