package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByIdProductoAndIdServicio(Integer idProducto, Integer idServicio);
    void deleteByIdProductoAndIdServicio(Integer idProducto, Integer idServicio);
}
