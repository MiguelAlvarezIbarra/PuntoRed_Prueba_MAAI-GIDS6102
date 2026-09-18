package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoAuthResponse {

    private Boolean success;

    private String token;

    private String message;

    private Integer status;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * GestoPago a veces regresa HTTP 200 pero con success=false y
     * token="INVALID" en vez de tronar la petición. Esto detecta ese caso
     * para no guardarlo como si fuera un token real.
     */
    public boolean isTokenValido() {
        return Boolean.TRUE.equals(success)
                && token != null
                && !token.isBlank()
                && !"INVALID".equalsIgnoreCase(token);
    }
}