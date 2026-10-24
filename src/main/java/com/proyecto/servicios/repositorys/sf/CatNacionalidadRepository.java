package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.CatNacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatNacionalidadRepository extends JpaRepository<CatNacionalidad, String> {

    boolean existsByNombre(String nombre);
}
