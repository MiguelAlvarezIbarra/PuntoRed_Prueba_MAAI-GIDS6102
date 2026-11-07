package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de mensajes, códigos y estatus HTTP de error del sistema.
 */
@Getter
public enum MensajeError {

    // Validaciones generales (Código 1)
    VALIDACION_GENERAL(1, "Error de validacion en los datos de la peticion", HttpStatus.BAD_REQUEST),
    NOMBRE_OBLIGATORIO(1, "Código 1: El nombre es obligatorio", HttpStatus.BAD_REQUEST),
    APELLIDO_PATERNO_OBLIGATORIO(1, "Código 1: El apellido paterno es obligatorio", HttpStatus.BAD_REQUEST),
    APELLIDO_MATERNO_OBLIGATORIO(1, "Código 1: El apellido materno es obligatorio", HttpStatus.BAD_REQUEST),
    FECHA_NACIMIENTO_OBLIGATORIA(1, "Código 1: La fecha de nacimiento es obligatoria", HttpStatus.BAD_REQUEST),
    MAYOR_EDAD_REQUERIDO(1, "El cliente debe ser mayor de edad (18 anios o mas)", HttpStatus.BAD_REQUEST),
    PASSWORD_OBLIGATORIO(1, "El password es obligatorio para registrar el cliente", HttpStatus.BAD_REQUEST),
    ROL_INVALIDO(1, "El rol debe ser 1 (administrador) o 2 (usuario normal)", HttpStatus.BAD_REQUEST),
    PETICION_MALFORMADA(1, "Error de validacion: la peticion esta mal formada o le faltan datos", HttpStatus.BAD_REQUEST),
    CLIENTE_YA_ACTIVO(1, "El cliente ya se encuentra activo", HttpStatus.BAD_REQUEST),
    CUENTA_NO_GENERADA(1, "No fue posible generar un numero de cuenta unico, intenta de nuevo", HttpStatus.BAD_REQUEST),
    FORMATO_DATO_INCORRECTO(1, "Formato de dato incorrecto en el campo: %s", HttpStatus.BAD_REQUEST),
    RFC_NO_COINCIDE(1, "Las primeras 10 posiciones del RFC (%s) no coinciden con los datos del cliente. Deberían ser: %s", HttpStatus.BAD_REQUEST),
    CURP_NO_COINCIDE(1, "Las primeras 10 posiciones de la CURP (%s) no coinciden con los datos del cliente. Deberían ser: %s", HttpStatus.BAD_REQUEST),
    SEXO_CURP_NO_COINCIDE(1, "El sexo en la CURP (%s) no coincide con el sexo del cliente (%s).", HttpStatus.BAD_REQUEST),
    DATOS_NO_VALIDOS(1, "Datos no validos", HttpStatus.BAD_REQUEST),
    TELEFONO_RECARGA_OBLIGATORIO(1, "Código 1: El número de teléfono (10 dígitos) es obligatorio cuando el tipo de pago es TELEFONO (Recarga)", HttpStatus.BAD_REQUEST),
    REFERENCIA_PAGO_OBLIGATORIA(1, "Código 1: La referencia es obligatoria cuando el tipo de pago es REFERENCIA (Pago Bancario)", HttpStatus.BAD_REQUEST),

    // Formatos de entrada (5 fecha, 6 numerico)
    FORMATO_FECHA_INVALIDO(5, "Formato de Fecha Incorrecta", HttpStatus.BAD_REQUEST),
    FORMATO_NUMERICO_INVALIDO(6, "Formato numérico inválido. No se permite enviar números como texto (con comillas) ni formatos incorrectos en el campo: ", HttpStatus.BAD_REQUEST),

    // Duplicidad y conflictos de integridad (Códigos 2 a 5)
    CLIENTE_YA_REGISTRADO(2, "Ya existe un registro con esos datos (CURP, RFC, correo o cuenta)", HttpStatus.CONFLICT),
    CURP_DUPLICADA(3, "Ya existe un cliente registrado con esa CURP", HttpStatus.CONFLICT),
    RFC_DUPLICADO(4, "Ya existe un cliente registrado con ese RFC", HttpStatus.CONFLICT),
    CORREO_DUPLICADO(5, "Ya existe un cliente registrado con ese correo", HttpStatus.CONFLICT),

    // Recursos no encontrados y catálogos (Códigos 6 y 7)
    CLIENTE_NO_ENCONTRADO(6, "Código 6: El cliente solicitado no existe", HttpStatus.NOT_FOUND),
    CUENTA_NO_ENCONTRADA(7, "Código 7: No existe una cuenta con ese numero", HttpStatus.NOT_FOUND),
    CATALOGO_NACIONALIDAD_INVALIDA(7, "Código 7: La nacionalidad proporcionada no es válida o no existe en el catálogo", HttpStatus.BAD_REQUEST),
    CATALOGO_PAIS_INVALIDO(7, "Código 7: El país proporcionado no es válido o no existe en el catálogo", HttpStatus.BAD_REQUEST),

