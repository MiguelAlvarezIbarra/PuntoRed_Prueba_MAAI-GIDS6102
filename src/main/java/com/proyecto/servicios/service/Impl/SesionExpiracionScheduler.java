package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Revisa periodicamente las sesiones activas y apaga la bandera sesionActiva
 * (la pone en false) si el usuario lleva mas de N minutos sin actividad.
 */
@Component
@Slf4j
public class SesionExpiracionScheduler {

    private final UsuarioRepository usuarioRepository;

    @Value("${seguridad.sesion.inactividad-minutos:3}")
    private long minutosInactividad;

    public SesionExpiracionScheduler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Scheduled(fixedRateString = "${seguridad.sesion.revision-ms:30000}")
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(minutosInactividad);
        if (usuarioRepository.existsBySesionActivaTrueAndUltimaActividadBefore(limite)) {
            List<Usuario> inactivos = usuarioRepository.findBySesionActivaTrueAndUltimaActividadBefore(limite);
            for (Usuario usuario : inactivos) {
                usuario.setSesionActiva(false);
                usuario.setJwtCifrado(null);
            }
            usuarioRepository.saveAll(inactivos);
            log.info("Sesiones cerradas por inactividad: {}", inactivos.size());
        }
    }
}
