package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
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

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> obtenerCuenta(@PathVariable String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.obtenerCuentaPorNumero(numeroCuenta), HttpStatus.OK);
    }

    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaCuentasResponse> obtenerActivas() {
        return new ResponseEntity<>(cuentaService.obtenerCuentasActivas(), HttpStatus.OK);
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoResponse> consultarSaldo(@PathVariable String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.consultarSaldo(numeroCuenta), HttpStatus.OK);
    }
}
