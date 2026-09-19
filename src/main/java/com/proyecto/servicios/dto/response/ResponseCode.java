package com.proyecto.servicios.dto.response;

import lombok.Getter;

@Getter
public enum ResponseCode {

    OK(0, "Datos consultados correctamente"),
    ERROR_GENERICO(1, "Datos consultados de forma errónea"),
    ERROR_AUTENTICACION(2, "No se pudo autenticar con GestoPago"),
    ERROR_TIMEOUT(3, "Tiempo de espera agotado al comunicarse con GestoPago"),
    ERROR_COMUNICACION(4, "Error de comunicación con GestoPago"),
    ERROR_RESPUESTA_GESTOPAGO(5, "GestoPago respondió con un error"),
    ERROR_SIN_DATOS(6, "No se encontraron productos");

    private final int codigo;
    private final String mensaje;

    ResponseCode(int codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}