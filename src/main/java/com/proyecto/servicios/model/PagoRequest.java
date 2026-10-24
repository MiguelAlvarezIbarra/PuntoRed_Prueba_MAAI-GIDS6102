package com.proyecto.servicios.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de entrada para realizar un pago o recarga mediante GestoPago (sendTx.do).
 * Incluye validaciones estrictas de formato para todos los campos:
 * - numeroCuenta de exactamente 18 digitos.
 * - idServicio e idProducto positivos.
 * - monto con maximo 2 decimales y mayor a cero.
 * - bandera tipoPago: TELEFONO (Recarga) o REFERENCIA (Pago Bancario).
 * - pin2fa de 6 digitos enviado al correo del cliente.
 */
@Getter
@Setter
@NoArgsConstructor
public class PagoRequest {

    @NotBlank(message = "Código 1: El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{18}$", message = "Código 4: El número de cuenta debe contener exactamente 18 dígitos numéricos")
    private String numeroCuenta;

    @NotNull(message = "Código 1: El idServicio es obligatorio")
    @Positive(message = "Código 2: El idServicio debe ser mayor a cero")
    private Integer idServicio;

    @NotNull(message = "Código 1: El idProducto es obligatorio")
    @Positive(message = "Código 2: El idProducto debe ser mayor a cero")
    private Integer idProducto;

    @NotNull(message = "Código 1: El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "Código 2: El monto debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "Código 6: Formato de dinero inválido (máximo 2 decimales)")
    private BigDecimal monto;

    @NotBlank(message = "Código 1: El tipo de pago (TELEFONO o REFERENCIA) es obligatorio")
    @Pattern(
            regexp = "^(TELEFONO|REFERENCIA)$",
            message = "Código 4: El tipo de pago solo permite TELEFONO (Recarga) o REFERENCIA (Pago Bancario)"
    )
    private String tipoPago;

    @Pattern(regexp = "^\\d{10}$", message = "Código 4: El teléfono debe contener exactamente 10 dígitos numéricos")
    private String telefono;

    @Pattern(
            regexp = "^[A-Za-z0-9]{4,30}$",
            message = "Código 4: La referencia solo permite letras y números sin caracteres especiales (4 a 30 caracteres)"
    )
    private String referencia;

    @NotBlank(message = "Código 1: El PIN de autenticación 2FA es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "Código 4: El PIN de autenticación 2FA debe contener exactamente 6 dígitos numéricos")
    private String pin2fa;

    @Pattern(
            regexp = "^[A-Za-z0-9_]{1,20}$",
            message = "Código 4: El UPC solo permite letras y números sin caracteres especiales"
    )
    private String upc;

    @Pattern(
            regexp = "^\\d{1,10}$",
            message = "Código 4: La unidad solo permite dígitos numéricos"
    )
    private String unidad;
}
