package com.proyecto.servicios.exception;

import com.proyecto.servicios.dto.response.ApiResponse;
import com.proyecto.servicios.dto.response.ResponseCode;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(GestoPagoAuthenticationException e) {
        log.error("Error de autenticación con GestoPago: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ResponseCode.ERROR_AUTENTICACION, e.getMessage()));
    }

    @ExceptionHandler(GestoPagoTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeout(GestoPagoTimeoutException e) {
        log.error("Timeout al comunicarse con GestoPago: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error(ResponseCode.ERROR_TIMEOUT, e.getMessage()));
    }

    @ExceptionHandler(GestoPagoCommunicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleComunicacion(GestoPagoCommunicationException e) {
        log.error("Error de comunicación con GestoPago: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ResponseCode.ERROR_COMUNICACION, e.getMessage()));
    }

    @ExceptionHandler(GestoPagoResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleRespuesta(GestoPagoResponseException e) {
        log.error("GestoPago devolvió un error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ResponseCode.ERROR_RESPUESTA_GESTOPAGO, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenerico(Exception e) {
        log.error("Error no controlado: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseCode.ERROR_GENERICO, e.getMessage()));
    }
}