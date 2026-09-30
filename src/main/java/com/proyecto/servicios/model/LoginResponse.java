package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse extends GenericResponse {
    private Integer clienteId;
    private Integer rol;
    private String jwt;
    // segundos de vida del token / de la sesion antes de expirar por inactividad
    private Long expiraEnSegundos;
}
