import React, { useEffect, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { ClienteFormModal } from '../components/ClienteFormModal';
import { useAuth } from '../context/AuthContext';
import { api, ApiClientError } from '../services/apiClient';
import type {
  AdminClienteRequest,
  ClienteActualizaRequest,
  ClienteData,
  ClienteSearchRequest,
} from '../types/api';
import { formatAccountNumber, formatCurrency, formatDateTime } from '../utils/formatters';

interface ClientesPageProps {
  onGoToCuenta: (numeroCuenta: string) => void;
  onGoToPago: (numeroCuenta: string) => void;
}

export const ClientesPage: React.FC<ClientesPageProps> = ({ onGoToCuenta, onGoToPago }) => {
  const { isAdmin, clearLocalSession } = useAuth();
  const [filtros, setFiltros] = useState<ClienteSearchRequest>({
    nombre: '',
    rfc: '',
    curp: '',
    numeroCuenta: '',
    correo: '',
    fechaDesde: '',
    fechaHasta: '',
    activo: null,
  });

  const [clientes, setClientes] = useState<ClienteData[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const [showAdminModal, setShowAdminModal] = useState(false);
  const [editingCliente, setEditingCliente] = useState<ClienteData | null>(null);
  const [bajaCliente, setBajaCliente] = useState<ClienteData | null>(null);
  const [bajaMotivo, setBajaMotivo] = useState('');

  const buscar = async (customFiltros?: ClienteSearchRequest) => {
    setLoading(true);
    setAlert(null);
    const active = customFiltros ?? filtros;
    const payload: ClienteSearchRequest = {
      ...(active.nombre?.trim() ? { nombre: active.nombre.trim() } : {}),
      ...(active.rfc?.trim() ? { rfc: active.rfc.trim() } : {}),
      ...(active.curp?.trim() ? { curp: active.curp.trim() } : {}),
      ...(active.numeroCuenta?.trim() ? { numeroCuenta: active.numeroCuenta.trim() } : {}),
      ...(active.correo?.trim() ? { correo: active.correo.trim() } : {}),
      ...(active.fechaDesde ? { fechaDesde: active.fechaDesde } : {}),
      ...(active.fechaHasta ? { fechaHasta: active.fechaHasta } : {}),
      ...(active.activo !== null && active.activo !== undefined ? { activo: active.activo } : {}),
    };

    try {
      const res = await api.buscarClientes(payload);
      setClientes(res.clientes || []);
    } catch (err) {
      if (err instanceof ApiClientError && err.codigo === 9) {
        clearLocalSession();
        return;
      }
      setAlert({
        type: 'danger',
        title: 'Error en búsqueda dinámica',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'No fue posible consultar los clientes.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void buscar({});
  }, []);

  const handleLimpiarFiltros = () => {
    const empty: ClienteSearchRequest = {
      nombre: '',
      rfc: '',
      curp: '',
      numeroCuenta: '',
      correo: '',
      fechaDesde: '',
      fechaHasta: '',
      activo: null,
    };
    setFiltros(empty);
    void buscar(empty);
  };

  const handleCrearAdmin = async (payload: AdminClienteRequest) => {
    setSubmitting(true);
    setAlert(null);
    try {
      const res = await api.registrarClienteAdmin(payload);
      setShowAdminModal(false);
      setAlert({
        type: 'success',
        title: 'Cliente creado por Administrador',
        message: `${res.cliente?.nombre} registrado. Cuenta: ${formatAccountNumber(res.cliente?.numeroCuenta)}`,
      });
      await buscar();
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo registrar el cliente',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error de validación.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleActualizar = async (payload: ClienteActualizaRequest) => {
    setSubmitting(true);
    setAlert(null);
    try {
      const res = await api.actualizarCliente(payload);
      setEditingCliente(null);
      setAlert({
        type: 'success',
        title: 'Cliente actualizado',
        message: res.mensaje || `Datos de ${payload.rfc} actualizados correctamente.`,
      });
      await buscar();
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo actualizar el cliente',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error de actualización.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmarBaja = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bajaCliente) {
      return;
    }
    setSubmitting(true);
    setAlert(null);
    try {
      const res = await api.darBajaCliente({
        rfc: bajaCliente.rfc,
        ...(bajaMotivo.trim() ? { motivo: bajaMotivo.trim() } : {}),
      });
      setBajaCliente(null);
      setBajaMotivo('');
      setAlert({
        type: 'warning',
        title: 'Baja lógica aplicada',
        message: res.mensaje || 'Cliente y cuentas marcados como inactivos.',
      });
      await buscar();
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error al dar de baja',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'No fue posible dar de baja al cliente.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleReactivar = async (rfc: string) => {
    setSubmitting(true);
    setAlert(null);
    try {
      const res = await api.reactivarCliente({ rfc });
      setAlert({
        type: 'success',
        title: 'Cliente reactivado',
        message: res.mensaje || `Cliente ${rfc} reactivado correctamente.`,
      });
      await buscar();
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error al reactivar',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'No fue posible reactivar el cliente.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
          <h4 className="fw-bold mb-1">Directorio y Búsqueda Dinámica de Clientes</h4>
          <p className="text-body-secondary small mb-0">
            Endpoint único <code>POST /clientes/search</code> con combinación dinámica de filtros AND
            {!isAdmin && ' (vista restringida a tu propio registro)'}.
          </p>
        </div>
        {isAdmin && (
          <button
            type="button"
            className="btn btn-primary fw-semibold"
            onClick={() => setShowAdminModal(true)}
          >
            <i className="bi bi-person-plus-fill me-2" />
            Nuevo Cliente (Admin)
          </button>
        )}
      </div>

      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      {/* Panel de Filtros Dinámicos */}
      <form
        className="pr-card p-3 p-md-4"
        onSubmit={(e) => {
          e.preventDefault();
          void buscar();
        }}
      >
        <div className="row g-3">
          <div className="col-sm-6 col-lg-3">
            <label className="form-label small fw-semibold">Nombre</label>
            <input
              type="text"
              className="form-control"
              placeholder="Coincidencia parcial..."
              value={filtros.nombre || ''}
              onChange={(e) => setFiltros({ ...filtros, nombre: e.target.value })}
            />
          </div>
          <div className="col-sm-6 col-lg-3">
            <label className="form-label small fw-semibold">RFC</label>
            <input
              type="text"
              className="form-control text-uppercase"
              placeholder="Ej. GODJ900515..."
              value={filtros.rfc || ''}
              onChange={(e) => setFiltros({ ...filtros, rfc: e.target.value.toUpperCase() })}
            />
          </div>
          <div className="col-sm-6 col-lg-3">
            <label className="form-label small fw-semibold">CURP</label>
            <input
              type="text"
              className="form-control text-uppercase"
              placeholder="Ej. GODJ900515..."
              value={filtros.curp || ''}
              onChange={(e) => setFiltros({ ...filtros, curp: e.target.value.toUpperCase() })}
            />
          </div>
          <div className="col-sm-6 col-lg-3">
            <label className="form-label small fw-semibold">Número de Cuenta</label>
            <input
              type="text"
              className="form-control pr-account-number"
              placeholder="18 dígitos o parcial..."
              value={filtros.numeroCuenta || ''}
              onChange={(e) => setFiltros({ ...filtros, numeroCuenta: e.target.value })}
            />
          </div>
          <div className="col-sm-6 col-lg-3">
            <label className="form-label small fw-semibold">Correo</label>
            <input
              type="text"
              className="form-control"
              placeholder="correo@dominio.com"
              value={filtros.correo || ''}
              onChange={(e) => setFiltros({ ...filtros, correo: e.target.value })}
            />
          </div>
          <div className="col-sm-6 col-lg-2">
            <label className="form-label small fw-semibold">Registro Desde</label>
            <input
              type="date"
              className="form-control"
              value={filtros.fechaDesde || ''}
              onChange={(e) => setFiltros({ ...filtros, fechaDesde: e.target.value })}
            />
          </div>
          <div className="col-sm-6 col-lg-2">
            <label className="form-label small fw-semibold">Registro Hasta</label>
            <input
              type="date"
              className="form-control"
              value={filtros.fechaHasta || ''}
              onChange={(e) => setFiltros({ ...filtros, fechaHasta: e.target.value })}
            />
          </div>
          <div className="col-sm-6 col-lg-2">
            <label className="form-label small fw-semibold">Estatus</label>
            <select
              className="form-select"
              value={
                filtros.activo === null || filtros.activo === undefined
                  ? ''
                  : String(filtros.activo)
              }
              onChange={(e) =>
                setFiltros({
                  ...filtros,
                  activo: e.target.value === '' ? null : e.target.value === 'true',
                })
              }
            >
              <option value="">Todos</option>
              <option value="true">Activos</option>
              <option value="false">Inactivos (Baja)</option>
            </select>
          </div>
          <div className="col-lg-3 d-flex align-items-end gap-2">
            <button type="submit" className="btn btn-primary flex-grow-1" disabled={loading}>
              <i className="bi bi-search me-1" /> Buscar
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={handleLimpiarFiltros}
              disabled={loading}
            >
              Limpiar
            </button>
          </div>
        </div>
      </form>

      {/* Tabla Escritorio */}
      <div className="pr-card d-none d-lg-block overflow-hidden">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Cliente</th>
                <th>RFC / CURP</th>
                <th>Contacto</th>
                <th>Cuenta Asignada</th>
                <th>Ingreso</th>
                <th>Estatus</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-body-secondary">
                    <span className="spinner-border spinner-border-sm me-2" />
                    Consultando clientes...
                  </td>
                </tr>
              ) : clientes.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-4 text-body-secondary">
                    No se encontraron clientes con los filtros seleccionados.
                  </td>
                </tr>
              ) : (
                clientes.map((c) => (
                  <tr key={c.rfc}>
                    <td>
                      <div className="fw-bold">
                        {c.nombre} {c.segundoNombre || ''} {c.apellidoPaterno} {c.apellidoMaterno}
                      </div>
                      <small className="text-body-secondary">
                        {c.ocupacion} · {c.empresa}
                      </small>
                    </td>
                    <td>
                      <div className="pr-account-number small fw-semibold">{c.rfc}</div>
                      <small className="text-body-secondary pr-account-number">{c.curp}</small>
                    </td>
                    <td>
                      <div className="small">{c.correo}</div>
                      <small className="text-body-secondary">{c.telefonoMovil}</small>
                    </td>
                    <td>
                      {c.numeroCuenta ? (
                        <button
                          type="button"
                          className="btn btn-link btn-sm p-0 pr-account-number text-decoration-none"
                          onClick={() => onGoToCuenta(c.numeroCuenta!)}
                        >
                          {formatAccountNumber(c.numeroCuenta)}
                        </button>
                      ) : (
                        '—'
                      )}
                      <div className="small text-body-secondary">
                        Alta: {formatDateTime(c.fechaRegistro)}
                      </div>
                    </td>
                    <td className="fw-semibold">{formatCurrency(c.ingresoMensual)}</td>
                    <td>
                      <span className={`badge ${c.activo ? 'text-bg-success' : 'text-bg-danger'}`}>
                        {c.activo ? 'ACTIVO' : 'INACTIVO'}
                      </span>
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        {c.activo && c.numeroCuenta && (
                          <button
                            type="button"
                            className="btn btn-outline-primary"
                            title="Pagar con esta cuenta"
                            onClick={() => onGoToPago(c.numeroCuenta!)}
                          >
                            <i className="bi bi-send" />
                          </button>
                        )}
                        {c.activo && (
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            title="Editar cliente"
                            onClick={() => setEditingCliente(c)}
                          >
                            <i className="bi bi-pencil" />
                          </button>
                        )}
                        {c.activo ? (
                          <button
                            type="button"
                            className="btn btn-outline-danger"
                            title="Baja lógica"
                            onClick={() => setBajaCliente(c)}
                          >
                            <i className="bi bi-person-x" />
                          </button>
                        ) : (
                          isAdmin && (
                            <button
                              type="button"
                              className="btn btn-outline-success"
                              title="Reactivar cliente"
                              disabled={submitting}
                              onClick={() => void handleReactivar(c.rfc)}
                            >
                              <i className="bi bi-arrow-counterclockwise" />
                            </button>
                          )
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Tarjetas Móvil / APK */}
      <div className="d-lg-none d-flex flex-column gap-3">
        {loading ? (
          <div className="pr-card p-4 text-center text-body-secondary">
            <span className="spinner-border spinner-border-sm me-2" />
            Consultando clientes...
          </div>
        ) : clientes.length === 0 ? (
          <div className="pr-card p-4 text-center text-body-secondary">
            Sin resultados para los filtros aplicados.
          </div>
        ) : (
          clientes.map((c) => (
            <div key={c.rfc} className="pr-card p-3">
              <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                <div>
                  <div className="fw-bold">
                    {c.nombre} {c.apellidoPaterno} {c.apellidoMaterno}
                  </div>
                  <div className="small text-body-secondary pr-account-number">{c.rfc}</div>
                </div>
                <span className={`badge ${c.activo ? 'text-bg-success' : 'text-bg-danger'}`}>
                  {c.activo ? 'ACTIVO' : 'INACTIVO'}
                </span>
              </div>

              <div className="small mb-2">
                <div>
                  <i className="bi bi-envelope me-1 text-primary" /> {c.correo}
                </div>
                <div>
                  <i className="bi bi-credit-card me-1 text-primary" />{' '}
                  <span className="pr-account-number">{formatAccountNumber(c.numeroCuenta)}</span>
                </div>
                <div>
                  <i className="bi bi-cash-coin me-1 text-success" />{' '}
                  {formatCurrency(c.ingresoMensual)}
                </div>
              </div>

              <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                {c.activo && c.numeroCuenta && (
                  <button
                    type="button"
                    className="btn btn-sm btn-primary flex-grow-1"
                    onClick={() => onGoToPago(c.numeroCuenta!)}
                  >
                    <i className="bi bi-send me-1" /> Pagar
                  </button>
                )}
                {c.activo && c.numeroCuenta && (
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => onGoToCuenta(c.numeroCuenta!)}
                  >
                    Saldo
                  </button>
                )}
                {c.activo && (
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => setEditingCliente(c)}
                  >
                    Editar
                  </button>
                )}
                {c.activo ? (
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => setBajaCliente(c)}
                  >
                    Baja
                  </button>
                ) : (
                  isAdmin && (
                    <button
                      type="button"
                      className="btn btn-sm btn-success flex-grow-1"
                      onClick={() => void handleReactivar(c.rfc)}
                    >
                      Reactivar
                    </button>
                  )
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {showAdminModal && (
        <ClienteFormModal
          mode="admin-create"
          submitting={submitting}
          onCancel={() => setShowAdminModal(false)}
          onSubmitAdmin={handleCrearAdmin}
        />
      )}

      {editingCliente && (
        <ClienteFormModal
          mode="edit"
          initialCliente={editingCliente}
          submitting={submitting}
          onCancel={() => setEditingCliente(null)}
          onSubmitUpdate={handleActualizar}
        />
      )}

      {bajaCliente && (
        <div
          className="modal d-block"
          tabIndex={-1}
          style={{ backgroundColor: 'rgba(10, 22, 40, 0.7)' }}
        >
          <div className="modal-dialog modal-dialog-centered">
            <form className="modal-content pr-card border-0" onSubmit={handleConfirmarBaja}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-danger">
                  <i className="bi bi-exclamation-triangle me-2" />
                  Confirmar Baja Lógica
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setBajaCliente(null)}
                />
              </div>
              <div className="modal-body">
                <p className="small mb-3">
                  Se dará de baja al cliente <strong>{bajaCliente.nombre}</strong> (
                  <code>{bajaCliente.rfc}</code>) y todas sus cuentas pasarán a estatus{' '}
                  <strong>INACTIVA</strong>.
                </p>
                <label className="form-label small fw-semibold">Motivo de baja (opcional)</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ej. Solicitud del titular"
                  value={bajaMotivo}
                  onChange={(e) => setBajaMotivo(e.target.value)}
                />
              </div>
              <div className="modal-footer border-top">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setBajaCliente(null)}
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-danger" disabled={submitting}>
                  {submitting ? 'Aplicando baja...' : 'Confirmar Baja'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