    // Credenciales, sesiones y autenticación (Códigos 8 y 9)
    CREDENCIALES_INVALIDAS(8, "Correo o password incorrectos", HttpStatus.UNAUTHORIZED),
    SESION_INACTIVA_LOGIN(8, "El cliente esta dado de baja, no puede iniciar sesion", HttpStatus.BAD_REQUEST),
    TOKEN_FALTANTE(9, "Se requiere autenticacion para realizar esta operacion (header Authorization: Bearer <token>)", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALIDO(9, "Token invalido o expirado", HttpStatus.UNAUTHORIZED),
    FORMATO_NUMERO_STRING(9, "Bad request - El campo '%s' debe ir entre comillas (String), no como numero", HttpStatus.BAD_REQUEST),

    // Autorización y permisos (Código 10)
    ACCESO_DENEGADO(10, "No tienes permisos para acceder o modificar los datos de este cliente", HttpStatus.FORBIDDEN),
    ACCESO_DENEGADO_ADMIN(10, "Esta operacion solo puede realizarla un administrador", HttpStatus.FORBIDDEN),

    // Reglas de negocio para estatus de cliente y cuenta (Código 11)
    CLIENTE_INACTIVO_MODIFICACION(11, "El cliente se encuentra dado de baja y no puede ser modificado", HttpStatus.CONFLICT),
    CLIENTE_YA_INACTIVO(11, "El cliente ya se encuentra dado de baja", HttpStatus.CONFLICT),
    CLIENTE_INACTIVO_COMPRA(11, "Código 11: El cliente se encuentra dado de baja y no puede realizar compras ni iniciar sesión", HttpStatus.CONFLICT),
    CUENTA_INACTIVA_COMPRA(11, "Código 11: La cuenta se encuentra inactiva y no puede realizar compras", HttpStatus.CONFLICT),

    // Errores a nivel HTTP (Códigos 12 a 14)
    RECURSO_NO_ENCONTRADO(12, "El recurso solicitado no existe", HttpStatus.NOT_FOUND),
    METODO_NO_PERMITIDO(13, "El metodo HTTP no esta permitido para este recurso", HttpStatus.METHOD_NOT_ALLOWED),
    TIPO_CONTENIDO_NO_SOPORTADO(14, "Tipo de contenido no soportado, envia la peticion como application/json", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // Pagos, Saldo y 2FA (Códigos 15 a 17)
    SALDO_INSUFICIENTE(15, "Código 15: Saldo insuficiente en la cuenta para realizar el pago", HttpStatus.BAD_REQUEST),
    PIN_2FA_NO_SOLICITADO(16, "Código 16: No existe un PIN 2FA activo para este cliente. Solicite primero el envío del PIN al correo", HttpStatus.BAD_REQUEST),
    PIN_2FA_EXPIRADO(16, "Código 16: El PIN de autenticación 2FA ha expirado. Solicite uno nuevo", HttpStatus.BAD_REQUEST),
    PIN_2FA_INVALIDO(16, "Código 16: El PIN de autenticación 2FA es incorrecto", HttpStatus.BAD_REQUEST),
    SENTX_CIFRADO_INVALIDO(17, "Código 17: No se pudo verificar el cifrado de la petición SenTx con el token", HttpStatus.BAD_REQUEST),

    // Integracion con GestoPago (Códigos 20 a 24)
    GESTOPAGO_AUTENTICACION(20, "No se pudo autenticar con el proveedor de productos", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_TIMEOUT(21, "Tiempo de espera agotado al comunicarse con el proveedor de productos", HttpStatus.GATEWAY_TIMEOUT),
    GESTOPAGO_COMUNICACION(22, "Error de comunicacion con el proveedor de productos", HttpStatus.SERVICE_UNAVAILABLE),
    GESTOPAGO_RESPUESTA(23, "El proveedor de productos respondio con un error", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_PAGO_RECHAZADO(24, "Código %s: %s", HttpStatus.BAD_REQUEST),

    // Error interno del servidor (Código 99)
    ERROR_INTERNO(99, "Ocurrio un error inesperado, intenta de nuevo mas tarde", HttpStatus.INTERNAL_SERVER_ERROR);

    private final Integer codigo;
    private final String mensaje;
    private final HttpStatus status;

    MensajeError(Integer codigo, String mensaje, HttpStatus status) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.status = status;
    }

    /**
     * Para los mensajes que llevan datos variables (marcados con %s).
     */
    public String formatear(Object... args) {
        return String.format(mensaje, args);
    }
}
