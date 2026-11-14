package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado de validacion previa de referencia/convenio con token firmado para desbloquear el pago.
 */
@Data
public class ValidacionReferenciaResponse {
    private Integer codigo;
    private String mensaje;
    private Boolean referenciaValida;
    private String tokenValidacion;
    private String tipoOperacion;
    private String idConvenio;
    private String codigoProducto;
    private String referencia1;
    private String referencia2;
    private String codigoRespuestaConvenio;
    private String mensajeConvenio;
    private BigDecimal montoSugerido;
    private BigDecimal montoMinimo;
    private BigDecimal montoMaximo;
    private Boolean montoEditable;
    private String trace;
    private List<OpcionMontoConvenio> opciones;
    private Map<String, Object> camposAdicionalesAval;
}
