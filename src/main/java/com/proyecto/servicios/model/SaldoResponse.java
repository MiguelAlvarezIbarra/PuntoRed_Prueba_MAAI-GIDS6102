package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SaldoResponse extends GenericResponse {
    private String numeroCuenta;
    private BigDecimal saldo;
}
