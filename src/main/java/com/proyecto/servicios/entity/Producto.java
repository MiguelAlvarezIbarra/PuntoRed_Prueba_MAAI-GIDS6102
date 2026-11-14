package com.proyecto.servicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "productos_seq")
    @SequenceGenerator(name = "productos_seq", sequenceName = "productos_seq", allocationSize = 50)
    private Long id;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "nombre_servicio", nullable = false)
    private String nombreServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "tipo_referencia")
    private String tipoReferencia;

    @Column(name = "precio")
    private String precio;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "categoria", length = 120)
    private String categoria;

    @Column(name = "codigo_proveedor", length = 60)
    private String codigoProveedor;

    @Column(name = "tipo_operacion", length = 30)
    private String tipoOperacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
