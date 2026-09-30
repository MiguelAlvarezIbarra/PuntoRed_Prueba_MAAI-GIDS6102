-- Agrega el rol de acceso: 1 = administrador, 2 = usuario normal (cliente).
-- Todo registro nuevo por defecto es 2; solo un admin puede crear otro rol=1.

ALTER TABLE usuarios
    ADD COLUMN rol SMALLINT NOT NULL DEFAULT 2;

ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_rol CHECK (rol IN (1, 2));

CREATE INDEX idx_usuarios_rol ON usuarios (rol);
