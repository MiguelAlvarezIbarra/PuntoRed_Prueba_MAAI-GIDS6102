package com.proyecto.servicios.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }

    public CredencialesInvalidasException(MensajeError error) {
        super(error.getMensaje());
    }
}
