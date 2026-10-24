package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta al generar y enviar el PIN de autenticacion 2FA por correo.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pin2faResponse extends GenericResponse {

    private String numeroCuenta;
    private String correoDestino;
    private Long expiraEnSegundos;
    private String pin2fa;
}
