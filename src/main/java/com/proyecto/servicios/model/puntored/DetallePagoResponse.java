package com.proyecto.servicios.model.puntored;

import lombok.Data;

/**
 * Respuesta detallada de una transaccion del historial, incluyendo posible actualizacion de estado.
 */
@Data
public class DetallePagoResponse {
    private Integer codigo;
    private String mensaje;
    private HistorialPagoItem transaccion;
    private Boolean estadoActualizado;
}
