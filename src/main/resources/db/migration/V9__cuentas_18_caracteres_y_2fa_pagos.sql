-- =====================================================================
-- V9: Numero de cuenta de 18 caracteres, soporte 2FA por correo en usuarios
--     y tabla de transacciones de pago (GestoPago sendTx.do).
-- =====================================================================

-- 1) Ajustar cuentas existentes a exactamente 18 digitos numericos
UPDATE cuentas
SET numero_cuenta = RPAD(REGEXP_REPLACE(numero_cuenta, '[^0-9]', '', 'g'), 18, '0')
WHERE char_length(numero_cuenta) < 18;

UPDATE cuentas
SET numero_cuenta = SUBSTRING(REGEXP_REPLACE(numero_cuenta, '[^0-9]', '', 'g'), 1, 18)
WHERE char_length(numero_cuenta) > 18;

ALTER TABLE cuentas
    DROP CONSTRAINT IF EXISTS ck_cuentas_numero_len;

ALTER TABLE cuentas
    ADD CONSTRAINT ck_cuentas_numero_len CHECK (numero_cuenta ~ '^[0-9]{18}$');

-- 2) Columnas para PIN de autenticacion 2FA en la tabla usuarios
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS pin_2fa_hash TEXT,
    ADD COLUMN IF NOT EXISTS pin_2fa_expiracion TIMESTAMP;

-- 3) Tabla de transacciones de pago (GestoPago SenTx)
CREATE TABLE IF NOT EXISTS transacciones_pago (
    id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuenta_id           INTEGER       NOT NULL,
    numero_cuenta       TEXT          NOT NULL,
    id_servicio         INTEGER       NOT NULL,
    id_producto         INTEGER       NOT NULL,
    tipo_pago           TEXT          NOT NULL,
    telefono_referencia TEXT          NOT NULL,
    monto               NUMERIC(14,2) NOT NULL,
    upc                 TEXT          NOT NULL,
    unidad              TEXT          NOT NULL,
    hora_local          TEXT          NOT NULL,
    sentx_cifrado       TEXT          NOT NULL,
    id_tx_gestopago     TEXT,
    num_autorizacion    TEXT,
    codigo_respuesta    TEXT          NOT NULL,
    mensaje_respuesta   TEXT          NOT NULL,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaccion_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT ck_transaccion_cuenta_len CHECK (numero_cuenta ~ '^[0-9]{18}$'),
    CONSTRAINT ck_transaccion_monto CHECK (monto > 0),
    CONSTRAINT ck_transaccion_tipo_pago CHECK (tipo_pago IN ('TELEFONO', 'REFERENCIA'))
);

CREATE INDEX IF NOT EXISTS idx_transacciones_pago_cuenta ON transacciones_pago (numero_cuenta);
