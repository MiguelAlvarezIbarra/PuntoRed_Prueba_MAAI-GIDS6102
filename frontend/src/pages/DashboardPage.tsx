import React, { useEffect, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { useAuth } from '../context/AuthContext';
import { api, ApiClientError } from '../services/apiClient';
import type {
  AppSection,
  CatalogoServicioItem,
  ClienteData,
  CuentaData,
} from '../types/api';
import { formatAccountNumber, formatCurrency } from '../utils/formatters';

interface DashboardPageProps {
  onNavigate: (section: AppSection, prefillCuenta?: string) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const { session, isAdmin, apiBaseUrl, clearLocalSession } = useAuth();
  const [clientes, setClientes] = useState<ClienteData[]>([]);
  const [cuentas, setCuentas] = useState<CuentaData[]>([]);
  const [servicios, setServicios] = useState<CatalogoServicioItem[]>([]);
  const [totalPagos, setTotalPagos] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const cargarResumen = async () => {
    setLoading(true);
    setAlert(null);
    try {
      const [cliRes, ctaRes, catRes, histRes] = await Promise.all([
        api.buscarClientes({}),
        api.obtenerMisCuentas().catch(() => ({ cuentas: [], codigo: 0, mensaje: '' })),
        api.buscarCatalogoPuntored({}).catch(() => ({ servicios: [], categorias: [], fuenteDatos: '', codigo: 0, mensaje: '' })),
        api.consultarHistorialPagos({}).catch(() => ({ total: 0, transacciones: [], codigo: 0, mensaje: '' })),
      ]);
      setClientes(cliRes.clientes || []);
      setCuentas(ctaRes.cuentas || []);
      setServicios(catRes.servicios || []);
      setTotalPagos(histRes.total ?? 0);
    } catch (err) {
      if (err instanceof ApiClientError && err.codigo === 9) {
        clearLocalSession();
        return;
      }
      setAlert({
        type: 'warning',
        title: 'No se pudo cargar todo el resumen',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Verifica la conexión con el backend.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void cargarResumen();
  }, []);

  const clientePrincipal = clientes[0];
  const cuentaPrincipal =
    cuentas.find((c) => c.numeroCuenta === clientePrincipal?.numeroCuenta) || cuentas[0];
  const saldoTotal = cuentas.reduce((sum, c) => sum + (c.saldo || 0), 0);

  return (
    <div className="d-flex flex-column gap-4">
      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      {/* Hero Ejecutivo */}
      <div className="pr-hero-card p-4">
        <div className="row align-items-center g-3">
          <div className="col-lg-8">
            <div className="d-flex align-items-center gap-2 mb-2 flex-wrap">
              <span className="badge pr-gold-pill rounded-pill px-3 py-1">
                {isAdmin ? 'Administrador del Sistema' : 'Cliente Verificado'}
              </span>
              <span className="badge bg-white bg-opacity-10 text-white rounded-pill px-3 py-1">
                ID Cliente #{session?.clienteId}
              </span>
            </div>
            <h3 className="fw-bold text-white mb-1">
              Bienvenido{clientePrincipal ? `, ${clientePrincipal.nombre} ${clientePrincipal.apellidoPaterno}` : ''}
            </h3>
            <p className="text-white-50 small mb-0">
              Conectado a <strong>{apiBaseUrl}</strong> · Sesión protegida con control de
              inactividad.
            </p>
          </div>
          <div className="col-lg-4 text-lg-end">
            <div className="small text-white-50 mb-1">
              {isAdmin ? 'Saldo Global en Cuentas Activas' : 'Tu Saldo Disponible'}
            </div>
            <div className="display-6 fw-bold text-white">
              {loading
                ? '...'
                : formatCurrency(isAdmin ? saldoTotal : cuentaPrincipal?.saldo ?? 0)}
            </div>
            {clientePrincipal?.numeroCuenta && (
              <div className="small text-white-50 pr-account-number mt-1">
                Cuenta: {formatAccountNumber(clientePrincipal.numeroCuenta)}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="row g-3">
        <div className="col-sm-6 col-xl-3">
          <div className="pr-card p-3 h-100 d-flex align-items-center gap-3">
            <span className="pr-kpi-icon bg-primary bg-opacity-10 text-primary">
              <i className="bi bi-people-fill" />
            </span>
            <div>
              <div className="text-body-secondary small">
                {isAdmin ? 'Clientes Visibles' : 'Mi Perfil'}
              </div>
              <div className="fs-4 fw-bold">{loading ? '—' : clientes.length}</div>
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div className="pr-card p-3 h-100 d-flex align-items-center gap-3">
            <span className="pr-kpi-icon bg-success bg-opacity-10 text-success">
              <i className="bi bi-credit-card-2-front-fill" />
            </span>
            <div>
              <div className="text-body-secondary small">Cuentas Activas</div>
              <div className="fs-4 fw-bold">{loading ? '—' : cuentas.length}</div>
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div className="pr-card p-3 h-100 d-flex align-items-center gap-3">
            <span className="pr-kpi-icon bg-warning bg-opacity-10 text-warning">
              <i className="bi bi-grid-3x3-gap-fill" />
            </span>
            <div>
              <div className="text-body-secondary small">Catálogo PuntoRed</div>
              <div className="fs-4 fw-bold">{loading ? '—' : servicios.length}</div>
            </div>
          </div>
        </div>

        <div className="col-sm-6 col-xl-3">
          <div className="pr-card p-3 h-100 d-flex align-items-center gap-3">
            <span className="pr-kpi-icon bg-info bg-opacity-10 text-info">
              <i className="bi bi-clock-history" />
            </span>
            <div>
              <div className="text-body-secondary small">Pagos Realizados</div>
              <div className="fs-4 fw-bold">{loading ? '—' : totalPagos}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Accesos Directos */}
      <div>
        <h5 className="fw-bold mb-3">Operaciones Principales</h5>
        <div className="row g-3">
          <div className="col-md-6 col-lg-3">
            <button
              type="button"
              className="pr-card p-3 w-100 text-start border-0 h-100 btn btn-light"
              onClick={() => onNavigate('pagos', clientePrincipal?.numeroCuenta || undefined)}
            >
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="badge text-bg-primary">PuntoRed + 2FA</span>
                <i className="bi bi-arrow-right-circle text-primary fs-5" />
              </div>
              <div className="fw-bold text-body mb-1">Pagar Servicio / Recarga</div>
              <div className="small text-body-secondary">
                Valida referencia, confirma resumen con PIN 2FA y recibe tu tirilla digital.
              </div>
            </button>
          </div>

          <div className="col-md-6 col-lg-3">
            <button
              type="button"
              className="pr-card p-3 w-100 text-start border-0 h-100 btn btn-light"
              onClick={() => onNavigate('cuentas', clientePrincipal?.numeroCuenta || undefined)}
            >
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="badge text-bg-success">Tesorería</span>
                <i className="bi bi-arrow-right-circle text-success fs-5" />
              </div>
              <div className="fw-bold text-body mb-1">Cuentas, Saldos y Abonos</div>
              <div className="small text-body-secondary">
                Consulta saldos de 18 dígitos y realiza abonos en línea.
              </div>
            </button>
          </div>

          <div className="col-md-6 col-lg-3">
            <button
              type="button"
              className="pr-card p-3 w-100 text-start border-0 h-100 btn btn-light"
              onClick={() => onNavigate('productos')}
            >
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="badge text-bg-warning">Catálogo</span>
                <i className="bi bi-arrow-right-circle text-warning fs-5" />
              </div>
              <div className="fw-bold text-body mb-1">Servicios PuntoRed</div>
              <div className="small text-body-secondary">
                Explora recargas, paquetes, recaudos y pines con logotipos reales.
              </div>
            </button>
          </div>

          <div className="col-md-6 col-lg-3">
            <button
              type="button"
              className="pr-card p-3 w-100 text-start border-0 h-100 btn btn-light"
              onClick={() => onNavigate('historial')}
            >
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="badge text-bg-info">Comprobantes</span>
                <i className="bi bi-arrow-right-circle text-info fs-5" />
              </div>
              <div className="fw-bold text-body mb-1">Historial de Pagos</div>
              <div className="small text-body-secondary">
                Revisa tus operaciones exitosas, rechazadas o pendientes y verifica folios.
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
