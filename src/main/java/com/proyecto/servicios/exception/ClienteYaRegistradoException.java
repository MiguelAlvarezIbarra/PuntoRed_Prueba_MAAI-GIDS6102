package com.proyecto.servicios.exception;

public class ClienteYaRegistradoException extends RuntimeException {
    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje);
    }

    public ClienteYaRegistradoException(MensajeError error) {
        super(error.getMensaje());
    }
}
