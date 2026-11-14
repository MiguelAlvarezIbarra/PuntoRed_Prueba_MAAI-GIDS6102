-- =====================================================================
-- V11: Defensa en profundidad (RLS) sobre tablas en schema public.
-- Impide exposicion inadvertida via PostgREST / Data API en Supabase
-- cuando no existen politicas publicas; la conexion backend con rol
-- propietario / BYPASSRLS (postgres) conserva acceso completo.
-- =====================================================================

ALTER TABLE IF EXISTS gestopago_tokens   ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS productos          ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS clientes           ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS domicilios         ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS cuentas            ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS usuarios           ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS cat_paises         ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS cat_nacionalidades ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS transacciones_pago ENABLE ROW LEVEL SECURITY;
