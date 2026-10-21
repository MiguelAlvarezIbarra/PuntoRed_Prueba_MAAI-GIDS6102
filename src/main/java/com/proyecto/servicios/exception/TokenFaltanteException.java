package com.proyecto.servicios.exception;

public class TokenFaltanteException extends RuntimeException {
    public TokenFaltanteException(String mensaje) {
        super(mensaje);
    }

    public TokenFaltanteException(MensajeError error) {
        super(error.getMensaje());
    }
}
