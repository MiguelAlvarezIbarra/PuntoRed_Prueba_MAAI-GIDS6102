package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.TokenFaltanteException;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Verifica que el header Authorization traiga un JWT valido y que su rol sea ADMIN.
 * Utiliza MensajeError enum para estandarizar los mensajes.
 */
@Component
public class AdminGuard {

    private static final String PREFIJO_BEARER = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * @return el clienteId del admin que hizo la peticion (util para logs)
     */
    public Integer exigirAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            throw new TokenFaltanteException(MensajeError.TOKEN_FALTANTE);
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            if (rol == null || !rol.equals(Roles.ADMIN)) {
                throw new AccesoDenegadoException(MensajeError.ACCESO_DENEGADO_ADMIN);
            }
            return jwtUtil.obtenerClienteId(token);
        } catch (JwtException e) {
            throw new TokenFaltanteException(MensajeError.TOKEN_INVALIDO);
        }
    }
}
