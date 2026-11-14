package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elemento del historial de pagos de servicios con estado, logo y folio.
 */
@Data
public class HistorialPagoItem {
    private Integer id;
    private String numeroCuenta;
    private String proveedor;
    private String tipoOperacion;
    private String nombreServicio;
    private String nombreProducto;
    private String categoria;
    private String logoUrl;
    private String referencia1;
    private String referencia2;
    private BigDecimal monto;
    private String idTransaccionProveedor;
    private String codigoAutorizacion;
    private String trace;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private String estado;
    private TicketData ticket;
    private LocalDateTime fechaRegistro;
}
