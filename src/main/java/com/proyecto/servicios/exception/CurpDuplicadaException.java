package com.proyecto.servicios.exception;

public class CurpDuplicadaException extends RuntimeException {
    public CurpDuplicadaException(String mensaje) {
        super(mensaje);
    }

    public CurpDuplicadaException(MensajeError error) {
        super(error.getMensaje());
    }
}
