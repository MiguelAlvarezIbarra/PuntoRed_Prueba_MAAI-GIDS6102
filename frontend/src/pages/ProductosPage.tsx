import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { ServiceLogo } from '../components/ServiceLogo';
import { api, ApiClientError } from '../services/apiClient';
import type { CatalogoCategoriaItem, CatalogoServicioItem } from '../types/api';
import { formatCurrency } from '../utils/formatters';

interface ProductosPageProps {
  onSelectServicioParaPago: (servicio: CatalogoServicioItem) => void;
}

const MODULOS = [
  { id: 'TODOS', label: 'Todos', icon: 'bi-grid-3x3-gap' },
  { id: 'RECARGA', label: 'Recargas', icon: 'bi-phone' },
  { id: 'PAQUETE', label: 'Paquetes', icon: 'bi-wifi' },
  { id: 'RECAUDO', label: 'Servicios / Recaudos', icon: 'bi-receipt-cutoff' },
  { id: 'PIN', label: 'Pines Digitales', icon: 'bi-controller' },
] as const;

export const ProductosPage: React.FC<ProductosPageProps> = ({ onSelectServicioParaPago }) => {
  const [servicios, setServicios] = useState<CatalogoServicioItem[]>([]);
  const [categorias, setCategorias] = useState<CatalogoCategoriaItem[]>([]);
  const [fuenteDatos, setFuenteDatos] = useState<string>('PUNTORED_H2H_V2');
  const [saldoComercio, setSaldoComercio] = useState<string | null>(null);

  const [tipoOperacion, setTipoOperacion] = useState<string>('TODOS');
  const [categoriaSeleccionada, setCategoriaSeleccionada] = useState<string>('TODAS');
  const [busqueda, setBusqueda] = useState('');
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const cargarCatalogo = useCallback(async (forzarRecarga = false) => {
    if (forzarRecarga) {
      setSyncing(true);
    } else {
      setLoading(true);
    }
    setAlert(null);
    try {
      const res = await api.buscarCatalogoPuntored({ forzarRecarga });
      setServicios(res.servicios || []);
      setCategorias(res.categorias || []);
      setFuenteDatos(res.fuenteDatos || 'PUNTORED_H2H_V2');
      setSaldoComercio(res.saldoComercio || null);
      if (forzarRecarga) {
        setAlert({
          type: 'success',
          title: 'Catálogo actualizado desde PuntoRed H2H V2',
          message: `Se obtuvieron ${res.servicios?.length ?? 0} productos y convenios activos.`,
        });
      }
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo cargar el catálogo dinámico de PuntoRed',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Verifica la conexión con el backend.',
      });
    } finally {
      setLoading(false);
      setSyncing(false);
    }
  }, []);

  useEffect(() => {
    void cargarCatalogo(false);
  }, [cargarCatalogo]);

  const categoriasDisponibles = useMemo(() => {
    const set = new Set<string>();
    servicios.forEach((s) => {
      if (tipoOperacion === 'TODOS'
        || (tipoOperacion === 'RECAUDO' && (s.tipoOperacion === 'RECAUDO' || s.tipoOperacion === 'RECAUDO_V2'))
        || s.tipoOperacion === tipoOperacion) {
        if (s.categoria) {
          set.add(s.categoria);
        }
      }
    });
    return ['TODAS', ...Array.from(set).sort((a, b) => a.localeCompare(b))];
  }, [servicios, tipoOperacion]);

  const serviciosFiltrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return servicios.filter((s) => {
      if (tipoOperacion !== 'TODOS') {
        const esRecaudo = tipoOperacion === 'RECAUDO'
          && (s.tipoOperacion === 'RECAUDO' || s.tipoOperacion === 'RECAUDO_V2');
        if (!esRecaudo && s.tipoOperacion !== tipoOperacion) {
          return false;
        }
      }
      if (categoriaSeleccionada !== 'TODAS' && s.categoria !== categoriaSeleccionada) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        s.nombreServicio.toLowerCase().includes(q)
        || s.nombreProducto.toLowerCase().includes(q)
        || (s.descripcion || '').toLowerCase().includes(q)
        || (s.categoria || '').toLowerCase().includes(q)
        || (s.proveedorNombre || '').toLowerCase().includes(q)
        || (s.codigoProducto || '').includes(q)
        || (s.idConvenio || '').includes(q)
      );
    });
  }, [servicios, tipoOperacion, categoriaSeleccionada, busqueda]);

  const conteosPorModulo = useMemo(() => {
    const counts: Record<string, number> = {
      TODOS: servicios.length,
      RECARGA: 0,
      PAQUETE: 0,
      RECAUDO: 0,
      PIN: 0,
    };
    servicios.forEach((s) => {
      if (s.tipoOperacion === 'RECAUDO' || s.tipoOperacion === 'RECAUDO_V2') {
        counts.RECAUDO += 1;
      } else if (counts[s.tipoOperacion] !== undefined) {
        counts[s.tipoOperacion] += 1;
      }
    });
    return counts;
  }, [servicios]);

  const badgeTipoOperacion = (tipo: string) => {
    switch (tipo) {
      case 'RECARGA':
        return { label: 'Recarga', cls: 'text-bg-primary' };
      case 'PAQUETE':
        return { label: 'Paquete', cls: 'text-bg-info' };
      case 'RECAUDO':
      case 'RECAUDO_V2':
        return { label: 'Recaudo / Factura', cls: 'text-bg-success' };
      case 'PIN':
        return { label: 'PIN Digital', cls: 'text-bg-warning' };
      default:
        return { label: tipo, cls: 'text-bg-secondary' };
    }
  };

  return (
    <div className="d-flex flex-column gap-4">
      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      {/* Header dinámico PuntoRed */}
      <div className="pr-card p-4">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
          <div>
            <h5 className="fw-bold mb-1">
              <i className="bi bi-grid-3x3-gap-fill text-primary me-2" />
              Catálogo Dinámico de Servicios PuntoRed
            </h5>
            <p className="text-body-secondary small mb-0">
              Obtenido en tiempo real desde PuntoRed H2H V2 ({fuenteDatos}) ·{' '}
              <strong>{serviciosFiltrados.length}</strong> de {servicios.length} servicios visibles
              {saldoComercio ? (
                <span className="ms-2 badge text-bg-light border">
                  Saldo Comercio: {formatCurrency(saldoComercio)}
                </span>
              ) : null}
            </p>
          </div>
          <button
            type="button"
            className="btn btn-primary"
            disabled={syncing || loading}
            onClick={() => void cargarCatalogo(true)}
          >
            {syncing ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Actualizando desde PuntoRed...
              </>
            ) : (
              <>
                <i className="bi bi-arrow-repeat me-2" />
                Actualizar Catálogo en Vivo
              </>
            )}
          </button>
        </div>

        {/* Pestañas de módulos PuntoRed */}
        <div className="d-flex flex-wrap gap-2 mb-3">
          {MODULOS.map((m) => (
            <button
              key={m.id}
              type="button"
              className={`btn btn-sm ${
                tipoOperacion === m.id ? 'btn-primary' : 'btn-outline-secondary'
              }`}
              onClick={() => {
                setTipoOperacion(m.id);
                setCategoriaSeleccionada('TODAS');
              }}
            >
              <i className={`bi ${m.icon} me-1`} />
              {m.label}{' '}
              <span className="badge bg-white bg-opacity-25 ms-1">
                {conteosPorModulo[m.id] ?? 0}
              </span>
            </button>
          ))}
        </div>

        {/* Buscador y filtro por categoría */}
        <div className="row g-2 align-items-center">
          <div className="col-md-7">
            <div className="input-group">
              <span className="input-group-text">
                <i className="bi bi-search" />
              </span>
              <input
                type="text"
                className="form-control"
                placeholder="Buscar servicio, convenio, operador, descripción o código..."
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
              {busqueda && (
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setBusqueda('')}
                >
                  Limpiar
                </button>
              )}
            </div>
          </div>

          <div className="col-md-5">
            <select
              className="form-select"
              value={categoriaSeleccionada}
              onChange={(e) => setCategoriaSeleccionada(e.target.value)}
            >
              {categoriasDisponibles.map((cat) => (
                <option key={cat} value={cat}>
                  {cat === 'TODAS' ? 'Todas las categorías disponibles' : cat}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Destacados visuales de categorías con logo */}
        {categorias.some((c) => c.logoUrl) && (
          <div className="d-flex gap-2 overflow-auto pt-3 pb-1">
            {categorias
              .filter((c) => Boolean(c.logoUrl) && (tipoOperacion === 'TODOS' || c.tipoOperacion === tipoOperacion))
              .slice(0, 12)
              .map((cat) => (
                <button
                  key={`${cat.tipoOperacion}-${cat.idCategoria}-${cat.nombreCategoria}`}
                  type="button"
                  className="btn btn-sm btn-outline-secondary d-flex align-items-center gap-2 flex-shrink-0"
                  onClick={() => {
                    setTipoOperacion(cat.tipoOperacion);
                    setBusqueda(cat.nombreCategoria);
                  }}
                >
                  <ServiceLogo logoUrl={cat.logoUrl} name={cat.nombreCategoria} size={22} />
                  <span>{cat.nombreCategoria}</span>
                </button>
              ))}
          </div>
        )}
      </div>

      {/* Grid de productos y servicios */}
      {loading ? (
        <div className="pr-card p-5 text-center">
          <div className="spinner-border text-primary mb-2" />
          <div className="text-body-secondary">
            Consultando proveedores, convenios y logotipos reales en PuntoRed...
          </div>
        </div>
      ) : serviciosFiltrados.length === 0 ? (
        <div className="pr-card p-5 text-center text-body-secondary">
          <i className="bi bi-inbox fs-1 d-block mb-2" />
          No hay productos ni convenios que coincidan con tu filtro.
        </div>
      ) : (
        <div className="row g-3">
          {serviciosFiltrados.slice(0, 120).map((s) => {
            const badge = badgeTipoOperacion(s.tipoOperacion);
            return (
              <div key={s.claveUnica} className="col-md-6 col-xl-4">
                <div className="pr-card p-3 h-100 d-flex flex-column justify-content-between">
                  <div>
                    <div className="d-flex align-items-start gap-3 mb-2">
                      <ServiceLogo
                        logoUrl={s.logoUrl}
                        name={s.nombreServicio}
                        tipoOperacion={s.tipoOperacion}
                        size={48}
                      />
                      <div className="flex-grow-1 min-w-0">
                        <div className="d-flex justify-content-between align-items-start gap-2">
                          <span className={`badge ${badge.cls}`}>{badge.label}</span>
                          {s.precioSugerido != null && s.precioSugerido > 0 && (
                            <span className="fw-bold text-primary">
                              {formatCurrency(s.precioSugerido)}
                            </span>
                          )}
                        </div>
                        <h6 className="fw-bold mt-1 mb-0 text-truncate" title={s.nombreProducto}>
                          {s.nombreProducto}
                        </h6>
                        <div className="small text-body-secondary text-truncate">
                          {s.nombreServicio} · {s.categoria}
                        </div>
                      </div>
                    </div>

                    {s.descripcion && (
                      <p className="small text-body-secondary mb-2">{s.descripcion}</p>
                    )}

                    <div className="d-flex flex-wrap gap-1 mb-3">
                      {s.codigoProducto && (
                        <span className="badge text-bg-light border">
                          Prod: {s.codigoProducto}
                        </span>
                      )}
                      {s.idConvenio && (
                        <span className="badge text-bg-light border">
                          Convenio: {s.idConvenio}
                        </span>
                      )}
                      {s.sku && (
                        <span className="badge text-bg-light border">SKU: {s.sku}</span>
                      )}
                      {s.nombreBanco && (
                        <span className="badge text-bg-light border">{s.nombreBanco}</span>
                      )}
                    </div>
                  </div>

                  <button
                    type="button"
                    className="btn btn-outline-primary btn-sm w-100 mt-auto"
                    onClick={() => onSelectServicioParaPago(s)}
                  >
                    <i className="bi bi-shield-check me-2" />
                    Validar Referencia y Pagar
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
