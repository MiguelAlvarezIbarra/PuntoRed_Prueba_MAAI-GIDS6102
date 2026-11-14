import React, { useEffect, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { api, ApiClientError } from '../services/apiClient';
import type { CuentaData, SaldoResponse } from '../types/api';
import { formatAccountNumber, formatCurrency, formatDateTime } from '../utils/formatters';

interface CuentasPageProps {
  prefillCuenta?: string;
  onGoToPago: (numeroCuenta: string) => void;
}

export const CuentasPage: React.FC<CuentasPageProps> = ({ prefillCuenta, onGoToPago }) => {
  const [cuentasActivas, setCuentasActivas] = useState<CuentaData[]>([]);
  const [numeroCuenta, setNumeroCuenta] = useState(prefillCuenta || '');
  const [montoAbono, setMontoAbono] = useState('');

  const [cuentaDetalle, setCuentaDetalle] = useState<CuentaData | null>(null);
  const [saldoActual, setSaldoActual] = useState<SaldoResponse | null>(null);

  const [loadingLista, setLoadingLista] = useState(false);
  const [loadingAction, setLoadingAction] = useState(false);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  useEffect(() => {
    if (prefillCuenta) {
      setNumeroCuenta(prefillCuenta);
    }
  }, [prefillCuenta]);

  const cargarActivas = async () => {
    setLoadingLista(true);
    try {
      const res = await api.obtenerMisCuentas();
      setCuentasActivas(res.cuentas || []);
      if (!numeroCuenta && res.cuentas?.length) {
        setNumeroCuenta(res.cuentas[0].numeroCuenta);
      }
    } catch (err) {
      setAlert({
        type: 'warning',
        title: 'No se pudo cargar el listado de cuentas activas',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error de red.',
      });
    } finally {
      setLoadingLista(false);
    }
  };

  useEffect(() => {
    void cargarActivas();
  }, []);

  const handleConsultarCuenta = async (targetCuenta?: string) => {
    const cuentaTrim = (targetCuenta ?? numeroCuenta).trim();
    if (!/^\d{18}$/.test(cuentaTrim)) {
      setAlert({
        type: 'warning',
        title: 'Formato de cuenta inválido',
        codigo: 4,
        message: 'El número de cuenta debe contener exactamente 18 dígitos numéricos.',
      });
      return;
    }

    setLoadingAction(true);
    setAlert(null);
    try {
      const [detRes, saldoRes] = await Promise.all([
        api.consultarCuenta({ numeroCuenta: cuentaTrim }),
        api.consultarSaldo({ numeroCuenta: cuentaTrim }),
      ]);
      setCuentaDetalle(detRes.cuenta);
      setSaldoActual(saldoRes);
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error al consultar la cuenta',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Cuenta no encontrada.',
      });
    } finally {
      setLoadingAction(false);
    }
  };

  const handleAbonar = async (e: React.FormEvent) => {
    e.preventDefault();
    const cuentaTrim = numeroCuenta.trim();
    if (!/^\d{18}$/.test(cuentaTrim)) {
      setAlert({
        type: 'warning',
        title: 'Cuenta requerida',
        codigo: 4,
        message: 'Ingresa un número de cuenta de exactamente 18 dígitos.',
      });
      return;
    }

    const monto = Number.parseFloat(montoAbono);
    if (Number.isNaN(monto) || monto <= 0) {
      setAlert({
        type: 'warning',
        title: 'Monto inválido',
        codigo: 2,
        message: 'El monto a abonar debe ser mayor a cero.',
      });
      return;
    }

    setLoadingAction(true);
    setAlert(null);
    try {
      const res = await api.abonarSaldo({
        numeroCuenta: cuentaTrim,
        monto: Number(monto.toFixed(2)),
      });
      setSaldoActual(res);
      setMontoAbono('');
      setAlert({
        type: 'success',
        title: 'Abono aplicado correctamente',
        message: `Nuevo saldo en cuenta ${formatAccountNumber(res.numeroCuenta)}: ${formatCurrency(res.saldo)}`,
      });
      await cargarActivas();
      await handleConsultarCuenta(cuentaTrim);
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo aplicar el abono',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error al abonar.',
      });
    } finally {
      setLoadingAction(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
          <h4 className="fw-bold mb-1">Cuentas, Saldos y Abonos</h4>
          <p className="text-body-secondary small mb-0">
            Operaciones sobre cuentas bancarias de 18 dígitos (<code>/cuentas/*</code>).
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          onClick={() => void cargarActivas()}
          disabled={loadingLista}
        >
          <i className="bi bi-arrow-clockwise me-1" /> Actualizar Cuentas
        </button>
      </div>

      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      <div className="row g-4">
        {/* Panel Consulta y Abono */}
        <div className="col-lg-5 d-flex flex-column gap-3">
          <div className="pr-card p-3 p-md-4">
            <h6 className="fw-bold mb-3">
              <i className="bi bi-search me-2 text-primary" />
              Consultar Cuenta / Saldo
            </h6>
            <label className="form-label small fw-semibold">Número de Cuenta (18 dígitos)</label>
            <div className="input-group mb-3">
              <input
                type="text"
                className="form-control pr-account-number"
                placeholder="000000000000000000"
                value={numeroCuenta}
                onChange={(e) => setNumeroCuenta(e.target.value.replace(/\D/g, '').slice(0, 18))}
              />
              <button
                type="button"
                className="btn btn-primary"
                disabled={loadingAction}
                onClick={() => void handleConsultarCuenta()}
              >
                Consultar
              </button>
            </div>

            {saldoActual && (
              <div className="pr-hero-card p-3 mt-2">
                <div className="d-flex justify-content-between align-items-center">
                  <span className="small text-white-50">Saldo Actualizado</span>
                  {cuentaDetalle && (
                    <span
                      className={`badge ${
                        cuentaDetalle.estatus === 'ACTIVA' ? 'text-bg-success' : 'text-bg-danger'
                      }`}
                    >
                      {cuentaDetalle.estatus}
                    </span>
                  )}
                </div>
                <div className="fs-2 fw-bold text-white my-1">
                  {formatCurrency(saldoActual.saldo)}
                </div>
                <div className="small text-white-50 pr-account-number">
                  {formatAccountNumber(saldoActual.numeroCuenta)}
                </div>
                {cuentaDetalle && (
                  <div className="small text-white-50 mt-2 pt-2 border-top border-white border-opacity-10 d-flex justify-content-between">
                    <span>Cliente ID: #{cuentaDetalle.clienteId}</span>
                    <span>Apertura: {formatDateTime(cuentaDetalle.fechaApertura)}</span>
                  </div>
                )}
              </div>
            )}
          </div>

          <form className="pr-card p-3 p-md-4" onSubmit={handleAbonar}>
            <h6 className="fw-bold mb-3">
              <i className="bi bi-plus-circle-fill me-2 text-success" />
              Abonar Saldo a Cuenta
            </h6>
            <div className="mb-3">
              <label className="form-label small fw-semibold">Monto a Abonar (MXN)</label>
              <div className="input-group">
                <span className="input-group-text">$</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-control"
                  placeholder="250.00"
                  value={montoAbono}
                  onChange={(e) => setMontoAbono(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-success flex-grow-1" disabled={loadingAction}>
                <i className="bi bi-cash-stack me-2" />
                Aplicar Abono
              </button>
              {/^\d{18}$/.test(numeroCuenta) && (
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  onClick={() => onGoToPago(numeroCuenta)}
                >
                  Ir a Pagar
                </button>
              )}
            </div>
          </form>
        </div>

        {/* Lista de Cuentas Activas */}
        <div className="col-lg-7">
          <div className="pr-card p-3 p-md-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">
                <i className="bi bi-list-check me-2 text-primary" />
                Cuentas Activas Registradas ({cuentasActivas.length})
              </h6>
            </div>

            {loadingLista ? (
              <div className="text-center py-4 text-body-secondary">
                <span className="spinner-border spinner-border-sm me-2" />
                Cargando cuentas...
              </div>
            ) : cuentasActivas.length === 0 ? (
              <div className="text-center py-4 text-body-secondary">
                No hay cuentas activas disponibles.
              </div>
            ) : (
              <div className="d-flex flex-column gap-2">
                {cuentasActivas.map((cta) => (
                  <div
                    key={cta.id}
                    className={`pr-card-elevated p-3 d-flex justify-content-between align-items-center flex-wrap gap-2 ${
                      numeroCuenta === cta.numeroCuenta ? 'border-primary' : ''
                    }`}
                  >
                    <div>
                      <div className="fw-bold pr-account-number">
                        {formatAccountNumber(cta.numeroCuenta)}
                      </div>
                      <small className="text-body-secondary">
                        Titular ID #{cta.clienteId} · Apertura {formatDateTime(cta.fechaApertura)}
                      </small>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                      <div className="text-end">
                        <div className="fw-bold text-success">{formatCurrency(cta.saldo)}</div>
                        <span className="badge text-bg-success">{cta.estatus}</span>
                      </div>
                      <div className="btn-group btn-group-sm">
                        <button
                          type="button"
                          className="btn btn-outline-primary"
                          onClick={() => {
                            setNumeroCuenta(cta.numeroCuenta);
                            void handleConsultarCuenta(cta.numeroCuenta);
                          }}
                        >
                          Seleccionar
                        </button>
                        <button
                          type="button"
                          className="btn btn-primary"
                          onClick={() => onGoToPago(cta.numeroCuenta)}
                        >
                          Pagar
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
