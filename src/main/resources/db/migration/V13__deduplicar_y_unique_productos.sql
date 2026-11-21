-- =====================================================================
-- V13: Deduplicar catalogo de productos y garantizar unicidad compuesta
--      por (id_producto, id_servicio) para prevenir NonUniqueResultException
--      ante sincronizaciones concurrentes.
-- =====================================================================

WITH duplicados AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY id_producto, id_servicio
               ORDER BY fecha_actualizacion DESC NULLS LAST, id DESC
           ) AS rn
    FROM public.productos
)
DELETE FROM public.productos p
USING duplicados d
WHERE p.id = d.id
  AND d.rn > 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_productos_id_producto_id_servicio'
          AND conrelid = 'public.productos'::regclass
    ) THEN
        ALTER TABLE public.productos
            ADD CONSTRAINT uq_productos_id_producto_id_servicio
                UNIQUE (id_producto, id_servicio);
    END IF;
END $$;
