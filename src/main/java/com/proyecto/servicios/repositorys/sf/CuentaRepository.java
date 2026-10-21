package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByCliente_IdAndEstatus(Integer clienteId, String estatus);

    List<Cuenta> findByEstatus(String estatus);

    List<Cuenta> findByCliente_Id(Integer clienteId);

    @Modifying
    @Query("UPDATE Cuenta c SET c.estatus = :estatus WHERE c.cliente.id = :clienteId")
    int actualizarEstatusPorClienteId(@Param("clienteId") Integer clienteId, @Param("estatus") String estatus);
}
