package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import com.proyecto.servicios.util.CryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import com.proyecto.servicios.util.JwtUtil;
import com.proyecto.servicios.util.Roles;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private HashUtil hashUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${seguridad.jwt.expiracion-minutos:15}")
    private long expiracionMinutos;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void crearUsuarioParaCliente(Cliente cliente, String passwordPlano, Integer rol) {
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreoCifrado(cryptoUtil.cifrar(cliente.getCorreo()));
        usuario.setCorreoHash(hashUtil.sha256(cliente.getCorreo()));
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setRol(rol != null ? rol : Roles.USUARIO_NORMAL);
        usuario.setSesionActiva(false);
        // datosBiometricos se deja NULL a proposito: aun no se usa, no es obligatorio.
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login - correoHash calculado");
        String correoHash = hashUtil.sha256(request.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreoHash(correoHash)
                // Mensaje generico a proposito: no revelar si el correo existe o no.
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o password incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o password incorrectos");
        }

        if (Boolean.FALSE.equals(usuario.getCliente().getActivo())) {
            throw new ValidacionException("El cliente esta dado de baja, no puede iniciar sesion");
        }

        String jwt = jwtUtil.generar(usuario.getCliente().getId(), correoHash, usuario.getRol());
        usuario.setJwtCifrado(cryptoUtil.cifrar(jwt));
        usuario.setSesionActiva(true);
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);

        LoginResponse response = new LoginResponse();
        response.setClienteId(usuario.getCliente().getId());
        response.setJwt(jwt);
        response.setRol(usuario.getRol());
        response.setExpiraEnSegundos(expiracionMinutos * 60);
        response.setCodigo(0);
        response.setMensaje("Login exitoso");
        log.info("Login exitoso - clienteId={}", usuario.getCliente().getId());
        return response;
    }

    @Override
    @Transactional
    public GenericResponse logout(String token) {
        GenericResponse response = new GenericResponse();
        try {
            Integer clienteId = jwtUtil.obtenerClienteId(token);
            usuarioRepository.findByCliente_Id(clienteId).ifPresent(usuario -> {
                usuario.setSesionActiva(false);
                usuario.setJwtCifrado(null);
                usuarioRepository.save(usuario);
            });
            response.setCodigo(0);
            response.setMensaje("Sesion cerrada correctamente");
        } catch (JwtException e) {
            log.warn("Logout con token invalido: {}", e.getMessage());
            response.setCodigo(1);
            response.setMensaje("Token invalido o ya expirado");
        }
        return response;
    }

    @Override
    @Transactional
    public void registrarActividad(Integer clienteId) {
        usuarioRepository.findByCliente_Id(clienteId).ifPresent(usuario -> {
            if (Boolean.TRUE.equals(usuario.getSesionActiva())) {
                usuario.setUltimaActividad(LocalDateTime.now());
                usuarioRepository.save(usuario);
            }
        });
    }
}
