package com.proyecto.servicios.model.gestopago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa la peticion SenTx requerida por GestoPago (sendTx.do):
 * - idProducto (IdProd)
 * - idServicio (IdSer)
 * - telefono / referencia (Tel/Referencia)
 * - horaLocal (Hora Local en formato yyyyMMddHHmmss)
 * - montoPago (Monto)
 * - upc (UPC)
 * - unidad (Unidad)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SenTxRequest {

    @NotNull(message = "Código 1: El idProducto de SenTx es obligatorio")
    @Positive(message = "Código 2: El idProducto de SenTx debe ser mayor a cero")
    private Integer idProducto;

    @NotNull(message = "Código 1: El idServicio de SenTx es obligatorio")
    @Positive(message = "Código 2: El idServicio de SenTx debe ser mayor a cero")
    private Integer idServicio;

    @Pattern(regexp = "^(\\d{10})?$", message = "Código 4: El teléfono de SenTx debe contener 10 dígitos")
    private String telefono;

    @NotBlank(message = "Código 1: El teléfono o referencia de SenTx es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{4,30}$", message = "Código 4: La referencia de SenTx solo permite letras y números")
    private String referencia;

    @NotBlank(message = "Código 1: La horaLocal de SenTx es obligatoria")
    @Pattern(regexp = "^\\d{14}$", message = "Código 5: Formato de horaLocal inválido (debe ser yyyyMMddHHmmss)")
    private String horaLocal;

    @NotNull(message = "Código 1: El montoPago de SenTx es obligatorio")
    @DecimalMin(value = "0.01", message = "Código 2: El montoPago de SenTx debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "Código 6: Formato de dinero inválido en montoPago")
    private BigDecimal montoPago;

    @NotBlank(message = "Código 1: El UPC de SenTx es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9_]{1,20}$", message = "Código 4: El UPC de SenTx no permite caracteres especiales")
    private String upc;

    @NotBlank(message = "Código 1: La unidad de SenTx es obligatoria")
    @Pattern(regexp = "^\\d{1,10}$", message = "Código 4: La unidad de SenTx solo permite números")
    private String unidad;
}
