package com.proyecto.servicios.exception.gestopago;

public class GestoPagoTimeoutException extends GestoPagoIntegrationException {
    public GestoPagoTimeoutException(String message) {
        super(message);
    }

    public GestoPagoTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}