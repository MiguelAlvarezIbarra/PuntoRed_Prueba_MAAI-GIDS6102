package com.proyecto.servicios.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Genera un hash SHA-256 determinista (mismo texto -> mismo hash siempre), usado para
 * poder buscar un correo en la base de datos SIN tener que descifrar todos los registros.
 * El correo se normaliza a minusculas y sin espacios antes de hashear.
 */
@Component
public class HashUtil {

    public String sha256(String texto) {
        try {
            String normalizado = texto.trim().toLowerCase();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
