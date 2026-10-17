package com.proyecto.servicios.exception;

public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public AccesoDenegadoException(MensajeError error) {
        super(error.getMensaje());
    }
}
