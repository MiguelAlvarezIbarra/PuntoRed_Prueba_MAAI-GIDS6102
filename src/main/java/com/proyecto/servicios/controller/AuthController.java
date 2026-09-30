package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API temporal que entrega el JWT (segun lo pedido en el proyecto).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String PREFIJO_BEARER = "Bearer ";

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(usuarioService.login(request), HttpStatus.OK);
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith(PREFIJO_BEARER)
                ? authorization.substring(PREFIJO_BEARER.length())
                : authorization;
        return new ResponseEntity<>(usuarioService.logout(token), HttpStatus.OK);
    }
}
