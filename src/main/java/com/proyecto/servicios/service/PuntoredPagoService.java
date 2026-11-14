package com.proyecto.servicios.service;

import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.model.puntored.DetallePagoRequest;
import com.proyecto.servicios.model.puntored.DetallePagoResponse;
import com.proyecto.servicios.model.puntored.HistorialPagosRequest;
import com.proyecto.servicios.model.puntored.HistorialPagosResponse;
import com.proyecto.servicios.model.puntored.PagoPuntoredRequest;
import com.proyecto.servicios.model.puntored.PagoPuntoredResponse;
import com.proyecto.servicios.model.puntored.ValidacionReferenciaResponse;
import com.proyecto.servicios.model.puntored.ValidarReferenciaRequest;

/**
 * Servicio integral de validacion de referencias, ejecucion de pagos en Puntored H2H V2
 * y consulta de historial/detalle de transacciones por usuario autenticado.
 */
public interface PuntoredPagoService {

    ValidacionReferenciaResponse validarReferencia(ValidarReferenciaRequest request, InfoToken infoToken);

    PagoPuntoredResponse realizarPago(PagoPuntoredRequest request, InfoToken infoToken);

    HistorialPagosResponse consultarHistorial(HistorialPagosRequest request, InfoToken infoToken);

    DetallePagoResponse consultarDetalleYReverificar(DetallePagoRequest request, InfoToken infoToken);
}
