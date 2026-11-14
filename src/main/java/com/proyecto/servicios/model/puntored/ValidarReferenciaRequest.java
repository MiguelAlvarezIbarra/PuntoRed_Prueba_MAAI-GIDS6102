package com.proyecto.servicios.model.puntored;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Solicitud para validar una referencia/linea antes de habilitar la confirmacion de pago.
 */
@Data
public class ValidarReferenciaRequest {

    @NotBlank(message = "Código 1: El tipo de operación es obligatorio")
    private String tipoOperacion;

    private String idConvenio;

    private String codigoProducto;

    private String codigoProveedor;

    @NotBlank(message = "Código 1: La referencia o línea a validar es obligatoria")
    @Size(max = 80, message = "Código 1: La referencia no debe exceder 80 caracteres")
    private String referencia1;

    @Size(max = 80, message = "Código 1: La referencia secundaria no debe exceder 80 caracteres")
    private String referencia2;

    private BigDecimal montoReferencia;

    private String codigoIac;

    private String tipoCuenta;

    private Integer tipoReferencia;
}
