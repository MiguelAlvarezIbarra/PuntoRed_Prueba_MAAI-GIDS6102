package com.proyecto.servicios.exception.gestopago;

public class GestoPagoResponseException extends GestoPagoIntegrationException {

    private final Integer statusCode;

    public GestoPagoResponseException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GestoPagoResponseException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}