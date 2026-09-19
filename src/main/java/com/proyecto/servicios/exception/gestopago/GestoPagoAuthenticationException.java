package com.proyecto.servicios.exception.gestopago;

public class GestoPagoAuthenticationException extends GestoPagoIntegrationException {
    public GestoPagoAuthenticationException(String message) {
        super(message);
    }

    public GestoPagoAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}