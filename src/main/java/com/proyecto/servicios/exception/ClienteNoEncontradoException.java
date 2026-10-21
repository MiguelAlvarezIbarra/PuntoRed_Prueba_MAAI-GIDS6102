package com.proyecto.servicios.exception;

public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public ClienteNoEncontradoException(MensajeError error) {
        super(error.getMensaje());
    }
}
