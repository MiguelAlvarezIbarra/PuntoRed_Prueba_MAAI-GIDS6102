package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import feign.RetryableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Unico manejador global de excepciones de toda la API.
 * Centraliza en un solo @ExceptionHandler todas las excepciones de validacion,
 * negocio, integracion y HTTP utilizando el catalogo MensajeError.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern PATRON_CODIGO = Pattern.compile("Código (\\d+):", Pattern.CANON_EQ);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleException(Exception ex) {
        if (ex instanceof ValidacionException valEx) {
            log.warn("Error de negocio [codigo={}]: {}", valEx.getCodigo(), valEx.getMessage());
            return construir(valEx.getCodigo(), valEx.getMessage(), valEx.getStatus());
        }
        if (ex instanceof MethodArgumentNotValidException manvEx) {
            return procesarMethodArgumentNotValid(manvEx);
        }
        if (ex instanceof ConstraintViolationException cvEx) {
            return procesarConstraintViolation(cvEx);
        }
        if (ex instanceof HttpMessageNotReadableException notReadableEx) {
            return procesarNotReadable(notReadableEx);
        }
        if (esRequestMalformado(ex)) {
            log.warn("Request mal formado: {}", ex.getClass().getSimpleName());
            return construir(MensajeError.PETICION_MALFORMADA);
        }
        if (ex instanceof DataIntegrityViolationException) {
            log.warn("Violacion de integridad en base de datos");
            return construir(MensajeError.CLIENTE_YA_REGISTRADO);
        }
        if (ex instanceof RetryableException || ex instanceof FeignException) {
            return procesarExcepcionProveedor(ex);
        }
        return procesarExcepcionHttpOGeneral(ex);
    }

    private ResponseEntity<GenericResponse> procesarMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> listError = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            listError.merge(fieldError.getField(), fieldError.getDefaultMessage(), (a, b) -> a + " | " + b);
        }
        log.warn("Error de validacion: {}", listError);
        int codigo = extraerCodigo(listError.values());
        HttpStatus status = resolverStatusPorCodigo(codigo);
        return construir(codigo, MensajeError.DATOS_NO_VALIDOS.getMensaje() + " " + listError, status);
    }

    private ResponseEntity<GenericResponse> procesarConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> listError = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacion : ex.getConstraintViolations()) {
            listError.merge(ultimoNodo(violacion.getPropertyPath()), violacion.getMessage(), (a, b) -> a + " | " + b);
        }
        log.warn("Error de validacion (constraint): {}", listError);
        int codigo = extraerCodigo(listError.values());
        HttpStatus status = resolverStatusPorCodigo(codigo);
        return construir(codigo, MensajeError.DATOS_NO_VALIDOS.getMensaje() + " " + listError, status);
    }

    private ResponseEntity<GenericResponse> procesarNotReadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatchEx && mismatchEx.getTargetType() != null) {
            return procesarMismatchedInput(mismatchEx);
        }
        log.warn("Cuerpo de la peticion ilegible o JSON mal formado");
        return construir(MensajeError.PETICION_MALFORMADA);
    }

    private ResponseEntity<GenericResponse> procesarMismatchedInput(MismatchedInputException mismatchEx) {
        Class<?> tipo = mismatchEx.getTargetType();
        String campo = rutaCampo(mismatchEx);
        log.warn("Formato invalido en el campo '{}' (tipo esperado {})", campo, tipo.getSimpleName());

        if (LocalDate.class.equals(tipo)) {
            return construir(MensajeError.FORMATO_FECHA_INVALIDO);
        }
        if (Number.class.isAssignableFrom(tipo)) {
            return construir(MensajeError.FORMATO_NUMERICO_INVALIDO.getCodigo(),
                    MensajeError.FORMATO_NUMERICO_INVALIDO.getMensaje() + campo, HttpStatus.BAD_REQUEST);
        }
        if (String.class.equals(tipo)) {
            return construir(MensajeError.FORMATO_NUMERO_STRING.getCodigo(),
                    MensajeError.FORMATO_NUMERO_STRING.formatear(campo), HttpStatus.BAD_REQUEST);
        }
        return construir(MensajeError.FORMATO_DATO_INCORRECTO.getCodigo(),
                MensajeError.FORMATO_DATO_INCORRECTO.formatear(campo), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<GenericResponse> procesarExcepcionProveedor(Exception ex) {
        log.error("Error de integracion con proveedor [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());
        if (ex instanceof RetryableException retryEx) {
            if (retryEx.getCause() instanceof SocketTimeoutException) {
                return construir(MensajeError.GESTOPAGO_TIMEOUT);
            }
            return construir(MensajeError.GESTOPAGO_COMUNICACION);
        }
        if (ex instanceof FeignException.Unauthorized || ex instanceof FeignException.Forbidden) {
            return construir(MensajeError.GESTOPAGO_AUTENTICACION);
        }
        return construir(MensajeError.GESTOPAGO_RESPUESTA);
    }

    private ResponseEntity<GenericResponse> procesarExcepcionHttpOGeneral(Exception ex) {
        if (ex instanceof NoResourceFoundException noResEx) {
            log.warn("Recurso inexistente: {}", noResEx.getResourcePath());
            return construir(MensajeError.RECURSO_NO_ENCONTRADO);
        }
        if (ex instanceof HttpRequestMethodNotSupportedException methodEx) {
            log.warn("Metodo HTTP no permitido: {}", methodEx.getMethod());
            return construir(MensajeError.METODO_NO_PERMITIDO);
        }
        if (ex instanceof HttpMediaTypeNotSupportedException mediaEx) {
            log.warn("Content-Type no soportado: {}", mediaEx.getContentType());
            return construir(MensajeError.TIPO_CONTENIDO_NO_SOPORTADO);
        }
        log.error("Error no controlado", ex);
        return construir(MensajeError.ERROR_INTERNO);
    }

    private boolean esRequestMalformado(Exception ex) {
        return ex instanceof MissingRequestHeaderException
                || ex instanceof MissingServletRequestParameterException
                || ex instanceof MethodArgumentTypeMismatchException;
    }

    private HttpStatus resolverStatusPorCodigo(int codigo) {
        if (codigo == MensajeError.CLIENTE_NO_ENCONTRADO.getCodigo()
                || codigo == MensajeError.CUENTA_NO_ENCONTRADA.getCodigo()) {
            return HttpStatus.NOT_FOUND;
        }
        if (codigo == MensajeError.CLIENTE_INACTIVO_COMPRA.getCodigo()) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private int extraerCodigo(Collection<String> mensajes) {
        for (String mensaje : mensajes) {
            if (mensaje == null) {
                continue;
            }
            Matcher matcher = PATRON_CODIGO.matcher(mensaje);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return MensajeError.VALIDACION_GENERAL.getCodigo();
    }

    private String rutaCampo(JsonMappingException ex) {
        String ruta = ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
        return ruta.isEmpty() ? "desconocido" : ruta;
    }

    private String ultimoNodo(Path path) {
        String nombre = null;
        for (Path.Node nodo : path) {
            nombre = nodo.getName();
        }
        return nombre != null ? nombre : path.toString();
    }

    private ResponseEntity<GenericResponse> construir(MensajeError error) {
        return construir(error.getCodigo(), error.getMensaje(), error.getStatus());
    }

    private ResponseEntity<GenericResponse> construir(Integer codigo, String mensaje, HttpStatus status) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
