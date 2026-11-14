import React, { useCallback, useEffect, useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { ServiceLogo } from '../components/ServiceLogo';
import { api, ApiClientError } from '../services/apiClient';
import type {
  AppSection,
  CatalogoServicioItem,
  CuentaData,
  PagoPuntoredResponse,
  Pin2faResponse,
  ValidacionReferenciaResponse,
} from '../types/api';
import { formatAccountNumber, formatCurrency, formatDateTime } from '../utils/formatters';

interface PagosPageProps {
  prefillCuenta?: string;
  prefillServicio?: CatalogoServicioItem | null;
  onNavigate?: (section: AppSection) => void;
}

export const PagosPage: React.FC<PagosPageProps> = ({
  prefillCuenta,
  prefillServicio,
  onNavigate,
}) => {
  const [cuentas, setCuentas] = useState<CuentaData[]>([]);
  const [servicios, setServicios] = useState<CatalogoServicioItem[]>([]);

  const [numeroCuenta, setNumeroCuenta] = useState(prefillCuenta || '');
  const [servicio, setServicio] = useState<CatalogoServicioItem | null>(prefillServicio || null);
  const [claveServicioSeleccionado, setClaveServicioSeleccionado] = useState(
    prefillServicio?.claveUnica || '',
  );

  // Paso 2: Referencia y validacion
  const [referencia1, setReferencia1] = useState('');
  const [referencia2, setReferencia2] = useState('');
  const [codigoIac, setCodigoIac] = useState('');
  const [validacion, setValidacion] = useState<ValidacionReferenciaResponse | null>(null);
  const [validatingRef, setValidatingRef] = useState(false);

  // Paso 3: Monto, confirmacion explicita y 2FA
  const [monto, setMonto] = useState(
    prefillServicio?.precioSugerido ? String(prefillServicio.precioSugerido) : '',
  );
  const [codigoRegistroSeleccionado, setCodigoRegistroSeleccionado] = useState('');
  const [confirmacionExplicita, setConfirmacionExplicita] = useState(false);
  const [pinSolicitado, setPinSolicitado] = useState<Pin2faResponse | null>(null);
  const [pin2fa, setPin2fa] = useState('');
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const [sendingPin, setSendingPin] = useState(false);
  const [paying, setPaying] = useState(false);
  const [verifyingStatus, setVerifyingStatus] = useState(false);
  const [resultadoPago, setResultadoPago] = useState<PagoPuntoredResponse | null>(null);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const cargarDatosIniciales = useCallback(async () => {
    try {
      const [ctaRes, catRes] = await Promise.all([
        api.obtenerMisCuentas().catch(() => ({ cuentas: [], codigo: 0, mensaje: '' })),
        api.buscarCatalogoPuntored({}).catch(() => ({ servicios: [], categorias: [], fuenteDatos: '', codigo: 0, mensaje: '' })),
      ]);
      const ctaList = ctaRes.cuentas || [];
      setCuentas(ctaList);
      if (!prefillCuenta && ctaList.length > 0) {
        setNumeroCuenta((prev) => prev || ctaList[0].numeroCuenta);
      }
      const srvList = catRes.servicios || [];
      setServicios(srvList);
      if (!prefillServicio && srvList.length > 0 && !servicio) {
        const primero = srvList[0];
        setServicio(primero);
        setClaveServicioSeleccionado(primero.claveUnica);
        if (primero.precioSugerido) {
          setMonto(String(primero.precioSugerido));
        }
      }
    } catch {
      // Manejado por banners individuales si falla
    }
  }, [prefillCuenta, prefillServicio, servicio]);

  useEffect(() => {
    void cargarDatosIniciales();
  }, [cargarDatosIniciales]);

  useEffect(() => {
    if (prefillCuenta) {
      setNumeroCuenta(prefillCuenta);
    }
  }, [prefillCuenta]);

  useEffect(() => {
    if (prefillServicio) {
      setServicio(prefillServicio);
      setClaveServicioSeleccionado(prefillServicio.claveUnica);
      setMonto(prefillServicio.precioSugerido ? String(prefillServicio.precioSugerido) : '');
      setValidacion(null);
      setResultadoPago(null);
      setConfirmacionExplicita(false);
      setReferencia1('');
      setReferencia2('');
    }
  }, [prefillServicio]);

  const cuentaSeleccionada = cuentas.find((c) => c.numeroCuenta === numeroCuenta);

  const resetValidacion = () => {
    setValidacion(null);
    setConfirmacionExplicita(false);
    setCodigoRegistroSeleccionado('');
    setResultadoPago(null);
  };

  const handleSeleccionarServicioDropdown = (clave: string) => {
    setClaveServicioSeleccionado(clave);
    const encontrado = servicios.find((s) => s.claveUnica === clave) || null;
    setServicio(encontrado);
    setMonto(encontrado?.precioSugerido ? String(encontrado.precioSugerido) : '');
    setReferencia1('');
    setReferencia2('');
    resetValidacion();
  };

  const handleValidarReferencia = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!servicio) {
      setAlert({ type: 'warning', title: 'Selecciona un servicio', message: 'Elige primero el producto o servicio a pagar.' });
      return;
    }
    const refTrim = referencia1.trim();
    if (!refTrim) {
      setAlert({ type: 'warning', title: 'Referencia requerida', message: 'Ingresa la referencia o número de línea antes de validar.' });
      return;
    }

    setValidatingRef(true);
    setAlert(null);
    setResultadoPago(null);
    try {
      const res = await api.validarReferencia({
        tipoOperacion: servicio.tipoOperacion,
        idConvenio: servicio.idConvenio,
        codigoProducto: servicio.codigoProducto,
        codigoProveedor: servicio.proveedorCodigo,
        referencia1: refTrim,
        referencia2: referencia2.trim() || undefined,
        montoReferencia: servicio.precioSugerido || (monto ? Number.parseFloat(monto) : 0),
        codigoIac: codigoIac.trim() || undefined,
      });
      setValidacion(res);
      if (res.referenciaValida) {
        if (res.montoSugerido && res.montoSugerido > 0) {
          setMonto(String(res.montoSugerido));
        }
        if (res.opciones && res.opciones.length > 0) {
          const op0 = res.opciones[0];
          setCodigoRegistroSeleccionado(op0.codigoRegistro || '');
          if (op0.monto && op0.monto > 0) {
            setMonto(String(op0.monto));
          }
        }
        setAlert({
          type: 'success',
          title: 'Referencia validada correctamente',
          message: res.mensajeConvenio || 'Puedes continuar con la confirmación y pago.',
        });
      } else {
        setAlert({
          type: 'danger',
          title: 'Referencia inválida o rechazada',
          codigo: res.codigo,
          message: res.mensajeConvenio || res.mensaje,
        });
      }
    } catch (err) {
      setValidacion(null);
      setAlert({
        type: 'danger',
        title: 'No se pudo validar la referencia',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error consultando el servicio.',
      });
    } finally {
      setValidatingRef(false);
    }
  };

  const handleSolicitarPin2fa = async () => {
    setAlert(null);
    if (!/^\d{18}$/.test(numeroCuenta.trim())) {
      setAlert({
        type: 'warning',
        title: 'Cuenta inválida',
        message: 'Selecciona o captura una cuenta válida de exactamente 18 dígitos.',
      });
      return;
    }
    setSendingPin(true);
    try {
      const res = await api.enviarPin2fa({ numeroCuenta: numeroCuenta.trim() });
      setPinSolicitado(res);
      if (res.pin2fa) {
        setPin2fa(res.pin2fa);
      }
      setAlert({
        type: 'success',
        title: 'PIN 2FA enviado',
        message: res.pin2fa
          ? `Enviado a ${res.correoDestino}. En modo pruebas se autocompletó el PIN (${res.pin2fa}).`
          : `Revisa tu correo ${res.correoDestino}. Expira en ${Math.round(res.expiraEnSegundos / 60)} minutos.`,
      });
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error enviando PIN 2FA',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error generando PIN.',
      });
    } finally {
      setSendingPin(false);
    }
  };

  const handleAbrirConfirmacion = (e: React.FormEvent) => {
    e.preventDefault();
    setAlert(null);
    if (!servicio || !validacion?.referenciaValida || !validacion.tokenValidacion) {
      setAlert({
        type: 'danger',
        title: 'Validación previa obligatoria',
        message: 'No puedes procesar el pago sin validar exitosamente la referencia en el paso 2.',
      });
      return;
    }
    const montoNum = Number.parseFloat(monto);
    if (Number.isNaN(montoNum) || montoNum < 1) {
      setAlert({ type: 'warning', title: 'Monto inválido', message: 'El monto a pagar debe ser mayor o igual a $1.00.' });
      return;
    }
    if (!confirmacionExplicita) {
      setAlert({
        type: 'warning',
        title: 'Confirmación requerida',
        message: 'Marca la casilla de confirmación explícita después de revisar el resumen.',
      });
      return;
    }
    if (!/^\d{6}$/.test(pin2fa.trim())) {
      setAlert({ type: 'warning', title: 'PIN 2FA requerido', message: 'Ingresa el PIN de 6 dígitos enviado a tu correo.' });
      return;
    }
    setShowConfirmModal(true);
  };

  const ejecutarPagoConfirmado = async () => {
    if (!servicio || !validacion?.tokenValidacion) {
      return;
    }
    setShowConfirmModal(false);
    setPaying(true);
    setAlert(null);
    try {
      const montoNum = Number.parseFloat(monto);
      const aval = validacion.camposAdicionalesAval || {};
      const res = await api.realizarPagoPuntored({
        numeroCuenta: numeroCuenta.trim(),
        tipoOperacion: servicio.tipoOperacion,
        nombreServicio: servicio.nombreServicio,
        nombreProducto: servicio.nombreProducto,
        categoria: servicio.categoria,
        logoUrl: servicio.logoUrl,
        referencia1: validacion.referencia1,
        referencia2: referencia2.trim() || undefined,
        monto: montoNum,
        idConvenio: servicio.idConvenio,
        codigoProveedor: servicio.proveedorCodigo,
        codigoCategoria: servicio.codigoCategoria,
        codigoProducto: servicio.codigoProducto,
        sku: servicio.sku,
        idPin: servicio.idPin,
        idSubcategoriaPin: servicio.idSubcategoriaPin,
        codigoRegistro: codigoRegistroSeleccionado || undefined,
        codigoIac: codigoIac.trim() || undefined,
        traceValidacion: validacion.trace || undefined,
        tokenValidacion: validacion.tokenValidacion,
        montoQuery: validacion.montoSugerido ?? undefined,
        aditionalValue54Eco: aval.aditionalValue54Eco || undefined,
        aditionalValue62Eco: aval.aditionalValue62Eco || undefined,
        accountDataAcredited: aval.accountDataAcredited || undefined,
        additionalCompanyCode: aval.additionalCompanyCode || undefined,
        confirmacionExplicita: true,
        pin2fa: pin2fa.trim(),
      });

      setResultadoPago(res);
      setPin2fa('');
      setPinSolicitado(null);
      // Invalidamos tokenValidacion usado para obligar nueva validacion si repite otro pago
      setValidacion(null);
      setConfirmacionExplicita(false);
      await cargarDatosIniciales();

      if (res.estado === 'EXITOSA' && res.transaccionConfirmada) {
        setAlert({
          type: 'success',
          title: 'Transacción Confirmada Exitosamente',
          message: `Folio proveedor: ${res.idTransaccionProveedor || res.trace}. Nuevo saldo: ${formatCurrency(res.saldoNuevo)}.`,
        });
      } else if (res.estado === 'RECHAZADA') {
        setAlert({
          type: 'danger',
          title: 'Operación Rechazada por el Proveedor',
          codigo: res.codigo,
          message: `${res.mensajeProveedor || res.mensaje}. Tu saldo no fue modificado.`,
        });
      } else {
        setAlert({
          type: 'warning',
          title: 'Operación Pendiente / No Confirmada',
          codigo: res.codigo,
          message: 'El proveedor no confirmó la respuesta a tiempo. Tu saldo permanece intacto hasta verificar el estado en PuntoRed.',
        });
      }
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'No se pudo procesar el pago',
        codigo: err instanceof ApiClientError ? err.codigo : undefined,
        message: err instanceof Error ? err.message : 'Error validando datos previos al pago.',
      });
    } finally {
      setPaying(false);
    }
  };

  const handleConsultarEstadoPendiente = async () => {
    if (!resultadoPago?.idTransaccionLocal) {
      return;
    }
    setVerifyingStatus(true);
    try {
      const det = await api.consultarDetallePago({
        idTransaccion: resultadoPago.idTransaccionLocal,
        consultarEstadoProveedor: true,
      });
      const tx = det.transaccion;
      setResultadoPago((prev) =>
        prev
          ? {
              ...prev,
              estado: tx.estado,
              transaccionConfirmada: tx.estado === 'EXITOSA',
              idTransaccionProveedor: tx.idTransaccionProveedor,
              codigoAutorizacion: tx.codigoAutorizacion,
              mensajeProveedor: tx.mensajeRespuesta,
              ticket: tx.ticket,
            }
          : prev,
      );
      setAlert({
        type: tx.estado === 'EXITOSA' ? 'success' : tx.estado === 'RECHAZADA' ? 'danger' : 'warning',
        title: `Estado actualizado: ${tx.estado}`,
        message: det.mensaje,
      });
      await cargarDatosIniciales();
    } catch (err) {
      setAlert({
        type: 'danger',
        title: 'Error consultando estado',
        message: err instanceof Error ? err.message : 'Intenta desde el módulo de Historial.',
      });
    } finally {
      setVerifyingStatus(false);
    }
  };

  const pagoHabilitado = Boolean(validacion?.referenciaValida && validacion?.tokenValidacion);

  return (
    <div className="d-flex flex-column gap-4">
      <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

      <div className="row g-4">
        {/* Columna Izquierda: Flujo guiado de 3 pasos */}
        <div className="col-lg-7 d-flex flex-column gap-4">
          {/* Paso 1: Servicio y Cuenta */}
          <div className="pr-card p-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">
                <span className="badge text-bg-primary me-2">1</span>
                Cuenta Origen y Servicio PuntoRed
              </h6>
              {onNavigate && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => onNavigate('productos')}
                >
                  <i className="bi bi-grid-3x3-gap me-1" />
                  Explorar Catálogo Visual
                </button>
              )}
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Cuenta bancaria de cargo (18 dígitos)</label>
              <select
                className="form-select"
                value={numeroCuenta}
                onChange={(e) => setNumeroCuenta(e.target.value)}
              >
                <option value="">Selecciona tu cuenta activa...</option>
                {cuentas.map((c) => (
                  <option key={c.numeroCuenta} value={c.numeroCuenta}>
                    {formatAccountNumber(c.numeroCuenta)} — Saldo {formatCurrency(c.saldo)}
                    {c.nombreCliente ? ` (${c.nombreCliente})` : ''}
                  </option>
                ))}
              </select>
              {cuentaSeleccionada && (
                <div className="form-text text-success fw-semibold">
                  Disponible en cuenta: {formatCurrency(cuentaSeleccionada.saldo)}
                </div>
              )}
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Producto / Convenio seleccionado</label>
              <select
                className="form-select"
                value={claveServicioSeleccionado}
                onChange={(e) => handleSeleccionarServicioDropdown(e.target.value)}
              >
                <option value="">Selecciona un servicio del catálogo...</option>
                {servicios.slice(0, 200).map((s) => (
                  <option key={s.claveUnica} value={s.claveUnica}>
                    [{s.tipoOperacion}] {s.nombreProducto}
                    {s.precioSugerido ? ` (${formatCurrency(s.precioSugerido)})` : ''}
                  </option>
                ))}
              </select>
            </div>

            {servicio && (
              <div className="p-3 rounded-3 border bg-body-tertiary d-flex align-items-center gap-3">
                <ServiceLogo
                  logoUrl={servicio.logoUrl}
                  name={servicio.nombreServicio}
                  tipoOperacion={servicio.tipoOperacion}
                  size={48}
                />
                <div className="flex-grow-1 min-w-0">
                  <div className="fw-bold text-truncate">{servicio.nombreProducto}</div>
                  <div className="small text-body-secondary">
                    {servicio.nombreServicio} · {servicio.categoria}
                  </div>
                  {servicio.descripcion && (
                    <div className="small text-body-secondary mt-1">{servicio.descripcion}</div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Paso 2: Consulta y Validación Obligatoria de Referencia */}
          <form className="pr-card p-4" onSubmit={(e) => void handleValidarReferencia(e)}>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">
                <span className="badge text-bg-primary me-2">2</span>
                Consulta y Validación de Referencia (Obligatorio)
              </h6>
              {validacion?.referenciaValida ? (
                <span className="badge text-bg-success">
                  <i className="bi bi-check-circle me-1" />
                  Referencia Válida
                </span>
              ) : (
                <span className="badge text-bg-secondary">Pendiente de validar</span>
              )}
            </div>

            <div className="row g-3">
              <div className="col-md-12">
                <label className="form-label fw-semibold">
                  {servicio?.etiquetaReferencia1 || 'Referencia principal / Número celular'}
                </label>
                <input
                  type="text"
                  className="form-control form-control-lg font-monospace"
                  placeholder={
                    servicio?.tipoOperacion === 'RECAUDO' || servicio?.tipoOperacion === 'RECAUDO_V2'
                      ? 'Ej: número de factura, contrato o cuenta'
                      : 'Ej: 3001234567'
                  }
                  value={referencia1}
                  onChange={(e) => {
                    setReferencia1(e.target.value);
                    resetValidacion();
                  }}
                />
              </div>

              {(servicio?.tipoOperacion === 'RECAUDO' || servicio?.etiquetaReferencia2) && (
                <>
                  <div className="col-md-7">
                    <label className="form-label">
                      {servicio?.etiquetaReferencia2 || 'Referencia secundaria (opcional)'}
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      value={referencia2}
                      onChange={(e) => {
                        setReferencia2(e.target.value);
                        resetValidacion();
                      }}
                    />
                  </div>
                  <div className="col-md-5">
                    <label className="form-label">Código IAC (si aplica)</label>
                    <input
                      type="text"
                      className="form-control"
                      value={codigoIac}
                      onChange={(e) => {
                        setCodigoIac(e.target.value);
                        resetValidacion();
                      }}
                    />
                  </div>
                </>
              )}

              <div className="col-12">
                <button
                  type="submit"
                  className="btn btn-outline-primary w-100 fw-semibold"
                  disabled={validatingRef || !servicio}
                >
                  {validatingRef ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" />
                      Validando referencia en PuntoRed...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-search me-2" />
                      Consultar y Validar Referencia
                    </>
                  )}
                </button>
              </div>
            </div>

            {validacion && (
              <div
                className={`mt-3 p-3 rounded-3 border ${
                  validacion.referenciaValida
                    ? 'border-success bg-success bg-opacity-10'
                    : 'border-danger bg-danger bg-opacity-10'
                }`}
              >
                <div className="fw-bold d-flex align-items-center gap-2 mb-1">
                  <i
                    className={`bi ${
                      validacion.referenciaValida
                        ? 'bi-patch-check-fill text-success'
                        : 'bi-x-octagon-fill text-danger'
                    }`}
                  />
                  <span>
                    {validacion.referenciaValida
                      ? 'Referencia Verificada para Pago'
                      : 'Referencia NO Válida — Pago Bloqueado'}
                  </span>
                </div>
                <div className="small">{validacion.mensajeConvenio || validacion.mensaje}</div>
                {validacion.trace && (
                  <div className="small text-body-secondary mt-1">
                    Trace conciliación: <code>{validacion.trace}</code>
                  </div>
                )}
              </div>
            )}
          </form>

          {/* Paso 3: Confirmación Explícita y 2FA (Bloqueado sin validación) */}
          <form className="pr-card p-4" onSubmit={handleAbrirConfirmacion}>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h6 className="fw-bold mb-0">
                <span className="badge text-bg-primary me-2">3</span>
                Resumen, 2FA y Confirmación Explícita
              </h6>
              {!pagoHabilitado && (
                <span className="badge text-bg-warning">
                  <i className="bi bi-lock-fill me-1" />
                  Bloqueado hasta validar referencia
                </span>
              )}
            </div>

            <fieldset disabled={!pagoHabilitado || paying}>
              {validacion?.opciones && validacion.opciones.length > 0 && (
                <div className="mb-3">
                  <label className="form-label fw-semibold">Concepto / Opción devuelta por convenio</label>
                  <select
                    className="form-select"
                    value={codigoRegistroSeleccionado}
                    onChange={(e) => {
                      setCodigoRegistroSeleccionado(e.target.value);
                      const op = validacion.opciones?.find((o) => o.codigoRegistro === e.target.value);
                      if (op?.monto) {
                        setMonto(String(op.monto));
                      }
                    }}
                  >
                    {validacion.opciones.map((op) => (
                      <option key={op.codigoRegistro || op.descripcionRegistro} value={op.codigoRegistro || ''}>
                        {op.descripcionRegistro} — {op.monto ? formatCurrency(op.monto) : 'Monto variable'}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className="row g-3 mb-3">
                <div className="col-md-6">
                  <label className="form-label fw-semibold">Importe a pagar (COP / MXN)</label>
                  <div className="input-group">
                    <span className="input-group-text">$</span>
                    <input
                      type="number"
                      step="1"
                      min="1"
                      className="form-control fw-bold"
                      readOnly={validacion?.montoEditable === false}
                      value={monto}
                      onChange={(e) => setMonto(e.target.value)}
                    />
                  </div>
                  {(validacion?.montoMinimo || validacion?.montoMaximo) && (
                    <div className="form-text">
                      Rango: {validacion.montoMinimo ? formatCurrency(validacion.montoMinimo) : '$1'} –{' '}
                      {validacion.montoMaximo ? formatCurrency(validacion.montoMaximo) : 'Sin límite'}
                    </div>
                  )}
                </div>

                <div className="col-md-6">
                  <label className="form-label fw-semibold">PIN 2FA de verificación</label>
                  <div className="input-group">
                    <input
                      type="text"
                      maxLength={6}
                      className="form-control font-monospace text-center fw-bold"
                      placeholder="6 dígitos"
                      value={pin2fa}
                      onChange={(e) => setPin2fa(e.target.value)}
                    />
                    <button
                      type="button"
                      className="btn btn-outline-primary"
                      disabled={sendingPin}
                      onClick={() => void handleSolicitarPin2fa()}
                    >
                      {sendingPin ? 'Enviando...' : 'Solicitar PIN'}
                    </button>
                  </div>
                  {pinSolicitado && (
                    <div className="form-text text-primary">
                      Enviado a {pinSolicitado.correoDestino}
                    </div>
                  )}
                </div>
              </div>

              <div className="form-check mb-3 p-3 rounded-3 border bg-body-tertiary">
                <input
                  id="chkConfirmacionExplicita"
                  type="checkbox"
                  className="form-check-input ms-0 me-2"
                  checked={confirmacionExplicita}
                  onChange={(e) => setConfirmacionExplicita(e.target.checked)}
                />
                <label className="form-check-label small fw-semibold" htmlFor="chkConfirmacionExplicita">
                  He verificado el servicio, la referencia ({validacion?.referencia1 || '—'}) y el
                  importe ({monto ? formatCurrency(monto) : '$0.00'}), y autorizo el cargo explícitamente.
                </label>
              </div>

              <button
                type="submit"
                className="btn btn-success btn-lg w-100 fw-semibold"
                disabled={paying || !confirmacionExplicita || !pin2fa}
              >
                {paying ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" />
                    Procesando pago en PuntoRed...
                  </>
                ) : (
                  <>
                    <i className="bi bi-check2-circle me-2" />
                    Revisar Resumen y Confirmar Pago
                  </>
                )}
              </button>
            </fieldset>
          </form>
        </div>

        {/* Columna Derecha: Resultado de la transacción & Tirilla Digital */}
        <div className="col-lg-5">
          {resultadoPago ? (
            <div className="pr-card p-4 border-2">
              <div className="text-center pb-3 border-bottom mb-3">
                {resultadoPago.estado === 'EXITOSA' ? (
                  <span className="badge text-bg-success fs-6 px-3 py-2 mb-2">
                    <i className="bi bi-check-circle-fill me-2" />
                    TRANSACCIÓN EXITOSA
                  </span>
                ) : resultadoPago.estado === 'RECHAZADA' ? (
                  <span className="badge text-bg-danger fs-6 px-3 py-2 mb-2">
                    <i className="bi bi-x-circle-fill me-2" />
                    OPERACIÓN RECHAZADA
                  </span>
                ) : (
                  <span className="badge text-bg-warning fs-6 px-3 py-2 mb-2">
                    <i className="bi bi-hourglass-split me-2" />
                    {resultadoPago.estado === 'NO_CONFIRMADA'
                      ? 'NO CONFIRMADA (TIMEOUT)'
                      : 'PENDIENTE DE CONFIRMACIÓN'}
                  </span>
                )}

                <div className="d-flex justify-content-center my-2">
                  <ServiceLogo
                    logoUrl={resultadoPago.ticket?.logoUrl || resultadoPago.logoUrl}
                    name={resultadoPago.nombreServicio}
                    size={56}
                  />
                </div>
                <h5 className="fw-bold mb-1">{resultadoPago.nombreProducto || resultadoPago.nombreServicio}</h5>
                <div className="fs-3 fw-bold text-primary">{formatCurrency(resultadoPago.monto)}</div>
                <div className="small text-body-secondary">{resultadoPago.mensaje}</div>
              </div>

              <div className="small d-flex flex-column gap-2 mb-3">
                <div className="d-flex justify-content-between">
                  <span className="text-body-secondary">Folio Proveedor:</span>
                  <strong className="font-monospace">
                    {resultadoPago.idTransaccionProveedor || 'Sin asignar'}
                  </strong>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-body-secondary">Autorización:</span>
                  <strong className="font-monospace">
                    {resultadoPago.codigoAutorizacion || '—'}
                  </strong>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-body-secondary">Trace Conciliación:</span>
                  <strong className="font-monospace">{resultadoPago.trace || '—'}</strong>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-body-secondary">Referencia:</span>
                  <strong>{resultadoPago.referencia1}</strong>
                </div>
                <div className="d-flex justify-content-between">
                  <span className="text-body-secondary">Fecha / Hora:</span>
                  <span>{formatDateTime(resultadoPago.fechaRegistro)}</span>
                </div>
                <div className="d-flex justify-content-between border-top pt-2">
                  <span className="text-body-secondary">Saldo anterior → nuevo:</span>
                  <strong>
                    {formatCurrency(resultadoPago.saldoAnterior)} →{' '}
                    <span className={resultadoPago.transaccionConfirmada ? 'text-success' : ''}>
                      {formatCurrency(resultadoPago.saldoNuevo)}
                    </span>
                  </strong>
                </div>
              </div>

              {resultadoPago.pinContenido && (
                <div className="alert alert-info py-2 small">
                  <strong>PIN / Código Digital Generado:</strong>{' '}
                  <code className="fs-6">{resultadoPago.pinContenido}</code>
                </div>
              )}

              {resultadoPago.ticket && (
                <div className="p-3 rounded-3 bg-body-tertiary border small mb-3">
                  {resultadoPago.ticket.encabezado && (
                    <div className="text-center text-body-secondary mb-1">
                      {resultadoPago.ticket.encabezado}
                    </div>
                  )}
                  <div className="text-center fw-bold mb-2">
                    {resultadoPago.ticket.titulo || 'COMPROBANTE DIGITAL'}
                  </div>
                  {resultadoPago.ticket.cuerpo &&
                    Object.entries(resultadoPago.ticket.cuerpo).map(([k, v]) => (
                      <div key={k} className="d-flex justify-content-between gap-2">
                        <span className="text-body-secondary">{k}:</span>
                        <span className="fw-semibold text-end">{String(v ?? '')}</span>
                      </div>
                    ))}
                  {resultadoPago.ticket.piePagina && (
                    <div className="text-center text-body-secondary mt-2 border-top pt-2">
                      {resultadoPago.ticket.piePagina}
                    </div>
                  )}
                </div>
              )}

              <div className="d-flex flex-column gap-2">
                {(resultadoPago.estado === 'PENDIENTE' || resultadoPago.estado === 'NO_CONFIRMADA') && (
                  <button
                    type="button"
                    className="btn btn-warning w-100 fw-semibold"
                    disabled={verifyingStatus}
                    onClick={() => void handleConsultarEstadoPendiente()}
                  >
                    {verifyingStatus ? 'Consultando PuntoRed...' : 'Verificar Estado en PuntoRed'}
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm w-100"
                  onClick={() => window.print()}
                >
                  <i className="bi bi-printer me-2" />
                  Imprimir Comprobante
                </button>
              </div>
            </div>
          ) : (
            <div className="pr-card p-4 text-center text-body-secondary">
              <i className="bi bi-receipt fs-1 d-block mb-2 text-primary opacity-50" />
              <h6 className="fw-bold text-body">Comprobante de Pago</h6>
              <p className="small mb-0">
                Valida primero tu referencia en el paso 2, confirma el resumen con tu PIN 2FA y aquí
                se mostrará el resultado oficial de PuntoRed (Exitosa, Rechazada o Pendiente / No Confirmada)
                junto con la tirilla digital y folio de autorización.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Modal de Confirmación Explícita de la Operación */}
      {showConfirmModal && servicio && (
        <div
          className="modal d-block show"
          style={{ backgroundColor: 'rgba(15, 23, 42, 0.6)' }}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Confirmación Explícita de Pago</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowConfirmModal(false)}
                />
              </div>
              <div className="modal-body">
                <div className="d-flex align-items-center gap-3 mb-3 p-3 rounded-3 bg-body-tertiary">
                  <ServiceLogo
                    logoUrl={servicio.logoUrl}
                    name={servicio.nombreServicio}
                    tipoOperacion={servicio.tipoOperacion}
                    size={48}
                  />
                  <div>
                    <div className="fw-bold">{servicio.nombreProducto}</div>
                    <div className="small text-body-secondary">{servicio.categoria}</div>
                  </div>
                </div>
                <div className="small d-flex flex-column gap-2">
                  <div className="d-flex justify-content-between">
                    <span className="text-body-secondary">Cuenta cargo:</span>
                    <strong>{formatAccountNumber(numeroCuenta)}</strong>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span className="text-body-secondary">Referencia validada:</span>
                    <strong>{validacion?.referencia1}</strong>
                  </div>
                  {validacion?.trace && (
                    <div className="d-flex justify-content-between">
                      <span className="text-body-secondary">Trace validación:</span>
                      <code>{validacion.trace}</code>
                    </div>
                  )}
                  <div className="d-flex justify-content-between fs-5 border-top pt-2 mt-1">
                    <span className="fw-semibold">Total a descontar:</span>
                    <strong className="text-primary">{formatCurrency(monto)}</strong>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowConfirmModal(false)}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-success fw-semibold"
                  onClick={() => void ejecutarPagoConfirmado()}
                >
                  <i className="bi bi-shield-check me-2" />
                  Confirmar y Pagar Ahora
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
