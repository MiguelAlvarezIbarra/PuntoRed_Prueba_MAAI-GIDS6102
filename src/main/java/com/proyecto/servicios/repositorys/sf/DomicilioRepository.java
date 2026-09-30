package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Integer> {

    Optional<Domicilio> findByCliente_Id(Integer clienteId);
}
