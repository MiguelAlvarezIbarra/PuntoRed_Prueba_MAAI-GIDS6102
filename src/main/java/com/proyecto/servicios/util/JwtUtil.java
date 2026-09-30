package com.proyecto.servicios.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Genera y valida los JWT de sesion. Es una implementacion PROPIA y temporal
 * (segun lo indicado en el proyecto), no depende de un proveedor externo de identidad.
 * El token vive poco tiempo (ver seguridad.jwt.expiracion-minutos) porque la sesion real
 * se controla con la bandera sesionActiva + ultimaActividad en la tabla usuarios.
 */
@Component
public class JwtUtil {

    private final SecretKey llave;
    private final long expiracionMinutos;

    public JwtUtil(
            @Value("${seguridad.jwt.llave}") String llaveBase64,
            @Value("${seguridad.jwt.expiracion-minutos:15}") long expiracionMinutos
    ) {
        this.llave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(llaveBase64));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Integer clienteId, String correoHash, Integer rol) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expiracionMinutos * 60_000);

        return Jwts.builder()
                .subject(String.valueOf(clienteId))
                .claim("correoHash", correoHash)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(llave)
                .compact();
    }

    public Integer obtenerRol(String token) {
        Object rol = validarYObtenerClaims(token).get("rol");
        return rol == null ? null : Integer.valueOf(rol.toString());
    }

    public Claims validarYObtenerClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(llave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Token invalido o expirado", e);
        }
    }

    public Integer obtenerClienteId(String token) {
        return Integer.valueOf(validarYObtenerClaims(token).getSubject());
    }
}
