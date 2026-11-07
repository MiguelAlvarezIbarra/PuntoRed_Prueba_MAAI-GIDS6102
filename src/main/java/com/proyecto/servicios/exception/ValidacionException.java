package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepcion unica de negocio e integracion del sistema.
 * Conserva el codigo y el HttpStatus del catalogo MensajeError para que
 * el GlobalExceptionHandler centralice todas las respuestas en un solo manejador.
 */
@Getter
public class ValidacionException extends RuntimeException {

    private final Integer codigo;
    private final HttpStatus status;

    public ValidacionException(String mensaje) {
        super(mensaje);
        this.codigo = MensajeError.VALIDACION_GENERAL.getCodigo();
        this.status = MensajeError.VALIDACION_GENERAL.getStatus();
    }

    public ValidacionException(MensajeError error) {
        super(error.getMensaje());
        this.codigo = error.getCodigo();
        this.status = error.getStatus();
    }

    public ValidacionException(MensajeError error, String mensajeDetalle) {
        super(mensajeDetalle);
        this.codigo = error.getCodigo();
        this.status = error.getStatus();
    }

    public ValidacionException(MensajeError error, String mensajeDetalle, Throwable cause) {
        super(mensajeDetalle, cause);
        this.codigo = error.getCodigo();
        this.status = error.getStatus();
    }

    public ValidacionException(Integer codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = HttpStatus.BAD_REQUEST;
    }
}
