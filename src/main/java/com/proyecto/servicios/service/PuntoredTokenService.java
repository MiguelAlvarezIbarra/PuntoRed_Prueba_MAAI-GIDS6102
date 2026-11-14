package com.proyecto.servicios.service;

/**
 * Administra el ciclo de vida del token Bearer para autenticar peticiones contra Puntored H2H V2.
 */
public interface PuntoredTokenService {

    String obtenerTokenValido();

    String renovarToken();

    void invalidarToken();
}
