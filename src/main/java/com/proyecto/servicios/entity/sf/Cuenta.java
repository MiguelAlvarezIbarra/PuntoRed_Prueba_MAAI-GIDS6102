package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "cuentas")
@Entity
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cuenta_cliente"))
    private Cliente cliente;

    @Column(name = "numero_cuenta", columnDefinition = "TEXT", nullable = false, unique = true)
    private String numeroCuenta;

    @Column(name = "saldo", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo;

    // ACTIVA / INACTIVA
    @Column(name = "estatus", columnDefinition = "TEXT", nullable = false)
    private String estatus;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @PrePersist
    public void prePersist() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
        if (estatus == null) {
            estatus = "ACTIVA";
        }
    }
}
