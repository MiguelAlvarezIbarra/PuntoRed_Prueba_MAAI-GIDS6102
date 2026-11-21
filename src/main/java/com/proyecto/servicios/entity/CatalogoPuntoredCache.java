package com.proyecto.servicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalogo_puntored_cache")
@Getter
@Setter
@NoArgsConstructor
public class CatalogoPuntoredCache {

    @Id
    @Column(name = "clave", columnDefinition = "TEXT")
    private String clave;

    @Column(name = "fuente_datos", columnDefinition = "TEXT", nullable = false)
    private String fuenteDatos;

    @Column(name = "saldo_comercio", columnDefinition = "TEXT")
    private String saldoComercio;

    @Column(name = "total_servicios", nullable = false)
    private Integer totalServicios = 0;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
