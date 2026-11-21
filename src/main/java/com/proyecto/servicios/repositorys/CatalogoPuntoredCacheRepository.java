package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.CatalogoPuntoredCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoPuntoredCacheRepository extends JpaRepository<CatalogoPuntoredCache, String> {
}
