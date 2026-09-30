package com.proyecto.servicios.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera numeros de cuenta candidatos de 10 digitos. La verificacion de unicidad
 * (contra la tabla cuentas) se hace en el servicio, reintentando si ya existe.
 */
@Component
public class GeneradorCuenta {

    private static final int LONGITUD = 10;
    private final SecureRandom random = new SecureRandom();

    public String generarCandidato() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        // El primer digito no es 0, para que siempre se vean 10 digitos "reales"
        sb.append(1 + random.nextInt(9));
        for (int i = 1; i < LONGITUD; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
