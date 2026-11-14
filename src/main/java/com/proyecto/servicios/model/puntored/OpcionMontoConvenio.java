package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Opcion de factura o deposito retornada por /payment-collection/v2/check-agreement.
 */
@Data
public class OpcionMontoConvenio {
    private String codigoRegistro;
    private String descripcionRegistro;
    private BigDecimal monto;
    private Integer tipoMonto;
    private BigDecimal montoMinimo;
    private BigDecimal montoMaximo;
    private Boolean requiereMonto;
}
