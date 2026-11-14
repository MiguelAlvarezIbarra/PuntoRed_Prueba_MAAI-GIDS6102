package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta detallada del intento de pago en Puntored, diferenciando estados:
 * EXITOSA, RECHAZADA, PENDIENTE y NO_CONFIRMADA.
 */
@Data
public class PagoPuntoredResponse {
    private Integer codigo;
    private String mensaje;
    private String estado;
    private Boolean transaccionConfirmada;
    private Integer idTransaccionLocal;
    private String idTransaccionProveedor;
    private String codigoAutorizacion;
    private String trace;
    private String pinContenido;
    private String codigoRespuestaProveedor;
    private String mensajeProveedor;
    private String numeroCuenta;
    private String nombreServicio;
    private String nombreProducto;
    private String categoria;
    private String logoUrl;
    private String referencia1;
    private String referencia2;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;
    private TicketData ticket;
    private LocalDateTime fechaRegistro;
}
