-- =====================================================================
-- Se elimina el modulo de personas (ya no se usa y exponia datos sin
-- proteccion). La migracion V2 se conserva porque Flyway ya la aplico
-- y su historial debe coincidir; esta migracion borra la tabla.
-- =====================================================================
DROP TABLE IF EXISTS personas;
