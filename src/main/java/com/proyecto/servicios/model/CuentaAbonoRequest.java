package com.proyecto.servicios.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para abonar saldo a una cuenta existente (18 caracteres).
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaAbonoRequest {

    @NotBlank(message = "Código 1: El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{18}$", message = "Código 4: El número de cuenta debe contener exactamente 18 dígitos numéricos")
    private String numeroCuenta;

    @NotNull(message = "Código 1: El monto a abonar es obligatorio")
    @DecimalMin(value = "0.01", message = "Código 2: El monto a abonar debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "Código 6: Formato de dinero inválido (máximo 2 decimales)")
    private BigDecimal monto;
}
