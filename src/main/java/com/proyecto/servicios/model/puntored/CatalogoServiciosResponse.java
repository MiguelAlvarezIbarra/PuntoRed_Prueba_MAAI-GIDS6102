package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.util.List;

/**
 * Respuesta unificada del catalogo dinamico de productos y servicios Puntored.
 */
@Data
public class CatalogoServiciosResponse {
    private Integer codigo;
    private String mensaje;
    private String fuenteDatos;
    private String saldoComercio;
    private List<CatalogoCategoriaItem> categorias;
    private List<CatalogoServicioItem> servicios;
}
