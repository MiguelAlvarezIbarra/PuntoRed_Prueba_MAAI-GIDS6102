package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Error de regla de negocio. Conserva el codigo del catalogo MensajeError para que
 * el GlobalExceptionHandler responda con el codigo correcto y no siempre con 1.
 */
@Getter
public class ValidacionException extends RuntimeException {

    private final Integer codigo;

    public ValidacionException(String mensaje) {
        super(mensaje);
        this.codigo = MensajeError.VALIDACION_GENERAL.getCodigo();
    }

    public ValidacionException(MensajeError error) {
        super(error.getMensaje());
        this.codigo = error.getCodigo();
    }
}
