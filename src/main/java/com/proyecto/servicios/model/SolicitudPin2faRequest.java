package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para solicitar el envio de un PIN de autenticacion 2FA al correo del cliente.
 */
@Getter
@Setter
@NoArgsConstructor
public class SolicitudPin2faRequest {

    @NotBlank(message = "Código 1: El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{18}$", message = "Código 4: El número de cuenta debe contener exactamente 18 dígitos numéricos")
    private String numeroCuenta;
}
