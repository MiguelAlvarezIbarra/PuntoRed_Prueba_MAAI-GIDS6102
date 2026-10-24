package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.CatPais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatPaisRepository extends JpaRepository<CatPais, String> {

    boolean existsByNombre(String nombre);
}
