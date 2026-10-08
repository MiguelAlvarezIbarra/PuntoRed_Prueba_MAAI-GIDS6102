-- Refuerza en base de datos lo que ya valida la API (defensa en profundidad):
-- 1) nombre, apellido_paterno y apellido_materno ahora piden minimo 3 caracteres (antes 2).
-- 2) sexo solo acepta 'H' o 'M'.
-- 3) estado_civil solo acepta un catalogo cerrado de 5 valores.

-- Update existing invalid data to satisfy the new constraints
UPDATE clientes SET nombre = RPAD(nombre, 3, 'x') WHERE char_length(nombre) < 3;
UPDATE clientes SET nombre = SUBSTRING(nombre, 1, 50) WHERE char_length(nombre) > 50;
UPDATE clientes SET apellido_paterno = RPAD(apellido_paterno, 3, 'x') WHERE char_length(apellido_paterno) < 3;
UPDATE clientes SET apellido_paterno = SUBSTRING(apellido_paterno, 1, 50) WHERE char_length(apellido_paterno) > 50;
UPDATE clientes SET apellido_materno = RPAD(apellido_materno, 3, 'x') WHERE char_length(apellido_materno) < 3;
UPDATE clientes SET apellido_materno = SUBSTRING(apellido_materno, 1, 50) WHERE char_length(apellido_materno) > 50;
UPDATE clientes SET sexo = 'H' WHERE sexo NOT IN ('H', 'M') AND sexo IS NOT NULL;
UPDATE clientes SET estado_civil = 'Soltero' WHERE estado_civil NOT IN ('Soltero', 'Casado', 'Divorciado', 'Viudo', 'Union Libre') AND estado_civil IS NOT NULL;

ALTER TABLE clientes DROP CONSTRAINT ck_clientes_nombre_len;
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_nombre_len CHECK (char_length(nombre) BETWEEN 3 AND 50);

ALTER TABLE clientes DROP CONSTRAINT ck_clientes_ap_len;
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_ap_len CHECK (char_length(apellido_paterno) BETWEEN 3 AND 50);

ALTER TABLE clientes DROP CONSTRAINT ck_clientes_am_len;
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_am_len CHECK (char_length(apellido_materno) BETWEEN 3 AND 50);

ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_sexo CHECK (sexo IS NULL OR sexo IN ('H', 'M'));

ALTER TABLE clientes
    ADD CONSTRAINT ck_clientes_estado_civil
    CHECK (estado_civil IS NULL OR estado_civil IN ('Soltero', 'Casado', 'Divorciado', 'Viudo', 'Union Libre'));
