import React, { useState } from 'react';
import { AlertBanner, type AlertBannerState } from '../components/AlertBanner';
import { ClienteFormModal } from '../components/ClienteFormModal';
import { EnvironmentSelectorModal } from '../components/EnvironmentSelectorModal';
import { useAuth } from '../context/AuthContext';
import { api, ApiClientError } from '../services/apiClient';
import type { ClienteRequest } from '../types/api';
import { formatAccountNumber } from '../utils/formatters';

export const AuthPage: React.FC = () => {
  const { login, theme, toggleTheme, apiBaseUrl } = useAuth();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [showEnvModal, setShowEnvModal] = useState(false);
  const [alert, setAlert] = useState<AlertBannerState | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setAlert(null);
    setLoading(true);
    try {
      await login({ correo: correo.trim(), password });
    } catch (err) {
      const apiErr = err instanceof ApiClientError ? err : null;
      setAlert({
        type: 'danger',
        title: 'No se pudo iniciar sesión',
        codigo: apiErr?.codigo,
        message: err instanceof Error ? err.message : 'Error inesperado de autenticación.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePublicRegister = async (payload: ClienteRequest) => {
    setAlert(null);
    setLoading(true);
    try {
      const res = await api.registrarClientePublico(payload);
      setShowRegister(false);
      setCorreo(payload.correo);
      setAlert({
        type: 'success',
        title: 'Registro completado exitosamente',
        message: `Cuenta asignada: ${formatAccountNumber(res.cliente?.numeroCuenta)}. Ya puedes iniciar sesión con tu correo y contraseña.`,
      });
    } catch (err) {
      const apiErr = err instanceof ApiClientError ? err : null;
      setAlert({
        type: 'danger',
        title: 'Error en registro de cliente',
        codigo: apiErr?.codigo,
        message: err instanceof Error ? err.message : 'No fue posible registrar el cliente.',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pr-auth-wrapper d-flex align-items-center justify-content-center p-3 p-md-4">
      <div className="container" style={{ maxWidth: 1020 }}>
        <div className="row g-0 pr-card overflow-hidden border-0 shadow-lg">
          {/* Panel Corporativo Izquierdo */}
          <div className="col-lg-5 pr-auth-brand-panel p-4 p-lg-5 d-flex flex-column justify-content-between">
            <div>
              <div className="d-flex align-items-center gap-2 mb-4">
                <span className="pr-kpi-icon bg-primary text-white">
                  <i className="bi bi-bank2" />
                </span>
                <div>
                  <div className="fw-bold fs-5 brand-font text-white">PuntoRed</div>
                  <div className="small text-white-50">Core Financiero &amp; Pagos GestoPago</div>
                </div>
              </div>

              <span className="badge pr-gold-pill px-3 py-2 rounded-pill mb-3">
                <i className="bi bi-shield-lock me-1" /> Acceso Corporativo &amp; 2FA
              </span>

              <h2 className="fw-bold text-white mb-3">
                Gestión integral de clientes, cuentas y pagos en tiempo real.
              </h2>
              <p className="text-white-50 small lh-lg mb-4">
                Plataforma diseñada para operar desde escritorio web o dispositivo móvil Android con
                cifrado AES-GCM, validación JWT por sesión activa y verificación 2FA por correo en
                operaciones SenTx.
              </p>
            </div>

            <div className="border-top border-white border-opacity-10 pt-3 small text-white-50">
              <div className="d-flex align-items-center gap-2 mb-1">
                <i className="bi bi-check-circle-fill text-success" />
                <span>Cuentas bancarias de 18 dígitos</span>
              </div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <i className="bi bi-check-circle-fill text-success" />
                <span>Búsquedas dinámicas JPA multi-filtro</span>
              </div>
              <div className="d-flex align-items-center gap-2">
                <i className="bi bi-check-circle-fill text-success" />
                <span>Integración en vivo con GestoPago</span>
              </div>
            </div>
          </div>

          {/* Formulario de Acceso */}
          <div className="col-lg-7 p-4 p-lg-5">
            <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
              <div>
                <h4 className="fw-bold mb-1">Iniciar Sesión</h4>
                <p className="text-body-secondary small mb-0">
                  Ingresa tus credenciales registradas en la plataforma.
                </p>
              </div>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setShowEnvModal(true)}
                  title="Configurar servidor API"
                >
                  <i className="bi bi-hdd-network me-1" />
                  API
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={toggleTheme}
                  title="Cambiar tema claro / oscuro"
                >
                  <i className={`bi ${theme === 'dark' ? 'bi-sun' : 'bi-moon-stars'}`} />
                </button>
              </div>
            </div>

            <AlertBanner alert={alert} onDismiss={() => setAlert(null)} />

            <form onSubmit={handleLogin}>
              <div className="mb-3">
                <label className="form-label fw-semibold small">Correo Electrónico</label>
                <div className="input-group">
                  <span className="input-group-text">
                    <i className="bi bi-envelope" />
                  </span>
                  <input
                    type="email"
                    className="form-control form-control-lg"
                    placeholder="cliente@correo.com"
                    value={correo}
                    onChange={(e) => setCorreo(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="mb-4">
                <label className="form-label fw-semibold small">Contraseña</label>
                <div className="input-group">
                  <span className="input-group-text">
                    <i className="bi bi-key" />
                  </span>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="form-control form-control-lg"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setShowPassword((prev) => !prev)}
                  >
                    <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} />
                  </button>
                </div>
              </div>

              <div className="d-grid gap-2">
                <button
                  type="submit"
                  className="btn btn-primary btn-lg fw-semibold"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" />
                      Autenticando...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-box-arrow-in-right me-2" />
                      Ingresar al Portal
                    </>
                  )}
                </button>

                <button
                  type="button"
                  className="btn btn-outline-primary fw-semibold"
                  onClick={() => setShowRegister(true)}
                >
                  <i className="bi bi-person-plus me-2" />
                  Crear Cuenta de Cliente Nueva
                </button>
              </div>
            </form>

            <div className="mt-4 pt-3 border-top d-flex align-items-center justify-content-between flex-wrap gap-2 small text-body-secondary">
              <span>
                <i className="bi bi-globe2 me-1" />
                Servidor activo: <code className="text-primary">{apiBaseUrl}</code>
              </span>
              <button
                type="button"
                className="btn btn-link btn-sm p-0 text-decoration-none"
                onClick={() => setShowEnvModal(true)}
              >
                Cambiar entorno
              </button>
            </div>
          </div>
        </div>
      </div>

      {showRegister && (
        <ClienteFormModal
          mode="public-register"
          submitting={loading}
          onCancel={() => setShowRegister(false)}
          onSubmitPublic={handlePublicRegister}
        />
      )}

      <EnvironmentSelectorModal show={showEnvModal} onClose={() => setShowEnvModal(false)} />
    </div>
  );
};
