package com.proyecto.servicios.exception;

public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(MensajeError error) {
        super(error.getMensaje());
    }
}
