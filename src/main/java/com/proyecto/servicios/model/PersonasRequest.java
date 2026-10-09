package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class PersonasRequest {

    private Integer codigo;

    @NotBlank(message = "Código 1: El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: Números o caracteres especiales no permitidos en nombre, y debe medir entre 3 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "Código 1: El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: Números o caracteres especiales no permitidos en apellido paterno, y debe medir entre 3 y 50 caracteres")
    private String apellidoP;

    @NotBlank(message = "Código 1: El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: Números o caracteres especiales no permitidos en apellido materno, y debe medir entre 3 y 50 caracteres")
    private String apellidoMaterno;

}