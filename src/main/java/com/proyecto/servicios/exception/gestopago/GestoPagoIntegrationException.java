package com.proyecto.servicios.exception.gestopago;

public class GestoPagoIntegrationException extends RuntimeException {
    public GestoPagoIntegrationException(String message) {
        super(message);
    }

    public GestoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}