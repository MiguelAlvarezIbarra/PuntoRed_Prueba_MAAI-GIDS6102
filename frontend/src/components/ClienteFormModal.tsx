import React, { useMemo, useState } from 'react';
import { CAT_NACIONALIDADES, CAT_PAISES, ESTADOS_CIVILES } from '../constants/catalogs';
import type {
  AdminClienteRequest,
  ClienteActualizaRequest,
  ClienteData,
  ClienteRequest,
} from '../types/api';
import { calcularPrefijoRfcCurp, esMayorDeEdad } from '../utils/formatters';

export type ClienteFormMode = 'public-register' | 'admin-create' | 'edit';

interface ClienteFormModalProps {
  mode: ClienteFormMode;
  initialCliente?: ClienteData | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmitPublic?: (payload: ClienteRequest) => Promise<void>;
  onSubmitAdmin?: (payload: AdminClienteRequest) => Promise<void>;
  onSubmitUpdate?: (payload: ClienteActualizaRequest) => Promise<void>;
}

export const ClienteFormModal: React.FC<ClienteFormModalProps> = ({
  mode,
  initialCliente,
  submitting,
  onCancel,
  onSubmitPublic,
  onSubmitAdmin,
  onSubmitUpdate,
}) => {
  const isEdit = mode === 'edit';
  const isAdminCreate = mode === 'admin-create';

  const [nombre, setNombre] = useState(initialCliente?.nombre || '');
  const [segundoNombre, setSegundoNombre] = useState(initialCliente?.segundoNombre || '');
  const [apellidoPaterno, setApellidoPaterno] = useState(initialCliente?.apellidoPaterno || '');
  const [apellidoMaterno, setApellidoMaterno] = useState(initialCliente?.apellidoMaterno || '');
  const [fechaNacimiento, setFechaNacimiento] = useState(initialCliente?.fechaNacimiento || '');
  const [curp, setCurp] = useState(initialCliente?.curp || '');
  const [rfc, setRfc] = useState(initialCliente?.rfc || '');
  const [sexo, setSexo] = useState<'H' | 'M'>(initialCliente?.sexo || 'H');
  const [nacionalidad, setNacionalidad] = useState(initialCliente?.nacionalidad || 'Mexicano');
  const [estadoCivil, setEstadoCivil] = useState(initialCliente?.estadoCivil || 'Soltero');
  const [correo, setCorreo] = useState(initialCliente?.correo || '');
  const [telefonoMovil, setTelefonoMovil] = useState(initialCliente?.telefonoMovil || '');
  const [telefonoAlternativo, setTelefonoAlternativo] = useState(
    initialCliente?.telefonoAlternativo || '',
  );
  const [ocupacion, setOcupacion] = useState(initialCliente?.ocupacion || '');
  const [empresa, setEmpresa] = useState(initialCliente?.empresa || '');
  const [ingresoMensual, setIngresoMensual] = useState(
    initialCliente?.ingresoMensual ? String(initialCliente.ingresoMensual) : '',
  );
  const [password, setPassword] = useState('');
  const [rol, setRol] = useState<1 | 2>(2);

  // Domicilio
  const [calle, setCalle] = useState(initialCliente?.domicilio?.calle || '');
  const [numeroExterior, setNumeroExterior] = useState(
    initialCliente?.domicilio?.numeroExterior || '',
  );
  const [numeroInterior, setNumeroInterior] = useState(
    initialCliente?.domicilio?.numeroInterior || '',
  );
  const [colonia, setColonia] = useState(initialCliente?.domicilio?.colonia || '');
  const [municipio, setMunicipio] = useState(initialCliente?.domicilio?.municipio || '');
  const [estado, setEstado] = useState(initialCliente?.domicilio?.estado || '');
  const [codigoPostal, setCodigoPostal] = useState(initialCliente?.domicilio?.codigoPostal || '');
  const [pais, setPais] = useState(initialCliente?.domicilio?.pais || 'México');

  const [localError, setLocalError] = useState('');

  const prefijoEsperado = useMemo(() => {
    const fechaBase = isEdit ? initialCliente?.fechaNacimiento || '' : fechaNacimiento;
    return calcularPrefijoRfcCurp(nombre, apellidoPaterno, apellidoMaterno, fechaBase);
  }, [nombre, apellidoPaterno, apellidoMaterno, fechaNacimiento, isEdit, initialCliente]);

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError('');

    if (!isEdit && !esMayorDeEdad(fechaNacimiento)) {
      setLocalError('El cliente debe ser mayor de edad (18 años o más).');
      return;
    }

    const ingresoNum = Number.parseFloat(ingresoMensual);
    if (Number.isNaN(ingresoNum) || ingresoNum <= 0) {
      setLocalError('El ingreso mensual debe ser mayor a cero.');
      return;
    }

    const domicilio = {
      calle: calle.trim(),
      numeroExterior: numeroExterior.trim(),
      ...(numeroInterior.trim() ? { numeroInterior: numeroInterior.trim() } : {}),
      colonia: colonia.trim(),
      municipio: municipio.trim(),
      estado: estado.trim(),
      codigoPostal: codigoPostal.trim(),
      pais,
    };

    if (isEdit && onSubmitUpdate) {
      const payload: ClienteActualizaRequest = {
        rfc: rfc.trim().toUpperCase(),
        nombre: nombre.trim(),
        ...(segundoNombre.trim() ? { segundoNombre: segundoNombre.trim() } : {}),
        apellidoPaterno: apellidoPaterno.trim(),
        apellidoMaterno: apellidoMaterno.trim(),
        sexo,
        nacionalidad,
        estadoCivil,
        correo: correo.trim(),
        telefonoMovil: telefonoMovil.trim(),
        ...(telefonoAlternativo.trim() ? { telefonoAlternativo: telefonoAlternativo.trim() } : {}),
        ocupacion: ocupacion.trim(),
        empresa: empresa.trim(),
        ingresoMensual: Number(ingresoNum.toFixed(2)),
        domicilio,
      };
      await onSubmitUpdate(payload);
      return;
    }

    const basePayload: ClienteRequest = {
      nombre: nombre.trim(),
      ...(segundoNombre.trim() ? { segundoNombre: segundoNombre.trim() } : {}),
      apellidoPaterno: apellidoPaterno.trim(),
      apellidoMaterno: apellidoMaterno.trim(),
      fechaNacimiento,
      curp: curp.trim().toUpperCase(),
      rfc: rfc.trim().toUpperCase(),
      sexo,
      nacionalidad,
      estadoCivil,
      correo: correo.trim(),
      telefonoMovil: telefonoMovil.trim(),
      ...(telefonoAlternativo.trim() ? { telefonoAlternativo: telefonoAlternativo.trim() } : {}),
      ocupacion: ocupacion.trim(),
      empresa: empresa.trim(),
      ingresoMensual: Number(ingresoNum.toFixed(2)),
      domicilio,
      password,
    };

    if (isAdminCreate && onSubmitAdmin) {
      await onSubmitAdmin({ ...basePayload, rol });
    } else if (onSubmitPublic) {
      await onSubmitPublic(basePayload);
    }
  };

  const title = isEdit
    ? `Actualizar Cliente (${rfc})`
    : isAdminCreate
      ? 'Alta de Usuario por Administrador'
      : 'Registro de Nuevo Cliente';

  return (
    <div
      className="modal d-block"
      tabIndex={-1}
      style={{ backgroundColor: 'rgba(10, 22, 40, 0.72)' }}
      role="dialog"
    >
      <div className="modal-dialog modal-xl modal-dialog-scrollable modal-dialog-centered">
        <form className="modal-content pr-card border-0" onSubmit={handleFormSubmit}>
          <div className="modal-header border-bottom">
            <div>
              <h5 className="modal-title fw-bold mb-0">{title}</h5>
              <small className="text-body-secondary">
                Todos los campos cumplen con las validaciones del modelo y catálogos oficiales.
              </small>
            </div>
            <button type="button" className="btn-close" onClick={onCancel} aria-label="Cerrar" />
          </div>

          <div className="modal-body">
            {localError && (
              <div className="alert alert-danger py-2 small fw-medium">{localError}</div>
            )}

            {prefijoEsperado && (
              <div className="alert alert-info py-2 px-3 small d-flex align-items-center gap-2 mb-3">
                <i className="bi bi-shield-check fs-5" />
                <span>
                  Prefijo oficial calculado para RFC/CURP: <strong className="pr-account-number">{prefijoEsperado}</strong>
                </span>
              </div>
            )}

            {/* Datos personales */}
            <h6 className="text-uppercase text-primary fw-bold small mb-2">
              <i className="bi bi-person-vcard me-1" /> Datos Personales e Identidad
            </h6>
            <div className="row g-3 mb-4">
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Nombre *</label>
                <input
                  type="text"
                  className="form-control"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  minLength={3}
                  maxLength={50}
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Segundo Nombre</label>
                <input
                  type="text"
                  className="form-control"
                  value={segundoNombre}
                  onChange={(e) => setSegundoNombre(e.target.value)}
                  maxLength={50}
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Apellido Paterno *</label>
                <input
                  type="text"
                  className="form-control"
                  value={apellidoPaterno}
                  onChange={(e) => setApellidoPaterno(e.target.value)}
                  minLength={3}
                  maxLength={50}
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Apellido Materno *</label>
                <input
                  type="text"
                  className="form-control"
                  value={apellidoMaterno}
                  onChange={(e) => setApellidoMaterno(e.target.value)}
                  minLength={3}
                  maxLength={50}
                  required
                />
              </div>

              {!isEdit && (
                <div className="col-md-3">
                  <label className="form-label small fw-semibold">Fecha Nacimiento *</label>
                  <input
                    type="date"
                    className="form-control"
                    value={fechaNacimiento}
                    onChange={(e) => setFechaNacimiento(e.target.value)}
                    required
                  />
                </div>
              )}

              {!isEdit && (
                <div className="col-md-4">
                  <label className="form-label small fw-semibold">CURP (18 caracteres) *</label>
                  <input
                    type="text"
                    className="form-control text-uppercase pr-account-number"
                    value={curp}
                    onChange={(e) => setCurp(e.target.value.toUpperCase())}
                    minLength={18}
                    maxLength={18}
                    placeholder="AAAA000000HDFRRR01"
                    required
                  />
                </div>
              )}

              <div className="col-md-3">
                <label className="form-label small fw-semibold">RFC (12-13 caracteres) *</label>
                <input
                  type="text"
                  className="form-control text-uppercase pr-account-number"
                  value={rfc}
                  onChange={(e) => setRfc(e.target.value.toUpperCase())}
                  minLength={12}
                  maxLength={13}
                  readOnly={isEdit}
                  placeholder="AAAA000000ABC"
                  required
                />
              </div>

              <div className="col-md-2">
                <label className="form-label small fw-semibold">Sexo *</label>
                <select
                  className="form-select"
                  value={sexo}
                  onChange={(e) => setSexo(e.target.value as 'H' | 'M')}
                >
                  <option value="H">H (Hombre)</option>
                  <option value="M">M (Mujer)</option>
                </select>
              </div>

              <div className="col-md-3">
                <label className="form-label small fw-semibold">Nacionalidad *</label>
                <select
                  className="form-select"
                  value={nacionalidad}
                  onChange={(e) => setNacionalidad(e.target.value)}
                >
                  {CAT_NACIONALIDADES.map((nac) => (
                    <option key={nac} value={nac}>
                      {nac}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-md-3">
                <label className="form-label small fw-semibold">Estado Civil *</label>
                <select
                  className="form-select"
                  value={estadoCivil}
                  onChange={(e) => setEstadoCivil(e.target.value)}
                >
                  {ESTADOS_CIVILES.map((ec) => (
                    <option key={ec} value={ec}>
                      {ec}
                    </option>
                  ))}
                </select>
              </div>

              {isAdminCreate && (
                <div className="col-md-3">
                  <label className="form-label small fw-semibold">Rol del Usuario *</label>
                  <select
                    className="form-select"
                    value={rol}
                    onChange={(e) => setRol(Number(e.target.value) as 1 | 2)}
                  >
                    <option value={2}>2 - Usuario Normal (Cliente)</option>
                    <option value={1}>1 - Administrador</option>
                  </select>
                </div>
              )}
            </div>

            {/* Contacto y Laboral */}
            <h6 className="text-uppercase text-primary fw-bold small mb-2">
              <i className="bi bi-briefcase me-1" /> Contacto e Información Laboral
            </h6>
            <div className="row g-3 mb-4">
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Correo Electrónico *</label>
                <input
                  type="email"
                  className="form-control"
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                  maxLength={100}
                  required
                />
              </div>
              <div className="col-md-2">
                <label className="form-label small fw-semibold">Tel. Móvil (10) *</label>
                <input
                  type="tel"
                  className="form-control"
                  value={telefonoMovil}
                  onChange={(e) => setTelefonoMovil(e.target.value.replace(/\D/g, '').slice(0, 10))}
                  pattern="\d{10}"
                  required
                />
              </div>
              <div className="col-md-2">
                <label className="form-label small fw-semibold">Tel. Alternativo</label>
                <input
                  type="tel"
                  className="form-control"
                  value={telefonoAlternativo}
                  onChange={(e) =>
                    setTelefonoAlternativo(e.target.value.replace(/\D/g, '').slice(0, 10))
                  }
                  pattern="\d{10}"
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Ocupación *</label>
                <input
                  type="text"
                  className="form-control"
                  value={ocupacion}
                  onChange={(e) => setOcupacion(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Empresa *</label>
                <input
                  type="text"
                  className="form-control"
                  value={empresa}
                  onChange={(e) => setEmpresa(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Ingreso Mensual (MXN) *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-control"
                  value={ingresoMensual}
                  onChange={(e) => setIngresoMensual(e.target.value)}
                  required
                />
              </div>
              {!isEdit && (
                <div className="col-md-4">
                  <label className="form-label small fw-semibold">
                    Contraseña (mín. 8 caracteres) *
                  </label>
                  <input
                    type="password"
                    className="form-control"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                </div>
              )}
            </div>

            {/* Domicilio */}
            <h6 className="text-uppercase text-primary fw-bold small mb-2">
              <i className="bi bi-geo-alt me-1" /> Domicilio Fiscal
            </h6>
            <div className="row g-3">
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Calle *</label>
                <input
                  type="text"
                  className="form-control"
                  value={calle}
                  onChange={(e) => setCalle(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-2">
                <label className="form-label small fw-semibold">No. Exterior *</label>
                <input
                  type="text"
                  className="form-control"
                  value={numeroExterior}
                  onChange={(e) => setNumeroExterior(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-2">
                <label className="form-label small fw-semibold">No. Interior</label>
                <input
                  type="text"
                  className="form-control"
                  value={numeroInterior}
                  onChange={(e) => setNumeroInterior(e.target.value)}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-semibold">Colonia *</label>
                <input
                  type="text"
                  className="form-control"
                  value={colonia}
                  onChange={(e) => setColonia(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Municipio / Alcaldía *</label>
                <input
                  type="text"
                  className="form-control"
                  value={municipio}
                  onChange={(e) => setMunicipio(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Estado *</label>
                <input
                  type="text"
                  className="form-control"
                  value={estado}
                  onChange={(e) => setEstado(e.target.value)}
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">Código Postal (5) *</label>
                <input
                  type="text"
                  className="form-control"
                  value={codigoPostal}
                  onChange={(e) => setCodigoPostal(e.target.value.replace(/\D/g, '').slice(0, 5))}
                  pattern="\d{5}"
                  required
                />
              </div>
              <div className="col-md-3">
                <label className="form-label small fw-semibold">País *</label>
                <select
                  className="form-select"
                  value={pais}
                  onChange={(e) => setPais(e.target.value)}
                >
                  {CAT_PAISES.map((p) => (
                    <option key={p} value={p}>
                      {p}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <div className="modal-footer border-top">
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary px-4" disabled={submitting}>
              {submitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" />
                  Procesando...
                </>
              ) : isEdit ? (
                'Guardar Cambios'
              ) : (
                'Registrar Cliente'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
