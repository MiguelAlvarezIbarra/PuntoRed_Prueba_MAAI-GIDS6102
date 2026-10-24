package com.proyecto.servicios.exception;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(MensajeError error) {
        super(error.getMensaje());
    }
}
