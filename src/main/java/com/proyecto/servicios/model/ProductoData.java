package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para productos. Evita exponer directamente la entidad JPA Producto.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoData {
    private Integer idProducto;
    private Integer idServicio;
    private Integer idCatTipoServicio;
    private String nombreProducto;
    private String nombreServicio;
    private Integer tipoFront;
    private String tipoReferencia;
    private String precio;
    private String logoUrl;
    private String descripcion;
    private String categoria;
    private String codigoProveedor;
    private String tipoOperacion;
    private LocalDateTime fechaActualizacion;
}
