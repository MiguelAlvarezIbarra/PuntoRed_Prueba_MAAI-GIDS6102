package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    boolean existsByRfcAndActivoTrue(String rfc);

    boolean existsByRfcAndActivoFalse(String rfc);

    // Búsqueda de filtros desde Spring Data JPA
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    List<Cliente> findByRfcContainingIgnoreCase(String rfc);

    List<Cliente> findByCurpContainingIgnoreCase(String curp);

    List<Cliente> findByCorreoContainingIgnoreCase(String correo);

    List<Cliente> findByActivo(Boolean activo);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    // Filtros booleanos optimizados (SELECT EXISTS) para validación de catálogos
    @Query(value = "SELECT EXISTS(SELECT 1 FROM cat_paises WHERE nombre = :pais)", nativeQuery = true)
    boolean existsByPais(@Param("pais") String pais);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM cat_nacionalidades WHERE nombre = :nacionalidad)", nativeQuery = true)
    boolean existsByNacionalidad(@Param("nacionalidad") String nacionalidad);
}
