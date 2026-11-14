package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.model.puntored.DetallePagoRequest;
import com.proyecto.servicios.model.puntored.DetallePagoResponse;
import com.proyecto.servicios.model.puntored.HistorialPagosRequest;
import com.proyecto.servicios.model.puntored.HistorialPagosResponse;
import com.proyecto.servicios.model.puntored.PagoPuntoredRequest;
import com.proyecto.servicios.model.puntored.PagoPuntoredResponse;
import com.proyecto.servicios.model.puntored.ValidacionReferenciaResponse;
import com.proyecto.servicios.model.puntored.ValidarReferenciaRequest;
import com.proyecto.servicios.service.PuntoredPagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el flujo completo de pagos PuntoRed H2H V2:
 * validacion previa de referencias, confirmacion explicita de pagos con 2FA,
 * consulta de historial autenticado y detalle/reverificacion ante el proveedor.
 */
@RestController
@RequestMapping("/pagos")
public class PuntoredPagoController {

    private final PuntoredPagoService puntoredPagoService;
    private final SeguridadGuard seguridadGuard;

    public PuntoredPagoController(PuntoredPagoService puntoredPagoService, SeguridadGuard seguridadGuard) {
        this.puntoredPagoService = puntoredPagoService;
        this.seguridadGuard = seguridadGuard;
    }

    @PostMapping(value = "/validar-referencia", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidacionReferenciaResponse> validarReferencia(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ValidarReferenciaRequest request) {
        InfoToken infoToken = seguridadGuard.exigirToken(authorization);
        return new ResponseEntity<>(puntoredPagoService.validarReferencia(request, infoToken), HttpStatus.OK);
    }

    @PostMapping(value = "/puntored", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagoPuntoredResponse> realizarPagoPuntored(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PagoPuntoredRequest request) {
        InfoToken infoToken = seguridadGuard.exigirToken(authorization);
        return new ResponseEntity<>(puntoredPagoService.realizarPago(request, infoToken), HttpStatus.OK);
    }

    @PostMapping(value = "/historial", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HistorialPagosResponse> consultarHistorial(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) HistorialPagosRequest request) {
        InfoToken infoToken = seguridadGuard.exigirToken(authorization);
        HistorialPagosRequest filtro = request != null ? request : new HistorialPagosRequest();
        return new ResponseEntity<>(puntoredPagoService.consultarHistorial(filtro, infoToken), HttpStatus.OK);
    }

    @PostMapping(value = "/detalle", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetallePagoResponse> consultarDetalle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody DetallePagoRequest request) {
        InfoToken infoToken = seguridadGuard.exigirToken(authorization);
        return new ResponseEntity<>(puntoredPagoService.consultarDetalleYReverificar(request, infoToken), HttpStatus.OK);
    }
}
