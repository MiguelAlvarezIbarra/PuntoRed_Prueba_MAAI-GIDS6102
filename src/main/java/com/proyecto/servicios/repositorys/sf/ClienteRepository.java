package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(1) FROM cat_paises WHERE nombre = :pais", nativeQuery = true)
    int countPais(@org.springframework.data.repository.query.Param("pais") String pais);

    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(1) FROM cat_nacionalidades WHERE nombre = :nacionalidad", nativeQuery = true)
    int countNacionalidad(@org.springframework.data.repository.query.Param("nacionalidad") String nacionalidad);
}
