package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Revisa periodicamente las sesiones activas y apaga la bandera sesionActiva
 * (la pone en false) si el usuario lleva mas de N minutos sin actividad.
 * Por requerimiento del proyecto, N = 3 minutos.
 * Utiliza comprobacion booleana y actualizacion masiva para evitar iteraciones lentas.
 */
@Component
@Slf4j
public class SesionExpiracionScheduler {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${seguridad.sesion.inactividad-minutos:3}")
    private long minutosInactividad;

    // Corre cada 30 segundos a revisar si alguien lleva mas de 3 min inactivo.
    @Transactional
    @Scheduled(fixedRateString = "${seguridad.sesion.revision-ms:30000}")
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosInactividad);
        if (usuarioRepository.existsBySesionActivaTrueAndUltimaActividadBefore(limite)) {
            int totalCerradas = usuarioRepository.cerrarSesionesInactivas(limite);
            log.info("Sesiones cerradas por inactividad: {}", totalCerradas);
        }
    }
}
