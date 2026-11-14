import React, { useState } from 'react';
import { LOCAL_API_URL, RENDER_API_URL } from '../constants/catalogs';
import { useAuth } from '../context/AuthContext';

interface EnvironmentSelectorModalProps {
  show: boolean;
  onClose: () => void;
}

export const EnvironmentSelectorModal: React.FC<EnvironmentSelectorModalProps> = ({
  show,
  onClose,
}) => {
  const { apiBaseUrl, updateApiBaseUrl } = useAuth();
  const [customUrl, setCustomUrl] = useState(apiBaseUrl);
  const [error, setError] = useState('');

  if (!show) {
    return null;
  }

  const handleSave = (urlToSave: string) => {
    const trimmed = urlToSave.trim();
    if (!/^https?:\/\/.+/.test(trimmed)) {
      setError('Ingresa una URL válida que inicie con http:// o https://');
      return;
    }
    setError('');
    updateApiBaseUrl(trimmed);
    onClose();
  };

  return (
    <div
      className="modal d-block"
      tabIndex={-1}
      style={{ backgroundColor: 'rgba(10, 22, 40, 0.65)' }}
      role="dialog"
    >
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content pr-card border-0">
          <div className="modal-header border-bottom">
            <h5 className="modal-title fw-bold d-flex align-items-center gap-2">
              <i className="bi bi-hdd-network text-primary" />
              Entorno de Conexión API
            </h5>
            <button type="button" className="btn-close" onClick={onClose} aria-label="Cerrar" />
          </div>
          <div className="modal-body">
            <p className="text-body-secondary small mb-3">
              Elige a qué servidor Spring Boot se conectará la aplicación web o el APK móvil.
            </p>

            <div className="d-grid gap-2 mb-3">
              <button
                type="button"
                className={`btn text-start d-flex justify-content-between align-items-center ${
                  apiBaseUrl === RENDER_API_URL ? 'btn-primary' : 'btn-outline-secondary'
                }`}
                onClick={() => {
                  setCustomUrl(RENDER_API_URL);
                  handleSave(RENDER_API_URL);
                }}
              >
                <div>
                  <div className="fw-semibold">Producción (Render Cloud)</div>
                  <div className="small opacity-75">{RENDER_API_URL}</div>
                </div>
                <span className="badge text-bg-light">Recomendado APK</span>
              </button>

              <button
                type="button"
                className={`btn text-start d-flex justify-content-between align-items-center ${
                  apiBaseUrl === LOCAL_API_URL ? 'btn-primary' : 'btn-outline-secondary'
                }`}
                onClick={() => {
                  setCustomUrl(LOCAL_API_URL);
                  handleSave(LOCAL_API_URL);
                }}
              >
                <div>
                  <div className="fw-semibold">Desarrollo Local (PC)</div>
                  <div className="small opacity-75">{LOCAL_API_URL}</div>
                </div>
                <span className="badge text-bg-secondary">Local</span>
              </button>
            </div>

            <label className="form-label fw-semibold small">URL personalizada</label>
            <div className="input-group">
              <input
                type="url"
                className="form-control"
                value={customUrl}
                onChange={(e) => setCustomUrl(e.target.value)}
                placeholder="https://..."
              />
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => handleSave(customUrl)}
              >
                Aplicar
              </button>
            </div>
            {error && <div className="text-danger small mt-2">{error}</div>}
          </div>
        </div>
      </div>
    </div>
  );
};
