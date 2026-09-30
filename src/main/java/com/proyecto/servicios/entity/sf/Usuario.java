package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla de SEGURIDAD del cliente: credenciales de login, sesion y biometria.
 * Todo lo que es texto sensible (correo, password, jwt) se guarda cifrado/hasheado.
 * Nunca se debe exponer esta entidad directamente en una respuesta de API.
 */
@Table(name = "usuarios")
@Entity
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_usuario_cliente"))
    private Cliente cliente;

    // Correo cifrado con AES (no se puede buscar directo -> se busca por correoHash)
    @Column(name = "correo_cifrado", columnDefinition = "TEXT", nullable = false)
    private String correoCifrado;

    // SHA-256 del correo en minusculas, para poder localizar el usuario sin descifrar
    @Column(name = "correo_hash", columnDefinition = "TEXT", nullable = false, unique = true)
    private String correoHash;

    // BCrypt del password, nunca se guarda en texto plano ni reversible
    @Column(name = "password_hash", columnDefinition = "TEXT", nullable = false)
    private String passwordHash;

    // JWT vigente, cifrado. Se sobreescribe en cada login. Nulo si no hay sesion.
    @Column(name = "jwt_cifrado", columnDefinition = "TEXT")
    private String jwtCifrado;

    // Score de similitud de reconocimiento facial (0.0 a 1.0). Aun no se usa -> nullable.
    @Column(name = "datos_biometricos")
    private Double datosBiometricos;

    // 1 = administrador, 2 = usuario normal (cliente). Por defecto siempre 2.
    @Column(name = "rol", nullable = false)
    private Integer rol = 2;

    // true = sesion abierta y puede operar; false = debe volver a autenticarse
    @Column(name = "sesion_activa", nullable = false)
    private Boolean sesionActiva = false;

    // Se actualiza en cada request autenticado; el scheduler la usa para expirar por inactividad
    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @PrePersist
    public void prePersist() {
        if (sesionActiva == null) {
            sesionActiva = false;
        }
        if (rol == null) {
            rol = 2;
        }
    }
}
