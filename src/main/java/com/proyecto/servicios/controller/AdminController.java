package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.AdminGuard;
import com.proyecto.servicios.model.AdminClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints exclusivos de administrador. Requieren header
 * Authorization: Bearer <jwt de un usuario con rol=1>.
 * El primer administrador se debe crear a mano (ver docs/DOCUMENTO_TECNICO.md).
 */
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private ClienteService clienteService;

    @PostMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crearComoAdmin(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AdminClienteRequest request) {

        Integer adminId = adminGuard.exigirAdmin(authorization);
        log.info("Admin clienteId={} crea un nuevo usuario con rol={}", adminId, request.getRol());

        return new ResponseEntity<>(clienteService.creaClienteConRol(request, request.getRol()), HttpStatus.CREATED);
    }
}
