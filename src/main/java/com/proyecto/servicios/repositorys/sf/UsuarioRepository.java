package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoHash(String correoHash);

    Optional<Usuario> findByCliente_Id(Integer clienteId);

    // Consulta booleana rapida para saber si existen sesiones inactivas
    boolean existsBySesionActivaTrueAndUltimaActividadBefore(LocalDateTime limite);

    List<Usuario> findBySesionActivaTrueAndUltimaActividadBefore(LocalDateTime limite);

    // Actualizacion masiva para cerrar sesiones sin ciclos iterativos lentos
    @Modifying
    @Query("UPDATE Usuario u SET u.sesionActiva = false, u.jwtCifrado = null WHERE u.sesionActiva = true AND u.ultimaActividad < :limite")
    int cerrarSesionesInactivas(@Param("limite") LocalDateTime limite);
}
