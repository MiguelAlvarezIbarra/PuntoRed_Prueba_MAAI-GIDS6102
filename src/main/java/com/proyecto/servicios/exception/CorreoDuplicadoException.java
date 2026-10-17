package com.proyecto.servicios.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public CorreoDuplicadoException(MensajeError error) {
        super(error.getMensaje());
    }
}
