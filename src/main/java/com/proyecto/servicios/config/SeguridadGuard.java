package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

/**
 * Control de autorizacion por recurso:
 * Garantiza que cada usuario unicamente pueda acceder a los recursos para los que tiene permisos.
 * - Administradores (rol 1): acceso a cualquier cliente.
 * - Usuarios normales (rol 2): unicamente acceso a su propio registro de cliente.
 */
@Component
public class SeguridadGuard {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ClienteRepository clienteRepository;

    public SeguridadGuard(JwtUtil jwtUtil, ClienteRepository clienteRepository) {
        this.jwtUtil = jwtUtil;
        this.clienteRepository = clienteRepository;
    }

    /**
     * Valida que la peticion traiga un token valido y que el usuario tenga permisos
     * sobre el cliente identificado por rfcObjetivo.
     */
    public void validarAccesoClientePorRfc(String authorizationHeader, String rfcObjetivo) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            throw new ValidacionException(MensajeError.TOKEN_FALTANTE);
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            Integer tokenClienteId = jwtUtil.obtenerClienteId(token);

            if (rol != null && rol.equals(Roles.ADMIN)) {
                return;
            }

            Cliente cliente = clienteRepository.findByRfc(rfcObjetivo)
                    .orElseThrow(() -> new ValidacionException(MensajeError.CLIENTE_NO_ENCONTRADO));

            if (!cliente.getId().equals(tokenClienteId)) {
                throw new ValidacionException(MensajeError.ACCESO_DENEGADO);
            }
        } catch (JwtException e) {
            throw new ValidacionException(MensajeError.TOKEN_INVALIDO);
        }
    }

    /**
     * Extrae la informacion del token si esta presente.
     * Retorna null si no se envio encabezado de autorizacion.
     */
    public InfoToken extraerInfoToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            return null;
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            Integer clienteId = jwtUtil.obtenerClienteId(token);
            return new InfoToken(clienteId, rol);
        } catch (JwtException e) {
            throw new ValidacionException(MensajeError.TOKEN_INVALIDO);
        }
    }

    public static class InfoToken {
        private final Integer clienteId;
        private final Integer rol;

        public InfoToken(Integer clienteId, Integer rol) {
            this.clienteId = clienteId;
            this.rol = rol;
        }

        public Integer getClienteId() {
            return clienteId;
        }

        public Integer getRol() {
            return rol;
        }

        public boolean esAdmin() {
            return rol != null && rol.equals(Roles.ADMIN);
        }
    }
}
