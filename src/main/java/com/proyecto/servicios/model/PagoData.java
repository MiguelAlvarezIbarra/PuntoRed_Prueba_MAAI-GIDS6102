package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos detallados del comprobante de pago procesado con GestoPago.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoData {

    private Integer idTransaccionLocal;
    private String numeroCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String tipoPago;
    private String telefonoReferencia;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoActual;
    private String upc;
    private String unidad;
    private String horaLocal;
    private String codigoGestoPago;
    private String mensajeGestoPago;
    private String idTxGestoPago;
    private String numAutorizacion;
    private String fechaGestoPago;
    private String pin;
    private String leyenda;
}
