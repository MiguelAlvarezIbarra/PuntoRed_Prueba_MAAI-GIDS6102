import React, { useCallback, useEffect, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { ServiceLogo } from '../components/ServiceLogo';
import { useAuth } from '../context/AuthContext';
import { api, ApiClientError } from '../services/apiClient';
import type { HistorialPagoItem } from '../types/api';
import { formatAccountNumber, formatCurrency, formatDateTime } from '../utils/formatters';

function getEstadoBadge(estado: string): { cls: string; label: string; icon: string } {
  switch (estado) {
    case 'EXITOSA':
      return { cls: 'text-bg-success', label: 'Exitosa', icon: 'bi-check-circle-fill' };
    case 'RECHAZADA':
      return { cls: 'text-bg-danger', label: 'Rechazada', icon: 'bi-x-circle-fill' };
    case 'NO_CONFIRMADA':
      return { cls: 'text-bg-warning', label: 'No Confirmada', icon: 'bi-exclamation-triangle-fill' };
    default:
      return { cls: 'text-bg-warning', label: 'Pendiente', icon: 'bi-hourglass-split' };
  }
}

export const HistorialPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [items, setItems] = useState<HistorialPagoItem[]>([]);
  const [estadoFiltro, setEstadoFiltro] = useState('');
  const [tipoFiltro, setTipoFiltro] = useState('');
  const [cuentaFiltro, setCuentaFiltro] = useState('');

  const [selectedItem, setSelectedItem] = useState<HistorialPagoItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [checkingDetail, setCheckingDetail] = useState(false);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const cargarHistorial = useCallback(async () => {
    setLoading(true);
    setAlert(null);
    try {
      const res = await api.consultarHistorialPagos({
        estado: estadoFiltro || undefined,
        tipoOperacion: tipoFiltro || undefined,
        numeroCuenta: cuentaFiltro.trim() || undefined,
      });
      setItems(res.transacciones || []);
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo cargar el historial de pagos',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Verifica la conexión.',
      });
    } finally {
      setLoading(false);
    }
  }, [estadoFiltro, tipoFiltro, cuentaFiltro]);

  useEffect(() => {
    void cargarHistorial();
  }, [cargarHistorial]);

  const handleVerDetalle = async (item: HistorialPagoItem, consultarProveedor = false) => {
    setCheckingDetail(true);
    setSelectedItem(item);
    try {
      const det = await api.consultarDetallePago({
        idTransaccion: item.id,
        consultarEstadoProveedor: consultarProveedor,
      });
      setSelectedItem(det.transaccion);
      if (det.estadoActualizado) {
        setAlert({
          type: det.transaccion.estado === 'EXITOSA' ? 'success' : 'warning',
          title: 'Estado actualizado desde PuntoRed',
          message: det.mensaje,
        });
        await cargarHistorial();
      }
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error consultando detalle',
        message: err instanceof Error ? err.message : 'No fue posible consultar el detalle.',
      });
    } finally {
      setCheckingDetail(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-4">
      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      <div className="pr-card p-4">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
          <div>
            <h5 className="fw-bold mb-1">
              <i className="bi bi-clock-history text-primary me-2" />
              Historial de Pagos y Comprobantes
            </h5>
            <p className="text-body-secondary small mb-0">
              {isAdmin
                ? 'Vista administrativa de todas las operaciones registradas.'
                : 'Mostrando exclusivamente las transacciones asociadas a tus cuentas.'}
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            disabled={loading}
            onClick={() => void cargarHistorial()}
          >
            <i className="bi bi-arrow-clockwise me-1" />
            Actualizar
          </button>
        </div>

        <div className="row g-2">
          <div className="col-md-4">
            <select
              className="form-select form-select-sm"
              value={estadoFiltro}
              onChange={(e) => setEstadoFiltro(e.target.value)}
            >
              <option value="">Todos los estados</option>
              <option value="EXITOSA">Exitosas</option>
              <option value="RECHAZADA">Rechazadas</option>
              <option value="PENDIENTE">Pendientes</option>
              <option value="NO_CONFIRMADA">No Confirmadas</option>
            </select>
          </div>
          <div className="col-md-4">
            <select
              className="form-select form-select-sm"
              value={tipoFiltro}
              onChange={(e) => setTipoFiltro(e.target.value)}
            >
              <option value="">Todos los tipos de servicio</option>
              <option value="RECARGA">Recargas</option>
              <option value="PAQUETE">Paquetes</option>
              <option value="RECAUDO">Recaudos V1</option>
              <option value="RECAUDO_V2">Recaudos V2</option>
              <option value="PIN">Pines Digitales</option>
            </select>
          </div>
          <div className="col-md-4">
            <input
              type="text"
              className="form-control form-control-sm font-monospace"
              placeholder="Filtrar por cuenta (18 dígitos)..."
              value={cuentaFiltro}
              onChange={(e) => setCuentaFiltro(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="pr-card p-0 overflow-hidden">
        {loading ? (
          <div className="p-5 text-center">
            <div className="spinner-border text-primary mb-2" />
            <div className="small text-body-secondary">Consultando operaciones...</div>
          </div>
        ) : items.length === 0 ? (
          <div className="p-5 text-center text-body-secondary">
            <i className="bi bi-receipt-cutoff fs-1 d-block mb-2" />
            Aún no hay pagos registrados para tu usuario con estos filtros.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Servicio</th>
                  <th>Fecha</th>
                  <th>Referencia</th>
                  <th>Folio / Auth</th>
                  <th className="text-end">Importe</th>
                  <th>Estado</th>
                  <th className="text-end">Acción</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => {
                  const badge = getEstadoBadge(item.estado);
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <ServiceLogo
                            logoUrl={item.logoUrl}
                            name={item.nombreServicio}
                            tipoOperacion={item.tipoOperacion}
                            size={38}
                          />
                          <div>
                            <div className="fw-semibold">{item.nombreProducto || item.nombreServicio}</div>
                            <div className="small text-body-secondary">
                              {item.nombreServicio} · {item.tipoOperacion}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="small">{formatDateTime(item.fechaRegistro)}</td>
                      <td className="font-monospace small">{item.referencia1}</td>
                      <td className="small font-monospace">
                        <div>{item.idTransaccionProveedor || item.trace || '—'}</div>
                        {item.codigoAutorizacion && (
                          <div className="text-body-secondary">Auth: {item.codigoAutorizacion}</div>
                        )}
                      </td>
                      <td className="text-end fw-bold">{formatCurrency(item.monto)}</td>
                      <td>
                        <span className={`badge ${badge.cls}`}>
                          <i className={`bi ${badge.icon} me-1`} />
                          {badge.label}
                        </span>
                      </td>
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary"
                          onClick={() => void handleVerDetalle(item, false)}
                        >
                          Detalle
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal de detalle del pago */}
      {selectedItem && (
        <div
          className="modal d-block show"
          style={{ backgroundColor: 'rgba(15, 23, 42, 0.6)' }}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title fw-bold">
                  Detalle de Transacción #{selectedItem.id}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedItem(null)}
                />
              </div>
              <div className="modal-body">
                <div className="d-flex align-items-center gap-3 p-3 rounded-3 bg-body-tertiary mb-3">
                  <ServiceLogo
                    logoUrl={selectedItem.ticket?.logoUrl || selectedItem.logoUrl}
                    name={selectedItem.nombreServicio}
                    tipoOperacion={selectedItem.tipoOperacion}
                    size={54}
                  />
                  <div className="flex-grow-1">
                    <h6 className="fw-bold mb-0">
                      {selectedItem.nombreProducto || selectedItem.nombreServicio}
                    </h6>
                    <div className="small text-body-secondary">
                      {selectedItem.nombreServicio} · {selectedItem.categoria || selectedItem.tipoOperacion}
                    </div>
                  </div>
                  <div className="text-end">
                    <div className="fs-4 fw-bold text-primary">
                      {formatCurrency(selectedItem.monto)}
                    </div>
                    <span className={`badge ${getEstadoBadge(selectedItem.estado).cls}`}>
                      {getEstadoBadge(selectedItem.estado).label}
                    </span>
                  </div>
                </div>

                <div className="row g-2 small mb-3">
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Cuenta cargo: </span>
                    <strong className="font-monospace">
                      {formatAccountNumber(selectedItem.numeroCuenta)}
                    </strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Fecha: </span>
                    <strong>{formatDateTime(selectedItem.fechaRegistro)}</strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Referencia: </span>
                    <strong className="font-monospace">{selectedItem.referencia1}</strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Folio Proveedor: </span>
                    <strong className="font-monospace">
                      {selectedItem.idTransaccionProveedor || 'Sin asignar'}
                    </strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Autorización: </span>
                    <strong className="font-monospace">
                      {selectedItem.codigoAutorizacion || '—'}
                    </strong>
                  </div>
                  <div className="col-sm-6">
                    <span className="text-body-secondary">Trace: </span>
                    <strong className="font-monospace">{selectedItem.trace || '—'}</strong>
                  </div>
                  <div className="col-12">
                    <span className="text-body-secondary">Respuesta ({selectedItem.codigoRespuesta}): </span>
                    <span>{selectedItem.mensajeRespuesta}</span>
                  </div>
                </div>

                {selectedItem.ticket && (
                  <div className="p-3 rounded-3 border bg-body-tertiary small">
                    <div className="text-center fw-bold mb-2">
                      {selectedItem.ticket.titulo || 'TIRILLA DIGITAL PUNTORED'}
                    </div>
                    {selectedItem.ticket.cuerpo &&
                      Object.entries(selectedItem.ticket.cuerpo).map(([k, v]) => (
                        <div key={k} className="d-flex justify-content-between">
                          <span className="text-body-secondary">{k}:</span>
                          <strong>{String(v ?? '')}</strong>
                        </div>
                      ))}
                    {selectedItem.ticket.piePagina && (
                      <div className="text-center text-body-secondary border-top mt-2 pt-2">
                        {selectedItem.ticket.piePagina}
                      </div>
                    )}
                  </div>
                )}
              </div>
              <div className="modal-footer">
                {(selectedItem.estado === 'PENDIENTE' || selectedItem.estado === 'NO_CONFIRMADA') && (
                  <button
                    type="button"
                    className="btn btn-warning"
                    disabled={checkingDetail}
                    onClick={() => void handleVerDetalle(selectedItem, true)}
                  >
                    {checkingDetail ? 'Consultando...' : 'Consultar Estado en PuntoRed'}
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => window.print()}
                >
                  <i className="bi bi-printer me-1" />
                  Imprimir
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => setSelectedItem(null)}
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
