package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la consulta de cuentas o saldos sin utilizar @PathVariable ni @RequestParam.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaConsultaRequest {

    @NotBlank(message = "Código 1: El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{18}$", message = "Código 4: El número de cuenta debe contener exactamente 18 dígitos numéricos")
    private String numeroCuenta;
}
