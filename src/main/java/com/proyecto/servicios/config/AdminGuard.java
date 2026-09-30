package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.TokenFaltanteException;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Verifica que el header Authorization traiga un JWT valido y que su rol sea ADMIN.
 * Se usa a mano en los controllers que lo necesiten (no es un filtro global, porque
 * todavia no se pidio proteger TODOS los endpoints, solo el de creacion por un admin).
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
            throw new TokenFaltanteException("Se requiere un token de administrador (header Authorization: Bearer <jwt>)");
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            if (rol == null || rol != Roles.ADMIN) {
                throw new AccesoDenegadoException("Esta operacion solo puede realizarla un administrador");
            }
            return jwtUtil.obtenerClienteId(token);
        } catch (JwtException e) {
            throw new TokenFaltanteException("Token invalido o expirado");
        }
    }
}
