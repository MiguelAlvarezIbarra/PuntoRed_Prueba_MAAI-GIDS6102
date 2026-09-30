package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "clientes")
@Entity
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // ---- Datos personales ----
    @Column(name = "nombre", columnDefinition = "TEXT", nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @Column(name = "apellido_paterno", columnDefinition = "TEXT", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", columnDefinition = "TEXT", nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", columnDefinition = "TEXT", nullable = false, unique = true)
    private String curp;

    @Column(name = "rfc", columnDefinition = "TEXT", nullable = false, unique = true)
    private String rfc;

    @Column(name = "sexo", columnDefinition = "TEXT")
    private String sexo;

    @Column(name = "nacionalidad", columnDefinition = "TEXT")
    private String nacionalidad;

    @Column(name = "estado_civil", columnDefinition = "TEXT")
    private String estadoCivil;

    // ---- Datos de contacto ----
    @Column(name = "correo", columnDefinition = "TEXT", nullable = false, unique = true)
    private String correo;

    @Column(name = "telefono_movil", columnDefinition = "TEXT", nullable = false)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", columnDefinition = "TEXT")
    private String telefonoAlternativo;

    // ---- Informacion laboral ----
    @Column(name = "ocupacion", columnDefinition = "TEXT")
    private String ocupacion;

    @Column(name = "empresa", columnDefinition = "TEXT")
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 14, scale = 2)
    private BigDecimal ingresoMensual;

    // ---- Control ----
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
    }
}
