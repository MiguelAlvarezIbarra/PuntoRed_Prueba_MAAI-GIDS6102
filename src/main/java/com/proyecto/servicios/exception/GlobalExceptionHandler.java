package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoIntegrationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.model.GenericResponse;
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Centraliza el manejo de errores de toda la API: cada excepcion de negocio se traduce
 * a un GenericResponse utilizando el catalogo enum MensajeError y el HTTP status correcto.
 * Los errores no controlados (500) se loguean completos en el servidor pero al cliente
 * solo se le regresa un mensaje generico seguro, para no exponer informacion sensible.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Extrae el numero de "Código N:" que traen los message de los DTOs. */
    private static final Pattern PATRON_CODIGO = Pattern.compile("Código (\\d+):");

    // ---------------------------------------------------------------
    // Validaciones de entrada (Bean Validation / formato JSON)
    // ---------------------------------------------------------------

    /**
     * En caso de que alguna validacion no se cumpla para algun DTO se lanza MethodArgumentNotValidException.
     * Se regresa cada campo con su mensaje; si el mensaje trae "Código N:" se usa N como codigo de respuesta.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> listError = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            listError.merge(fieldError.getField(), fieldError.getDefaultMessage(), (a, b) -> a + " | " + b);
        }
        log.warn("Error de validacion: {}", listError);
        int codigo = extraerCodigo(listError.values());
        return construir(codigo, MensajeError.DATOS_NO_VALIDOS.getMensaje() + " " + listError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraint(ConstraintViolationException ex) {
        // Solo se expone el nombre del campo, no la ruta interna metodo.argumento.campo
        Map<String, String> listError = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacion : ex.getConstraintViolations()) {
            listError.merge(ultimoNodo(violacion.getPropertyPath()), violacion.getMessage(), (a, b) -> a + " | " + b);
        }
        log.warn("Error de validacion (constraint): {}", listError);
        int codigo = extraerCodigo(listError.values());
        return construir(codigo, MensajeError.DATOS_NO_VALIDOS.getMensaje() + " " + listError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatchEx && mismatchEx.getTargetType() != null) {
            Class<?> tipo = mismatchEx.getTargetType();
            String campo = rutaCampo(mismatchEx);
            log.warn("Formato invalido en el campo '{}' (tipo esperado {})", campo, tipo.getSimpleName());

            if (LocalDate.class.equals(tipo)) {
                return construir(MensajeError.FORMATO_FECHA_INVALIDO, HttpStatus.BAD_REQUEST);
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
        log.warn("Cuerpo de la peticion ilegible o JSON mal formado");
        return construir(MensajeError.PETICION_MALFORMADA, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GenericResponse> handleRequestMalformado(Exception ex) {
        log.warn("Request mal formado: {}", ex.getClass().getSimpleName());
        return construir(MensajeError.PETICION_MALFORMADA, HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------
    // Excepciones de negocio
    // ---------------------------------------------------------------

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<GenericResponse> handleValidacionNegocio(ValidacionException ex) {
        log.warn("Error de validacion de negocio: {}", ex.getMessage());
        return construir(ex.getCodigo(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        log.warn("Cliente ya registrado: {}", ex.getMessage());
        return construir(MensajeError.CLIENTE_YA_REGISTRADO.getCodigo(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse> handleCurpDuplicada(CurpDuplicadaException ex) {
        log.warn("CURP duplicada: {}", ex.getMessage());
        return construir(MensajeError.CURP_DUPLICADA.getCodigo(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse> handleRfcDuplicado(RfcDuplicadoException ex) {
        log.warn("RFC duplicado: {}", ex.getMessage());
        return construir(MensajeError.RFC_DUPLICADO.getCodigo(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<GenericResponse> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        log.warn("Correo duplicado: {}", ex.getMessage());
        return construir(MensajeError.CORREO_DUPLICADO.getCodigo(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return construir(MensajeError.CLIENTE_NO_ENCONTRADO.getCodigo(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return construir(MensajeError.CUENTA_NO_ENCONTRADA.getCodigo(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClienteInactivoException.class)
    public ResponseEntity<GenericResponse> handleClienteInactivo(ClienteInactivoException ex) {
        log.warn("Cliente inactivo: {}", ex.getMessage());
        return construir(MensajeError.CLIENTE_INACTIVO_MODIFICACION.getCodigo(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TokenFaltanteException.class)
    public ResponseEntity<GenericResponse> handleTokenFaltante(TokenFaltanteException ex) {
        log.warn("Token faltante o invalido: {}", ex.getMessage());
        return construir(MensajeError.TOKEN_FALTANTE.getCodigo(), ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<GenericResponse> handleAccesoDenegado(AccesoDenegadoException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return construir(MensajeError.ACCESO_DENEGADO.getCodigo(), ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de login invalido: {}", ex.getMessage());
        return construir(MensajeError.CREDENCIALES_INVALIDAS.getCodigo(), ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad en base de datos");
        return construir(MensajeError.CLIENTE_YA_REGISTRADO, HttpStatus.CONFLICT);
    }

    // ---------------------------------------------------------------
    // Integracion con GestoPago: al cliente solo se le regresa un mensaje
    // seguro del catalogo; el detalle tecnico queda unicamente en el log.
    // ---------------------------------------------------------------

    @ExceptionHandler(GestoPagoAuthenticationException.class)
    public ResponseEntity<GenericResponse> handleGestoPagoAutenticacion(GestoPagoAuthenticationException ex) {
        logIntegracion(ex);
        return construir(MensajeError.GESTOPAGO_AUTENTICACION, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(GestoPagoTimeoutException.class)
    public ResponseEntity<GenericResponse> handleGestoPagoTimeout(GestoPagoTimeoutException ex) {
        logIntegracion(ex);
        return construir(MensajeError.GESTOPAGO_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(GestoPagoCommunicationException.class)
    public ResponseEntity<GenericResponse> handleGestoPagoComunicacion(GestoPagoCommunicationException ex) {
        logIntegracion(ex);
        return construir(MensajeError.GESTOPAGO_COMUNICACION, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({GestoPagoResponseException.class, GestoPagoIntegrationException.class})
    public ResponseEntity<GenericResponse> handleGestoPagoRespuesta(GestoPagoIntegrationException ex) {
        logIntegracion(ex);
        return construir(MensajeError.GESTOPAGO_RESPUESTA, HttpStatus.BAD_GATEWAY);
    }

    // ---------------------------------------------------------------
    // Errores a nivel HTTP (antes caian en el 500 generico)
    // ---------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoEncontrado(NoResourceFoundException ex) {
        log.warn("Recurso inexistente: {}", ex.getResourcePath());
        return construir(MensajeError.RECURSO_NO_ENCONTRADO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo HTTP no permitido: {}", ex.getMethod());
        return construir(MensajeError.METODO_NO_PERMITIDO, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleTipoContenido(HttpMediaTypeNotSupportedException ex) {
        log.warn("Content-Type no soportado: {}", ex.getContentType());
        return construir(MensajeError.TIPO_CONTENIDO_NO_SOPORTADO, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneral(Exception ex) {
        log.error("Error no controlado", ex);
        return construir(MensajeError.ERROR_INTERNO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void logIntegracion(GestoPagoIntegrationException ex) {
        String causa = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "N/A";
        log.error("Error de integracion GestoPago [{}] causa={} detalle={}", ex.getClass().getSimpleName(), causa, ex.getMessage());
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

    private ResponseEntity<GenericResponse> construir(MensajeError error, HttpStatus status) {
        return construir(error.getCodigo(), error.getMensaje(), status);
    }

    private ResponseEntity<GenericResponse> construir(Integer codigo, String mensaje, HttpStatus status) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
