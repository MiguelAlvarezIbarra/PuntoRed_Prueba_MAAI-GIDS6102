package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "domicilios")
@Entity
@Getter
@Setter
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_domicilio_cliente"))
    private Cliente cliente;

    @Column(name = "calle", columnDefinition = "TEXT", nullable = false)
    private String calle;

    @Column(name = "numero_exterior", columnDefinition = "TEXT", nullable = false)
    private String numeroExterior;

    @Column(name = "numero_interior", columnDefinition = "TEXT")
    private String numeroInterior;

    @Column(name = "colonia", columnDefinition = "TEXT", nullable = false)
    private String colonia;

    @Column(name = "municipio", columnDefinition = "TEXT", nullable = false)
    private String municipio;

    @Column(name = "estado", columnDefinition = "TEXT", nullable = false)
    private String estado;

    @Column(name = "codigo_postal", columnDefinition = "TEXT", nullable = false)
    private String codigoPostal;

    @Column(name = "pais", columnDefinition = "TEXT", nullable = false)
    private String pais;
}
