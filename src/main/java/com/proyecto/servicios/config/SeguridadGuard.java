package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.TokenFaltanteException;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Valida que la peticion traiga un token valido y que el usuario tenga permisos
     * sobre el cliente identificado por rfcObjetivo.
     */
    public void validarAccesoClientePorRfc(String authorizationHeader, String rfcObjetivo) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            throw new TokenFaltanteException("Se requiere autenticacion para realizar esta operacion (header Authorization: Bearer <token>)");
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());
        try {
            Integer rol = jwtUtil.obtenerRol(token);
            Integer tokenClienteId = jwtUtil.obtenerClienteId(token);

            // Administrador tiene acceso total
            if (rol != null && rol.equals(Roles.ADMIN)) {
                return;
            }

            // Usuario normal: verificar que el RFC le pertenece
            Cliente cliente = clienteRepository.findByRfc(rfcObjetivo)
                    .orElseThrow(() -> new ClienteNoEncontradoException("El cliente con RFC " + rfcObjetivo + " no existe"));

            if (!cliente.getId().equals(tokenClienteId)) {
                throw new AccesoDenegadoException("No tienes permisos para consultar o modificar la informacion de otro cliente");
            }
        } catch (JwtException e) {
            throw new TokenFaltanteException("Token invalido o expirado");
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
            throw new TokenFaltanteException("Token invalido o expirado");
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
