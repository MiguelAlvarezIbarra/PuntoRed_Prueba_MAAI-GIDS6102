export interface GenericResponse {
  codigo: number;
  mensaje: string;
}

export interface ApiEnvelope<T> {
  codigo: number;
  mensaje: string;
  data: T | null;
}

export interface LoginRequest {
  correo: string;
  password: string;
}

export interface LoginResponse extends GenericResponse {
  clienteId: number;
  rol: number;
  jwt: string;
  expiraEnSegundos: number;
}

export interface DomicilioRequest {
  calle: string;
  numeroExterior: string;
  numeroInterior?: string;
  colonia: string;
  municipio: string;
  estado: string;
  codigoPostal: string;
  pais: string;
}

export interface ClienteRequest {
  nombre: string;
  segundoNombre?: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  curp: string;
  rfc: string;
  sexo: 'H' | 'M';
  nacionalidad: string;
  estadoCivil: string;
  correo: string;
  telefonoMovil: string;
  telefonoAlternativo?: string;
  ocupacion: string;
  empresa: string;
  ingresoMensual: number;
  domicilio: DomicilioRequest;
  password: string;
}

export interface AdminClienteRequest extends ClienteRequest {
  rol: 1 | 2;
}

export interface ClienteActualizaRequest {
  rfc: string;
  nombre: string;
  segundoNombre?: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  sexo: 'H' | 'M';
  nacionalidad: string;
  estadoCivil: string;
  correo: string;
  telefonoMovil: string;
  telefonoAlternativo?: string;
  ocupacion: string;
  empresa: string;
  ingresoMensual: number;
  domicilio: DomicilioRequest;
}

export interface ClienteData {
  nombre: string;
  segundoNombre?: string | null;
  apellidoPaterno: string;
  apellidoMaterno: string;
  fechaNacimiento: string;
  curp: string;
  rfc: string;
  sexo: 'H' | 'M';
  nacionalidad: string;
  estadoCivil: string;
  correo: string;
  telefonoMovil: string;
  telefonoAlternativo?: string | null;
  ocupacion: string;
  empresa: string;
  ingresoMensual: number;
  activo: boolean;
  fechaRegistro?: string;
  domicilio?: DomicilioRequest | null;
  numeroCuenta?: string | null;
}

export interface ClienteResponse extends GenericResponse {
  cliente: ClienteData;
}

export interface ListaClientesResponse extends GenericResponse {
  clientes: ClienteData[];
}

export interface ClienteSearchRequest {
  nombre?: string;
  rfc?: string;
  curp?: string;
  numeroCuenta?: string;
  correo?: string;
  fechaDesde?: string;
  fechaHasta?: string;
  activo?: boolean | null;
}

export interface ClienteBajaRequest {
  rfc: string;
  motivo?: string;
}

export interface ClienteReactivarRequest {
  rfc: string;
}

export interface CuentaData {
  id: number;
  clienteId: number;
  nombreCliente?: string;
  numeroCuenta: string;
  saldo: number;
  estatus: string;
  fechaApertura: string;
}

export interface CuentaResponse extends GenericResponse {
  cuenta: CuentaData;
}

export interface ListaCuentasResponse extends GenericResponse {
  cuentas: CuentaData[];
}

export interface SaldoResponse extends GenericResponse {
  numeroCuenta: string;
  saldo: number;
}

export interface CuentaConsultaRequest {
  numeroCuenta: string;
}

export interface CuentaAbonoRequest {
  numeroCuenta: string;
  monto: number;
}

export interface ProductoData {
  idProducto: number;
  idServicio: number;
  idCatTipoServicio: number;
  nombreProducto: string;
  nombreServicio: string;
  tipoFront: number;
  tipoReferencia: string;
  precio: string;
  fechaActualizacion?: string;
}

export interface SolicitudPin2faRequest {
  numeroCuenta: string;
}

