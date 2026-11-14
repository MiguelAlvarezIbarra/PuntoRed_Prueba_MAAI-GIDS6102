-- =====================================================================
-- V10: Soporte completo de catalogo visual (logos, descripciones, categorias)
--      y transacciones PuntoRed H2H V2 con estados diferenciados e historial.
-- =====================================================================

-- 1) Enriquecer catalogo local de productos con metadatos visuales
ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS logo_url TEXT,
    ADD COLUMN IF NOT EXISTS descripcion TEXT,
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(120),
    ADD COLUMN IF NOT EXISTS codigo_proveedor VARCHAR(60),
    ADD COLUMN IF NOT EXISTS tipo_operacion VARCHAR(30);

-- 2) Ampliar transacciones_pago para soportar operaciones Puntored H2H V2,
--    diferenciacion de estados (EXITOSA / RECHAZADA / PENDIENTE / NO_CONFIRMADA),
--    metadatos del servicio (nombre, categoria, logo) y ticket completo JSON.
ALTER TABLE transacciones_pago
    DROP CONSTRAINT IF EXISTS ck_transaccion_tipo_pago;

ALTER TABLE transacciones_pago
    ADD CONSTRAINT ck_transaccion_tipo_pago
        CHECK (tipo_pago IN ('TELEFONO', 'REFERENCIA', 'RECARGA', 'PAQUETE', 'RECAUDO', 'RECAUDO_V2', 'PIN'));

ALTER TABLE transacciones_pago
    ADD COLUMN IF NOT EXISTS nombre_servicio TEXT,
    ADD COLUMN IF NOT EXISTS nombre_producto TEXT,
    ADD COLUMN IF NOT EXISTS categoria TEXT,
    ADD COLUMN IF NOT EXISTS logo_url TEXT,
    ADD COLUMN IF NOT EXISTS referencia_secundaria TEXT,
    ADD COLUMN IF NOT EXISTS trace TEXT,
    ADD COLUMN IF NOT EXISTS estado TEXT NOT NULL DEFAULT 'EXITOSA',
    ADD COLUMN IF NOT EXISTS ticket_json TEXT,
    ADD COLUMN IF NOT EXISTS proveedor TEXT NOT NULL DEFAULT 'GESTOPAGO',
    ADD COLUMN IF NOT EXISTS codigo_producto_externo TEXT,
    ADD COLUMN IF NOT EXISTS codigo_convenio_externo TEXT;

ALTER TABLE transacciones_pago
    DROP CONSTRAINT IF EXISTS ck_transaccion_estado;

ALTER TABLE transacciones_pago
    ADD CONSTRAINT ck_transaccion_estado
        CHECK (estado IN ('EXITOSA', 'RECHAZADA', 'PENDIENTE', 'NO_CONFIRMADA'));

CREATE INDEX IF NOT EXISTS idx_transacciones_pago_cuenta_fecha
    ON transacciones_pago (cuenta_id, fecha_registro DESC);

CREATE INDEX IF NOT EXISTS idx_transacciones_pago_trace
    ON transacciones_pago (trace);
