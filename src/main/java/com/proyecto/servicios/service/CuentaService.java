package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.model.SaldoResponse;

public interface CuentaService {

    CuentaResponse obtenerCuentaPorNumero(String numeroCuenta);

    ListaCuentasResponse obtenerCuentasActivas();

    SaldoResponse consultarSaldo(String numeroCuenta);
}
