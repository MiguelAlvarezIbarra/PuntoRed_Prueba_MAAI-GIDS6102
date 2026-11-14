package com.proyecto.servicios.model.puntored;

import lombok.Data;

/**
 * Criterios opcionales para consultar el catalogo dinamico de Puntored.
 */
@Data
public class CatalogoBusquedaRequest {
    private String tipoOperacion;
    private String categoria;
    private String idCategoriaRecaudo;
    private String idTipoPin;
    private String idSubcategoriaPin;
    private String texto;
    private Boolean forzarRecarga;
}
