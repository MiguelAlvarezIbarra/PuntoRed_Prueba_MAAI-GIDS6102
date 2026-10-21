package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Catálogo centralizado de mensajes y códigos de excepción y error del sistema.
 */
@Getter
public enum MensajeError {

    // Validaciones generales (Código 1)
    VALIDACION_GENERAL(1, "Error de validacion en los datos de la peticion"),
    NOMBRE_OBLIGATORIO(1, "Código 1: El nombre es obligatorio"),
    APELLIDO_PATERNO_OBLIGATORIO(1, "Código 1: El apellido paterno es obligatorio"),
    APELLIDO_MATERNO_OBLIGATORIO(1, "Código 1: El apellido materno es obligatorio"),
    FECHA_NACIMIENTO_OBLIGATORIA(1, "Código 1: La fecha de nacimiento es obligatoria"),
    MAYOR_EDAD_REQUERIDO(1, "El cliente debe ser mayor de edad (18 anios o mas)"),
    PASSWORD_OBLIGATORIO(1, "El password es obligatorio para registrar el cliente"),
    ROL_INVALIDO(1, "El rol debe ser 1 (administrador) o 2 (usuario normal)"),
    PETICION_MALFORMADA(1, "Error de validacion: la peticion esta mal formada o le faltan datos"),
    CLIENTE_YA_ACTIVO(1, "El cliente ya se encuentra activo"),

    // Duplicidad y conflictos de integridad (Códigos 2 a 5)
    CLIENTE_YA_REGISTRADO(2, "Ya existe un registro con esos datos (CURP, RFC, correo o cuenta)"),
    CURP_DUPLICADA(3, "Ya existe un cliente registrado con esa CURP"),
    RFC_DUPLICADO(4, "Ya existe un cliente registrado con ese RFC"),
    CORREO_DUPLICADO(5, "Ya existe un cliente registrado con ese correo"),

    // Recursos no encontrados y catálogos (Códigos 6 y 7)
    CLIENTE_NO_ENCONTRADO(6, "El cliente solicitado no existe"),
    CUENTA_NO_ENCONTRADA(7, "No existe una cuenta con ese numero"),
    CATALOGO_NACIONALIDAD_INVALIDA(7, "Código 7: La nacionalidad proporcionada no es válida o no existe en el catálogo"),
    CATALOGO_PAIS_INVALIDO(7, "Código 7: El país proporcionado no es válido o no existe en el catálogo"),

    // Credenciales, sesiones y autenticación (Códigos 8 y 9)
    CREDENCIALES_INVALIDAS(8, "Correo o password incorrectos"),
    SESION_INACTIVA_LOGIN(8, "El cliente esta dado de baja, no puede iniciar sesion"),
    TOKEN_FALTANTE(9, "Se requiere autenticacion para realizar esta operacion (header Authorization: Bearer <token>)"),
    TOKEN_INVALIDO(9, "Token invalido o expirado"),
    FORMATO_NUMERO_STRING(9, "Bad request - El campo debe ir entre comillas (String), no como numero"),

    // Autorización y permisos (Código 10)
    ACCESO_DENEGADO(10, "No tienes permisos para acceder o modificar los datos de este cliente"),
    ACCESO_DENEGADO_ADMIN(10, "Esta operacion solo puede realizarla un administrador"),

    // Reglas de negocio para estatus de cliente (Código 11)
    CLIENTE_INACTIVO_MODIFICACION(11, "El cliente se encuentra dado de baja y no puede ser modificado"),
    CLIENTE_YA_INACTIVO(11, "El cliente ya se encuentra dado de baja"),

    // Error interno del servidor (Código 99)
    ERROR_INTERNO(99, "Ocurrio un error inesperado, intenta de nuevo mas tarde");

    private final Integer codigo;
    private final String mensaje;

    MensajeError(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}
