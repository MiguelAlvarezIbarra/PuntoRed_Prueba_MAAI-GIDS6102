import { RENDER_API_URL } from '../constants/catalogs';
import type {
  AdminClienteRequest,
  ApiEnvelope,
  CatalogoBusquedaRequest,
  CatalogoServiciosResponse,
  ClienteActualizaRequest,
  ClienteBajaRequest,
  ClienteReactivarRequest,
  ClienteRequest,
  ClienteResponse,
  ClienteSearchRequest,
  CuentaAbonoRequest,
  CuentaConsultaRequest,
  CuentaResponse,
  DetallePagoRequest,
  DetallePagoResponse,
  GenericResponse,
  HistorialPagosRequest,
  HistorialPagosResponse,
  ListaClientesResponse,
  ListaCuentasResponse,
  LoginRequest,
  LoginResponse,
  PagoPuntoredRequest,
  PagoPuntoredResponse,
  PagoRequest,
  PagoResponse,
  Pin2faResponse,
  ProductoData,
  SaldoResponse,
  SolicitudPin2faRequest,
  ValidacionReferenciaResponse,
  ValidarReferenciaRequest,
} from '../types/api';

const STORAGE_BASE_URL_KEY = 'puntored_api_base_url';
const STORAGE_TOKEN_KEY = 'puntored_jwt_token';

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

export class ApiClientError extends Error {
  public readonly codigo: number;
  public readonly status: number;

  constructor(message: string, codigo = 99, status = 500) {
    super(message);
    this.name = 'ApiClientError';
    this.codigo = codigo;
    this.status = status;
  }
}

export function getApiBaseUrl(): string {
  const saved = localStorage.getItem(STORAGE_BASE_URL_KEY);
  const envDefault = import.meta.env.VITE_API_BASE_URL || RENDER_API_URL;
  return (saved || envDefault).replace(/\/+$/, '');
}

export function setApiBaseUrl(url: string): string {
  const clean = url.trim().replace(/\/+$/, '');
  localStorage.setItem(STORAGE_BASE_URL_KEY, clean);
  return clean;
}

export function getStoredToken(): string | null {
  return localStorage.getItem(STORAGE_TOKEN_KEY);
}

export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(STORAGE_TOKEN_KEY, token);
  } else {
    localStorage.removeItem(STORAGE_TOKEN_KEY);
  }
}

async function requestJson<T>(
  path: string,
  options: {
    method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
    body?: unknown;
    auth?: boolean;
    timeoutMs?: number;
  } = {},
): Promise<T> {
  const { method = 'GET', body, auth = true, timeoutMs = 45000 } = options;
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);

  const headers: Record<string, string> = {
    Accept: 'application/json',
  };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (auth) {
    const token = getStoredToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    const text = await response.text();
    let payload: unknown = null;
    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        payload = { codigo: response.status, mensaje: text };
      }
    }

    if (!response.ok) {
      const errObj = (payload ?? {}) as Partial<GenericResponse>;
      const codigo = errObj.codigo ?? response.status;
      if (auth && (response.status === 401 || codigo === 9) && path !== '/auth/logout') {
        unauthorizedHandler?.();
      }
      throw new ApiClientError(
        errObj.mensaje || `Error HTTP ${response.status}`,
        codigo,
        response.status,
      );
    }

    return payload as T;
  } catch (err) {
    if (err instanceof ApiClientError) {
      throw err;
    }
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new ApiClientError(
        'Tiempo de espera agotado al conectar con el servidor (si Render estaba dormido, intenta de nuevo en unos segundos).',
        21,
        504,
      );
    }
    throw new ApiClientError(
      'No fue posible conectar con el servidor API. Verifica tu conexión o la URL del entorno.',
      22,
      503,
    );
  } finally {
    window.clearTimeout(timer);
  }
}

