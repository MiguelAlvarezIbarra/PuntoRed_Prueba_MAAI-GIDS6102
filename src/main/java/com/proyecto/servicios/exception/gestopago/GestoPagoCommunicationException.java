package com.proyecto.servicios.exception.gestopago;

public class GestoPagoCommunicationException extends GestoPagoIntegrationException {
    public GestoPagoCommunicationException(String message) {
        super(message);
    }

    public GestoPagoCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}