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
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El nombre solo debe contener letras y espacios (2 a 50 caracteres)")
    private String nombre;

    @Pattern(regexp = "^[\\p{L} ]{0,50}$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El apellido paterno solo debe contener letras y espacios (2 a 50 caracteres)")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L} ]{2,50}$", message = "El apellido materno solo debe contener letras y espacios (2 a 50 caracteres)")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser una fecha futura")
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
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
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

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

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