export const api = {
  // Auth
  login: (payload: LoginRequest) =>
    requestJson<LoginResponse>('/auth/login', { method: 'POST', body: payload, auth: false }),

  logout: () =>
    requestJson<GenericResponse>('/auth/logout', { method: 'POST', auth: true }),

  // Clientes
  registrarClientePublico: (payload: ClienteRequest) =>
    requestJson<ClienteResponse>('/clientes', { method: 'POST', body: payload, auth: false }),

  registrarClienteAdmin: (payload: AdminClienteRequest) =>
    requestJson<ClienteResponse>('/admin/clientes', { method: 'POST', body: payload, auth: true }),

  buscarClientes: (payload: ClienteSearchRequest) =>
    requestJson<ListaClientesResponse>('/clientes/search', { method: 'POST', body: payload, auth: true }),

  actualizarCliente: (payload: ClienteActualizaRequest) =>
    requestJson<ClienteResponse>('/clientes', { method: 'PUT', body: payload, auth: true }),

  darBajaCliente: (payload: ClienteBajaRequest) =>
    requestJson<GenericResponse>('/clientes', { method: 'DELETE', body: payload, auth: true }),

  reactivarCliente: (payload: ClienteReactivarRequest) =>
    requestJson<GenericResponse>('/clientes/reactivar', { method: 'PATCH', body: payload, auth: true }),

  // Cuentas
  obtenerMisCuentas: () =>
    requestJson<ListaCuentasResponse>('/cuentas/mias', { method: 'GET', auth: true }),

  obtenerCuentasActivas: () =>
    requestJson<ListaCuentasResponse>('/cuentas/activas', { method: 'GET', auth: true }),

  consultarCuenta: (payload: CuentaConsultaRequest) =>
    requestJson<CuentaResponse>('/cuentas/consulta', { method: 'POST', body: payload, auth: true }),

  consultarSaldo: (payload: CuentaConsultaRequest) =>
    requestJson<SaldoResponse>('/cuentas/saldo', { method: 'POST', body: payload, auth: true }),

  abonarSaldo: (payload: CuentaAbonoRequest) =>
    requestJson<SaldoResponse>('/cuentas/abonar', { method: 'POST', body: payload, auth: true }),

  // Catalogo dinamico PuntoRed H2H V2 y productos locales
  obtenerCatalogoPuntored: () =>
    requestJson<CatalogoServiciosResponse>('/api/catalogo', { method: 'GET', auth: true }),

  buscarCatalogoPuntored: (payload: CatalogoBusquedaRequest) =>
    requestJson<CatalogoServiciosResponse>('/api/catalogo/buscar', {
      method: 'POST',
      body: payload,
      auth: true,
    }),

  obtenerProductos: () =>
    requestJson<ApiEnvelope<ProductoData[]>>('/api/productos', { method: 'GET', auth: true }),

  sincronizarProductos: () =>
    requestJson<ApiEnvelope<number>>('/api/productos/sync', { method: 'POST', auth: true }),

  // Pagos PuntoRed H2H V2, validacion de referencia, 2FA e historial
  enviarPin2fa: (payload: SolicitudPin2faRequest) =>
    requestJson<Pin2faResponse>('/pagos/enviar-pin', { method: 'POST', body: payload, auth: true }),

  validarReferencia: (payload: ValidarReferenciaRequest) =>
    requestJson<ValidacionReferenciaResponse>('/pagos/validar-referencia', {
      method: 'POST',
      body: payload,
      auth: true,
    }),

  realizarPagoPuntored: (payload: PagoPuntoredRequest) =>
    requestJson<PagoPuntoredResponse>('/pagos/puntored', { method: 'POST', body: payload, auth: true }),

  consultarHistorialPagos: (payload: HistorialPagosRequest = {}) =>
    requestJson<HistorialPagosResponse>('/pagos/historial', { method: 'POST', body: payload, auth: true }),

  consultarDetallePago: (payload: DetallePagoRequest) =>
    requestJson<DetallePagoResponse>('/pagos/detalle', { method: 'POST', body: payload, auth: true }),

  // Legacy SenTx
  realizarPago: (payload: PagoRequest) =>
    requestJson<PagoResponse>('/pagos', { method: 'POST', body: payload, auth: true }),
};
