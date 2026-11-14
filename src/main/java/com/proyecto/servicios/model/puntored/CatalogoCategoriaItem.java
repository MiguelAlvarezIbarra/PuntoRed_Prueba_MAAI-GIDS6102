package com.proyecto.servicios.model.puntored;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una categoria dinamica del catalogo Puntored (con su logotipo oficial).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoCategoriaItem {
    private String idCategoria;
    private String nombreCategoria;
    private String tipoOperacion;
    private String logoUrl;
    private String categoriaPadre;
    private Integer totalElementos;
}
