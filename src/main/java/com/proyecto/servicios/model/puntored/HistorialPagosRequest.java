package com.proyecto.servicios.model.puntored;

import lombok.Data;

/**
 * Filtros opcionales para consultar el historial de pagos del usuario autenticado.
 */
@Data
public class HistorialPagosRequest {
    private String numeroCuenta;
    private String estado;
    private String tipoOperacion;
    private Integer limite;
}