export interface Pin2faResponse extends GenericResponse {
  numeroCuenta: string;
  correoDestino: string;
  expiraEnSegundos: number;
  pin2fa?: string;
}

export type TipoPago = 'TELEFONO' | 'REFERENCIA';

export interface PagoRequest {
  numeroCuenta: string;
  idServicio: number;
  idProducto: number;
  monto: number;
  tipoPago: TipoPago;
  telefono?: string;
  referencia?: string;
  pin2fa: string;
  upc?: string;
  unidad?: string;
}

export interface PagoData {
  idTransaccionLocal: number;
  numeroCuenta: string;
  idServicio: number;
  idProducto: number;
  tipoPago: TipoPago;
  telefonoReferencia: string;
  monto: number;
  saldoAnterior: number;
  saldoActual: number;
  upc: string;
  unidad: string;
  horaLocal: string;
  codigoGestoPago: string;
  mensajeGestoPago: string;
  idTxGestoPago?: string;
  numAutorizacion?: string;
  fechaGestoPago?: string;
  pin?: string;
  leyenda?: string;
}

export interface PagoResponse extends GenericResponse {
  pago?: PagoData;
}

export type AppSection =
  | 'dashboard'
  | 'clientes'
  | 'cuentas'
  | 'productos'
  | 'pagos'
  | 'historial';

export type TipoOperacionPuntored =
  | 'RECARGA'
  | 'PAQUETE'
  | 'RECAUDO'
  | 'RECAUDO_V2'
  | 'PIN';

export type EstadoTransaccion =
  | 'EXITOSA'
  | 'RECHAZADA'
  | 'PENDIENTE'
  | 'NO_CONFIRMADA';

export interface CatalogoBusquedaRequest {
  tipoOperacion?: string;
  categoria?: string;
  idCategoriaRecaudo?: string;
  idTipoPin?: string;
  idSubcategoriaPin?: string;
  texto?: string;
  forzarRecarga?: boolean;
}

export interface CatalogoCategoriaItem {
  idCategoria: string;
  nombreCategoria: string;
  tipoOperacion: string;
  logoUrl?: string | null;
  categoriaPadre?: string | null;
  totalElementos: number;
}

export interface CatalogoServicioItem {
  claveUnica: string;
  tipoOperacion: TipoOperacionPuntored;
  categoria: string;
  nombreServicio: string;
  nombreProducto: string;
  descripcion?: string | null;
  proveedorCodigo?: string | null;
  proveedorNombre?: string | null;
  logoUrl?: string | null;
  codigoProducto?: string | null;
  codigoCategoria?: string | null;
  idConvenio?: string | null;
  sku?: string | null;
  idPin?: string | null;
  idSubcategoriaPin?: string | null;
  precioSugerido?: number | null;
  validez?: string | null;
  etiquetaReferencia1?: string | null;
  etiquetaReferencia2?: string | null;
  tipoRecaudo?: number | null;
  nombreBanco?: string | null;
  requiereValidacionReferencia?: boolean | null;
  terminosUrl?: string | null;
}

export interface CatalogoServiciosResponse extends GenericResponse {
  fuenteDatos: string;
  saldoComercio?: string | null;
  categorias: CatalogoCategoriaItem[];
  servicios: CatalogoServicioItem[];
}

export interface ValidarReferenciaRequest {
  tipoOperacion: string;
  idConvenio?: string | null;
  codigoProducto?: string | null;
  codigoProveedor?: string | null;
  referencia1: string;
  referencia2?: string | null;
  montoReferencia?: number | null;
  codigoIac?: string | null;
  tipoCuenta?: string | null;
  tipoReferencia?: number | null;
}

export interface OpcionMontoConvenio {
  codigoRegistro?: string | null;
  descripcionRegistro?: string | null;
  monto?: number | null;
  tipoMonto?: number | null;
  montoMinimo?: number | null;
  montoMaximo?: number | null;
  requiereMonto?: boolean | null;
}

