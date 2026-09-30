-- =====================================================================
-- Onboarding de Clientes Personas Fisicas - PostgreSQL
-- Campos de texto: TEXT (solicitado). La longitud se valida en la API y con CHECK.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CLIENTES
-- ---------------------------------------------------------------------
CREATE TABLE clientes (
    id                   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               TEXT           NOT NULL,
    segundo_nombre       TEXT,
    apellido_paterno     TEXT           NOT NULL,
    apellido_materno     TEXT           NOT NULL,
    fecha_nacimiento     DATE           NOT NULL,
    curp                 TEXT           NOT NULL,
    rfc                  TEXT           NOT NULL,
    sexo                 TEXT,
    nacionalidad         TEXT,
    estado_civil         TEXT,
    correo               TEXT           NOT NULL,
    telefono_movil       TEXT           NOT NULL,
    telefono_alternativo TEXT,
    ocupacion            TEXT,
    empresa              TEXT,
    ingreso_mensual      NUMERIC(14,2)  NOT NULL,
    activo               BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_registro       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_clientes_curp   UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    CONSTRAINT ck_clientes_curp_len  CHECK (char_length(curp) = 18),
    CONSTRAINT ck_clientes_rfc_len   CHECK (char_length(rfc) IN (12, 13)),
    CONSTRAINT ck_clientes_correo_len CHECK (char_length(correo) <= 100),
    CONSTRAINT ck_clientes_tel_movil CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_tel_alt   CHECK (telefono_alternativo IS NULL OR telefono_alternativo ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_ingreso   CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_nombre_len CHECK (char_length(nombre) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_ap_len    CHECK (char_length(apellido_paterno) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_am_len    CHECK (char_length(apellido_materno) BETWEEN 2 AND 50)
);

-- curp, rfc y correo ya tienen indice por su UNIQUE. Indices extra para las consultas pedidas:
CREATE INDEX idx_clientes_activo         ON clientes (activo);
CREATE INDEX idx_clientes_fecha_registro ON clientes (fecha_registro);

-- ---------------------------------------------------------------------
-- DOMICILIOS (1:1 con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE domicilios (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id      INTEGER NOT NULL,
    calle           TEXT    NOT NULL,
    numero_exterior TEXT    NOT NULL,
    numero_interior TEXT,
    colonia         TEXT    NOT NULL,
    municipio       TEXT    NOT NULL,
    estado          TEXT    NOT NULL,
    codigo_postal   TEXT    NOT NULL,
    pais            TEXT    NOT NULL,

    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_domicilio_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_domicilios_cp      CHECK (codigo_postal ~ '^[0-9]{5}$')
);

-- ---------------------------------------------------------------------
-- CUENTAS (N:1 con clientes)
-- ---------------------------------------------------------------------
CREATE TABLE cuentas (
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id     INTEGER       NOT NULL,
    numero_cuenta  TEXT          NOT NULL,
    saldo          NUMERIC(14,2) NOT NULL DEFAULT 0,
    estatus        TEXT          NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_cuentas_saldo   CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA'))
);

CREATE INDEX idx_cuentas_cliente_id ON cuentas (cliente_id);
CREATE INDEX idx_cuentas_estatus    ON cuentas (estatus);

-- ---------------------------------------------------------------------
-- USUARIOS (seguridad: credenciales, jwt, sesion, biometria) 1:1 con clientes
-- correo_cifrado / jwt_cifrado: AES-256-GCM (base64). password_hash: BCrypt.
-- correo_hash: SHA-256 para buscar sin descifrar.
-- datos_biometricos: score de similitud facial (MediaPipe), OPCIONAL (NULL).
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id                INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id        INTEGER          NOT NULL,
    correo_cifrado    TEXT             NOT NULL,
    correo_hash       TEXT             NOT NULL,
    password_hash     TEXT             NOT NULL,
    jwt_cifrado       TEXT,
    datos_biometricos DOUBLE PRECISION,
    sesion_activa     BOOLEAN          NOT NULL DEFAULT FALSE,
    ultima_actividad  TIMESTAMP,

    CONSTRAINT uq_usuarios_cliente     UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_correo_hash UNIQUE (correo_hash),
    CONSTRAINT fk_usuario_cliente      FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_usuarios_biometria   CHECK (datos_biometricos IS NULL OR datos_biometricos BETWEEN 0 AND 1)
);

-- Indice parcial: el scheduler solo consulta sesiones activas
CREATE INDEX idx_usuarios_sesion_activa ON usuarios (ultima_actividad) WHERE sesion_activa = TRUE;
