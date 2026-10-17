package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.AdminGuard;
import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.model.*;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para administracion de clientes.
 * Cumple con todas las restricciones de diseno:
 * - No utiliza @PathVariable ni @RequestParam.
 * - Recibe criterios de busqueda e informacion mediante objetos Request DTO con @RequestBody y @Valid.
 * - Utiliza un unico endpoint de consulta dinamica con filtros combinables.
 * - Valida autorizacion por recurso.
 * - No incluye bloques try-catch ni validaciones repetitivas (centralizado en @RestControllerAdvice).
 * - Respuestas uniformes mediante Response DTOs.
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private SeguridadGuard seguridadGuard;

    @Autowired
    private AdminGuard adminGuard;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.creaCliente(request), HttpStatus.CREATED);
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ClienteActualizaRequest request) {
        seguridadGuard.validarAccesoClientePorRfc(authorization, request.getRfc());
        return new ResponseEntity<>(clienteService.actualizaCliente(request), HttpStatus.OK);
    }

    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminarCliente(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ClienteBajaRequest request) {
        seguridadGuard.validarAccesoClientePorRfc(authorization, request.getRfc());
        return new ResponseEntity<>(clienteService.eliminaCliente(request), HttpStatus.OK);
    }

    @PatchMapping(value = "/reactivar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> reactivarCliente(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ClienteReactivarRequest request) {
        adminGuard.exigirAdmin(authorization);
        return new ResponseEntity<>(clienteService.reactivarCliente(request), HttpStatus.OK);
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaClientesResponse> buscarDinamico(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) ClienteSearchRequest request) {
        return new ResponseEntity<>(clienteService.buscarDinamico(request, authorization), HttpStatus.OK);
    }
}
