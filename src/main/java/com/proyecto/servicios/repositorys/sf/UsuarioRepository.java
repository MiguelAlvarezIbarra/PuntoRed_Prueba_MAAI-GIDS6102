package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoHash(String correoHash);

    Optional<Usuario> findByCliente_Id(Integer clienteId);

    // Usado por el scheduler para cerrar sesiones inactivas
    List<Usuario> findBySesionActivaTrueAndUltimaActividadBefore(LocalDateTime limite);
}
