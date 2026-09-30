package com.proyecto.servicios.config;

import com.proyecto.servicios.service.UsuarioService;
import com.proyecto.servicios.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Si el request trae un JWT valido, refresca ultima_actividad del usuario para que el
 * scheduler no le cierre la sesion. NO bloquea requests sin token (no se pidio proteger
 * los endpoints todavia, solo controlar la expiracion por inactividad).
 */
@Component
@Slf4j
public class ActividadSesionInterceptor implements HandlerInterceptor {

    private static final String PREFIJO_BEARER = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            try {
                Integer clienteId = jwtUtil.obtenerClienteId(header.substring(PREFIJO_BEARER.length()));
                usuarioService.registrarActividad(clienteId);
            } catch (JwtException e) {
                // Token invalido/expirado: se ignora aqui, sin loguear el token.
                log.debug("Token invalido en request, no se registra actividad");
            }
        }
        return true;
    }
}
