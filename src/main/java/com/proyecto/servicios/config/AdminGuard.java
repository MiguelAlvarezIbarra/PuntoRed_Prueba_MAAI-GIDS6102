package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

/**
 * Verifica que el header Authorization traiga un JWT valido y que su rol sea ADMIN.
 * Utiliza MensajeError enum para estandarizar los mensajes.
 */
@Component
public class AdminGuard {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtUtil jwtUtil;

    public AdminGuard(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * @return el clienteId del admin que hizo la peticion (util para logs)
     */
    public Integer exigirAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            throw new ValidacionException(MensajeError.TOKEN_FALTANTE);
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            if (rol == null || !rol.equals(Roles.ADMIN)) {
                throw new ValidacionException(MensajeError.ACCESO_DENEGADO_ADMIN);
            }
            return jwtUtil.obtenerClienteId(token);
        } catch (JwtException e) {
            throw new ValidacionException(MensajeError.TOKEN_INVALIDO);
        }
    }
}
