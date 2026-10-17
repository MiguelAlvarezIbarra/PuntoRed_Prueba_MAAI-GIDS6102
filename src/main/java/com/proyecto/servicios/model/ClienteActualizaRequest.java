package com.proyecto.servicios.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para la actualizacion de clientes.
 * El RFC identifica al cliente a actualizar.
 * Campos controlados por el servidor como id, curp, cuenta, fechaRegistro y activo
 * no se pueden alterar a traves de este DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteActualizaRequest {

    @NotBlank(message = "El RFC del cliente a actualizar es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{2,3}$",
            message = "El RFC debe tener formato valido de 12 o 13 caracteres"
    )
    private String rfc;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "El nombre solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String nombre;

    @Pattern(regexp = "^[\\p{L} ]{0,50}$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "El apellido paterno solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "El apellido materno solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String apellidoMaterno;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[HM]$", message = "El sexo debe ser 'H' o 'M'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(
            regexp = "^(Soltero|Casado|Divorciado|Viudo|Union Libre)$",
            message = "El estado civil debe ser uno de: Soltero, Casado, Divorciado, Viudo, Union Libre"
    )
    private String estadoCivil;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    @Size(max = 100, message = "El correo no debe exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupacion es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "Formato de dinero inválido (máximo 2 decimales)")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;
}
