package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centraliza el manejo de errores de toda la API: cada excepcion de negocio se traduce
 * a un GenericResponse con un codigo/mensaje entendible y el HTTP status correcto.
 * Los errores no controlados (500) se loguean completos en el servidor pero al cliente
 * solo se le regresa un mensaje generico, para no exponer informacion sensible.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Error de validacion: {}", mensaje);
        
        int codigo = 1;
        if (mensaje.contains("Código ")) {
            try {
                String extract = mensaje.substring(mensaje.indexOf("Código ") + 7);
                extract = extract.substring(0, extract.indexOf(":"));
                codigo = Integer.parseInt(extract.trim());
            } catch (Exception e) {
                // fallback to 1 if parsing fails
            }
        }
        
        return construir(codigo, "Error de validacion: " + mensaje, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraint(ConstraintViolationException ex) {
        log.warn("Error de validacion (constraint): {}", ex.getMessage());
        return construir(1, "Error de validacion: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<GenericResponse> handleValidacionNegocio(ValidacionException ex) {
        log.warn("Error de validacion de negocio: {}", ex.getMessage());
        return construir(1, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        log.warn("Cliente ya registrado: {}", ex.getMessage());
        return construir(2, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse> handleCurpDuplicada(CurpDuplicadaException ex) {
        log.warn("CURP duplicada: {}", ex.getMessage());
        return construir(3, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse> handleRfcDuplicado(RfcDuplicadoException ex) {
        log.warn("RFC duplicado: {}", ex.getMessage());
        return construir(4, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<GenericResponse> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        log.warn("Correo duplicado: {}", ex.getMessage());
        return construir(5, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return construir(6, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return construir(7, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TokenFaltanteException.class)
    public ResponseEntity<GenericResponse> handleTokenFaltante(TokenFaltanteException ex) {
        log.warn("Token faltante o invalido: {}", ex.getMessage());
        return construir(9, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<GenericResponse> handleAccesoDenegado(AccesoDenegadoException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return construir(10, ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de login invalido: {}", ex.getMessage());
        return construir(8, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
            com.fasterxml.jackson.databind.exc.MismatchedInputException mismatchEx = (com.fasterxml.jackson.databind.exc.MismatchedInputException) cause;
            if (mismatchEx.getTargetType() != null && mismatchEx.getTargetType().equals(java.time.LocalDate.class)) {
                return construir(5, "Formato de Fecha Incorrecta", HttpStatus.BAD_REQUEST);
            }
            if (mismatchEx.getTargetType() != null && java.lang.Number.class.isAssignableFrom(mismatchEx.getTargetType())) {
                String campo = mismatchEx.getPath().isEmpty() ? "desconocido" : mismatchEx.getPath().get(0).getFieldName();
                return construir(6, "Formato numérico inválido. No se permite enviar números como texto (con comillas) ni formatos incorrectos en el campo: " + campo, HttpStatus.BAD_REQUEST);
            }
            if (!mismatchEx.getPath().isEmpty()) {
                return construir(1, "Formato de dato incorrecto en el campo: " + mismatchEx.getPath().get(0).getFieldName(), HttpStatus.BAD_REQUEST);
            }
        }
        // Fallback para otros errores de formato, como un JSON roto
        return construir(1, "Error de validacion: la peticion esta mal formada o le faltan datos", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GenericResponse> handleRequestMalformado(Exception ex) {
        log.warn("Request mal formado: {}", ex.getClass().getSimpleName());
        return construir(1, "Error de validacion: la peticion esta mal formada o le faltan datos", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleIntegridad(DataIntegrityViolationException ex) {
        // Respaldo de las restricciones UNIQUE de la BD (por ejemplo, dos registros simultaneos).
        log.warn("Violacion de integridad en base de datos");
        return construir(2, "Ya existe un registro con esos datos (CURP, RFC, correo o cuenta)", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneral(Exception ex) {
        // Se loguea completo del lado del servidor, pero NUNCA se manda el stacktrace ni
        // detalles internos al cliente.
        log.error("Error no controlado", ex);
        return construir(99, "Ocurrio un error inesperado, intenta de nuevo mas tarde", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<GenericResponse> construir(Integer codigo, String mensaje, HttpStatus status) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
