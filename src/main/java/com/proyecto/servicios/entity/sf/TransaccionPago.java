package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "transacciones_pago")
@Getter
@Setter
@NoArgsConstructor
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cuenta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaccion_cuenta"))
    private Cuenta cuenta;

    @Column(name = "numero_cuenta", columnDefinition = "TEXT", nullable = false)
    private String numeroCuenta;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "tipo_pago", columnDefinition = "TEXT", nullable = false)
    private String tipoPago;

    @Column(name = "telefono_referencia", columnDefinition = "TEXT", nullable = false)
    private String telefonoReferencia;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "upc", columnDefinition = "TEXT", nullable = false)
    private String upc;

    @Column(name = "unidad", columnDefinition = "TEXT", nullable = false)
    private String unidad;

    @Column(name = "hora_local", columnDefinition = "TEXT", nullable = false)
    private String horaLocal;

    @Column(name = "sentx_cifrado", columnDefinition = "TEXT", nullable = false)
    private String senTxCifrado;

    @Column(name = "id_tx_gestopago", columnDefinition = "TEXT")
    private String idTxGestoPago;

    @Column(name = "num_autorizacion", columnDefinition = "TEXT")
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", columnDefinition = "TEXT", nullable = false)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT", nullable = false)
    private String mensajeRespuesta;

    @Column(name = "nombre_servicio", columnDefinition = "TEXT")
    private String nombreServicio;

    @Column(name = "nombre_producto", columnDefinition = "TEXT")
    private String nombreProducto;

    @Column(name = "categoria", columnDefinition = "TEXT")
    private String categoria;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "referencia_secundaria", columnDefinition = "TEXT")
    private String referenciaSecundaria;

    @Column(name = "trace", columnDefinition = "TEXT")
    private String trace;

    @Column(name = "estado", columnDefinition = "TEXT", nullable = false)
    private String estado = "EXITOSA";

    @Column(name = "ticket_json", columnDefinition = "TEXT")
    private String ticketJson;

    @Column(name = "proveedor", columnDefinition = "TEXT", nullable = false)
    private String proveedor = "GESTOPAGO";

    @Column(name = "codigo_producto_externo", columnDefinition = "TEXT")
    private String codigoProductoExterno;

    @Column(name = "codigo_convenio_externo", columnDefinition = "TEXT")
    private String codigoConvenioExterno;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now(ZoneId.systemDefault());
        }
        if (estado == null || estado.isBlank()) {
            estado = "EXITOSA";
        }
        if (proveedor == null || proveedor.isBlank()) {
            proveedor = "GESTOPAGO";
        }
    }
}
