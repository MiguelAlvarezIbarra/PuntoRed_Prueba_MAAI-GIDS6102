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

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El numero de cuenta debe contener exactamente 10 digitos")
    private String numeroCuenta;
}
