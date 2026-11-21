-- =====================================================================
-- V12: Persistencia del catalogo sincronizado en PostgreSQL para evitar
--      consultas repetidas a las APIs externas en cada arranque/peticion.
-- =====================================================================

CREATE TABLE IF NOT EXISTS catalogo_puntored_cache (
    clave            TEXT PRIMARY KEY,
    fuente_datos     TEXT NOT NULL,
    saldo_comercio   TEXT,
    total_servicios  INTEGER NOT NULL DEFAULT 0,
    payload_json     TEXT NOT NULL,
    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE IF EXISTS catalogo_puntored_cache ENABLE ROW LEVEL SECURITY;
