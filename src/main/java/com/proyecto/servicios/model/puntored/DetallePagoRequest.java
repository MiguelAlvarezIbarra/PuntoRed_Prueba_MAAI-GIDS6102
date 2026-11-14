package com.proyecto.servicios.model.puntored;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Solicitud para consultar el detalle de un pago o reverificar su estado ante Puntored.
 */
@Data
public class DetallePagoRequest {

    @NotNull(message = "Código 1: El identificador de la transacción es obligatorio")
    private Integer idTransaccion;

    private Boolean consultarEstadoProveedor;
}
