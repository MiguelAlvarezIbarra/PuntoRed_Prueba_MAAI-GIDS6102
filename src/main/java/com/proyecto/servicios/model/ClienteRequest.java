package com.proyecto.servicios.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    // Se ignora en creacion (lo asigna la BD) y no se permite modificar en update.
    private Integer id;

    // ---- Datos personales ----
    @NotBlank(message = "Código 1: El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: El nombre solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String nombre;

    @Pattern(regexp = "^[\\p{L} ]{0,50}$", message = "Código 4: El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "Código 1: El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: El apellido paterno solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String apellidoPaterno;

    @NotBlank(message = "Código 1: El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{3,50}$", message = "Código 4: El apellido materno solo debe contener letras y espacios (minimo 3, maximo 50 caracteres)")
    private String apellidoMaterno;

    @NotNull(message = "Código 1: La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "Código 5: La fecha de nacimiento no puede ser una fecha futura")
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(
            regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$",
            message = "La CURP debe tener el formato oficial de 18 caracteres"
    )
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(
            regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{2,3}$",
            message = "El RFC debe tener formato valido de 12 o 13 caracteres"
    )
    private String rfc;

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

    // ---- Datos de contacto ----
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    @Size(max = 100, message = "El correo no debe exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    private String telefonoMovil;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono alternativo debe contener exactamente 10 digitos")
    private String telefonoAlternativo;

    // ---- Informacion laboral ----
    @NotBlank(message = "La ocupacion es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "Código 1: El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "Código 2: El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "Código 6: Formato de dinero inválido (máximo 2 decimales)")
    private java.math.BigDecimal ingresoMensual;

    // ---- Domicilio ----
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // Password de acceso: solo se exige al CREAR el cliente (se valida a mano en el
    // servicio, no aqui, porque este mismo DTO se reutiliza en el PUT de actualizacion
    // y ahi el password no viaja).
    @Size(min = 8, message = "El password debe tener al menos 8 caracteres")
    private String password;
}