export interface ValidacionReferenciaResponse extends GenericResponse {
  referenciaValida: boolean;
  tokenValidacion?: string | null;
  tipoOperacion: string;
  idConvenio?: string | null;
  codigoProducto?: string | null;
  referencia1: string;
  referencia2?: string | null;
  codigoRespuestaConvenio?: string | null;
  mensajeConvenio?: string | null;
  montoSugerido?: number | null;
  montoMinimo?: number | null;
  montoMaximo?: number | null;
  montoEditable?: boolean | null;
  trace?: string | null;
  opciones?: OpcionMontoConvenio[] | null;
  camposAdicionalesAval?: Record<string, string | null> | null;
}

export interface TicketData {
  encabezado?: string | null;
  titulo?: string | null;
  cuerpo?: Record<string, unknown> | null;
  piePagina?: string | null;
  logoUrl?: string | null;
}

export interface PagoPuntoredRequest {
  numeroCuenta: string;
  tipoOperacion: string;
  nombreServicio: string;
  nombreProducto?: string | null;
  categoria?: string | null;
  logoUrl?: string | null;
  referencia1: string;
  referencia2?: string | null;
  monto: number;
  idConvenio?: string | null;
  codigoProveedor?: string | null;
  codigoCategoria?: string | null;
  codigoProducto?: string | null;
  sku?: string | null;
  idPin?: string | null;
  idSubcategoriaPin?: string | null;
  codigoRegistro?: string | null;
  codigoIac?: string | null;
  tipoCuenta?: string | null;
  tipoReferencia?: number | null;
  traceValidacion?: string | null;
  tokenValidacion: string;
  montoQuery?: number | null;
  aditionalValue54Eco?: string | null;
  aditionalValue62Eco?: string | null;
  accountDataAcredited?: string | null;
  additionalCompanyCode?: string | null;
  confirmacionExplicita: boolean;
  pin2fa: string;
}

export interface PagoPuntoredResponse extends GenericResponse {
  estado: EstadoTransaccion;
  transaccionConfirmada: boolean;
  idTransaccionLocal?: number | null;
  idTransaccionProveedor?: string | null;
  codigoAutorizacion?: string | null;
  trace?: string | null;
  pinContenido?: string | null;
  codigoRespuestaProveedor?: string | null;
  mensajeProveedor?: string | null;
  numeroCuenta: string;
  nombreServicio: string;
  nombreProducto?: string | null;
  categoria?: string | null;
  logoUrl?: string | null;
  referencia1: string;
  referencia2?: string | null;
  monto: number;
  saldoAnterior: number;
  saldoNuevo: number;
  ticket?: TicketData | null;
  fechaRegistro?: string | null;
}

export interface HistorialPagosRequest {
  numeroCuenta?: string;
  estado?: string;
  tipoOperacion?: string;
  limite?: number;
}

export interface HistorialPagoItem {
  id: number;
  numeroCuenta: string;
  proveedor: string;
  tipoOperacion: string;
  nombreServicio: string;
  nombreProducto?: string | null;
  categoria?: string | null;
  logoUrl?: string | null;
  referencia1: string;
  referencia2?: string | null;
  monto: number;
  idTransaccionProveedor?: string | null;
  codigoAutorizacion?: string | null;
  trace?: string | null;
  codigoRespuesta: string;
  mensajeRespuesta: string;
  estado: EstadoTransaccion;
  ticket?: TicketData | null;
  fechaRegistro: string;
}

export interface HistorialPagosResponse extends GenericResponse {
  total: number;
  transacciones: HistorialPagoItem[];
}

export interface DetallePagoRequest {
  idTransaccion: number;
  consultarEstadoProveedor?: boolean;
}

export interface DetallePagoResponse extends GenericResponse {
  transaccion: HistorialPagoItem;
  estadoActualizado: boolean;
}

export interface AuthSession {
  jwt: string;
  clienteId: number;
  rol: 1 | 2;
  correo: string;
  expiraEnSegundos: number;
  loginAt: number;
}
