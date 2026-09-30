package com.proyecto.servicios.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Igual que ClienteRequest, pero el ADMIN si puede elegir el rol del nuevo usuario
 * (1 = administrador, 2 = usuario normal). En el registro publico (POST /clientes)
 * este campo no existe y siempre se fuerza a 2.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminClienteRequest extends ClienteRequest {

    @NotNull(message = "El rol es obligatorio (1 = administrador, 2 = usuario normal)")
    @Min(value = 1, message = "El rol debe ser 1 (administrador) o 2 (usuario normal)")
    @Max(value = 2, message = "El rol debe ser 1 (administrador) o 2 (usuario normal)")
    private Integer rol;
}
