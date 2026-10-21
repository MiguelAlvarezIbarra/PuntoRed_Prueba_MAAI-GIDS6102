package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la solicitud de reactivacion de un cliente inactivo.
 * Recibe el RFC en el cuerpo de la peticion sin usar @PathVariable ni @RequestParam.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteReactivarRequest {

    @NotBlank(message = "El RFC del cliente a reactivar es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{2,3}$",
            message = "El RFC debe tener formato valido de 12 o 13 caracteres"
    )
    private String rfc;
}
