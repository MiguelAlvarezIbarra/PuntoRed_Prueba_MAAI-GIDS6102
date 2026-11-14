package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Servicio o producto normalizado del catalogo dinamico de Puntored H2H V2.
 */
@Data
public class CatalogoServicioItem {
    private String claveUnica;
    private String tipoOperacion;
    private String categoria;
    private String nombreServicio;
    private String nombreProducto;
    private String descripcion;
    private String proveedorCodigo;
    private String proveedorNombre;
    private String logoUrl;
    private String codigoProducto;
    private String codigoCategoria;
    private String idConvenio;
    private String sku;
    private String idPin;
    private String idSubcategoriaPin;
    private BigDecimal precioSugerido;
    private String validez;
    private String etiquetaReferencia1;
    private String etiquetaReferencia2;
    private Integer tipoRecaudo;
    private String nombreBanco;
    private Boolean requiereValidacionReferencia;
    private String terminosUrl;
}
