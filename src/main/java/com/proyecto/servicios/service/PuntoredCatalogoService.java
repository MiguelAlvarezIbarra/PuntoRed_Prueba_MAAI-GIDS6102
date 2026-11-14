package com.proyecto.servicios.service;

import com.proyecto.servicios.model.puntored.CatalogoBusquedaRequest;
import com.proyecto.servicios.model.puntored.CatalogoServiciosResponse;

/**
 * Servicio de consulta dinamica del catalogo de productos y servicios en Puntored H2H V2
 * (Recargas, Paquetes, Recaudos/Convenios y Pines digitales con logotipos y descripciones reales).
 */
public interface PuntoredCatalogoService {

    CatalogoServiciosResponse consultarCatalogo(CatalogoBusquedaRequest request);
}
