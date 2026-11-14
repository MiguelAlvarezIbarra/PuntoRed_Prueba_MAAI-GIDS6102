package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Integer> {

    List<TransaccionPago> findByNumeroCuentaOrderByFechaRegistroDesc(String numeroCuenta);

    List<TransaccionPago> findByCuenta_Cliente_IdOrderByFechaRegistroDesc(Integer clienteId);

    List<TransaccionPago> findAllByOrderByFechaRegistroDesc();

    Optional<TransaccionPago> findByIdAndCuenta_Cliente_Id(Integer id, Integer clienteId);

    Optional<TransaccionPago> findByTrace(String trace);
}
