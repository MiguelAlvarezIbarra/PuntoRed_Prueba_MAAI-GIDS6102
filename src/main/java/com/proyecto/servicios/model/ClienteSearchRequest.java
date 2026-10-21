package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Filtros de la busqueda dinamica de clientes para el endpoint unico de consulta.
 * Todos los campos son opcionales y se combinan con AND mediante Criteria API (Specification).
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaDesde;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaHasta;

    private Boolean activo;
}
