package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Integer> {

    List<TransaccionPago> findByNumeroCuentaOrderByFechaRegistroDesc(String numeroCuenta);
}
