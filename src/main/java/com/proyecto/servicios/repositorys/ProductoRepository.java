package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByIdProductoAndIdServicio(Integer idProducto, Integer idServicio);

    @Query("SELECT p FROM Producto p ORDER BY COALESCE(p.tipoFront, 0) ASC")
    List<Producto> findAllOrdenadosPorTipoFront();
}