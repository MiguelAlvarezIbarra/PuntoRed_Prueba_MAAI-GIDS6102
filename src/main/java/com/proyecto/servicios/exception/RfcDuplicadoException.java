package com.proyecto.servicios.exception;

public class RfcDuplicadoException extends RuntimeException {
    public RfcDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public RfcDuplicadoException(MensajeError error) {
        super(error.getMensaje());
    }
}
