package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonasRequest {

    private Integer codigo;
    private String nombre;
    private String apellidoP;
    private String apellidoMaterno;

}