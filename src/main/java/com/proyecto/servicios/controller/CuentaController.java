package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CuentaConsultaRequest;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    @Autowired
    private CuentaService cuentaService;

    @PostMapping(value = "/consulta", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> obtenerCuenta(@Valid @RequestBody CuentaConsultaRequest request) {
        return new ResponseEntity<>(cuentaService.obtenerCuentaPorNumero(request.getNumeroCuenta()), HttpStatus.OK);
    }

    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaCuentasResponse> obtenerActivas() {
        return new ResponseEntity<>(cuentaService.obtenerCuentasActivas(), HttpStatus.OK);
    }

    @PostMapping(value = "/saldo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoResponse> consultarSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        return new ResponseEntity<>(cuentaService.consultarSaldo(request.getNumeroCuenta()), HttpStatus.OK);
    }
}
