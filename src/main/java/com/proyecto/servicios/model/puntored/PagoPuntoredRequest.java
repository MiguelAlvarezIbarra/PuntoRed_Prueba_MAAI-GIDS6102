package com.proyecto.servicios.model.puntored;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Solicitud para confirmar y ejecutar un pago o compra de servicio/recarga/paquete/pin en Puntored H2H V2.
 */
@Data
public class PagoPuntoredRequest {

    @NotBlank(message = "Código 1: El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{18}$", message = "Código 1: La cuenta debe tener exactamente 18 dígitos numéricos")
    private String numeroCuenta;

    @NotBlank(message = "Código 1: El tipo de operación es obligatorio")
    private String tipoOperacion;

    @NotBlank(message = "Código 1: El nombre del servicio es obligatorio")
    private String nombreServicio;

    private String nombreProducto;
    private String categoria;
    private String logoUrl;

    @NotBlank(message = "Código 1: La referencia o línea es obligatoria")
    private String referencia1;

    private String referencia2;

    @NotNull(message = "Código 1: El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "Código 1: El monto debe ser mayor o igual a 1.00")
    private BigDecimal monto;

    private String idConvenio;
    private String codigoProveedor;
    private String codigoCategoria;
    private String codigoProducto;
    private String sku;
    private String idPin;
    private String idSubcategoriaPin;
    private String codigoRegistro;
    private String codigoIac;
    private String tipoCuenta;
    private Integer tipoReferencia;
    private String traceValidacion;

    @NotBlank(message = "Código 28: Se requiere el token de validación de referencia antes de pagar")
    private String tokenValidacion;

    private BigDecimal montoQuery;
    private String aditionalValue54Eco;
    private String aditionalValue62Eco;
    private String accountDataAcredited;
    private String additionalCompanyCode;

    @NotNull(message = "Código 33: Se requiere confirmación explícita del resumen de pago")
    private Boolean confirmacionExplicita;

    @NotBlank(message = "Código 1: El PIN de autenticación 2FA es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "Código 1: El PIN 2FA debe tener exactamente 6 dígitos")
    private String pin2fa;
}
