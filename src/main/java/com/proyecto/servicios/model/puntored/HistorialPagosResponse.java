package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.util.List;

/**
 * Listado de transacciones de pago pertenecientes al usuario autenticado (o todas para admin).
 */
@Data
public class HistorialPagosResponse {
    private Integer codigo;
    private String mensaje;
    private Integer total;
    private List<HistorialPagoItem> transacciones;
}
