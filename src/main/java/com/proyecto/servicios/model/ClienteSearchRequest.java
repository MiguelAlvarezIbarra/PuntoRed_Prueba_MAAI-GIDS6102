package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtros de la busqueda dinamica de clientes. Todos son opcionales:
 * solo se filtra por los campos que vengan en el JSON (se combinan con AND).
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteSearchRequest {
    private String nombre;
    private String rfc;
    private String curp;
    @JsonAlias("cuenta")
    private String numeroCuenta;
    private String correo;
}
