import React from 'react';

export interface AlertBannerState {
  type: 'success' | 'danger' | 'warning' | 'info';
  title: string;
  message: string;
  codigo?: number;
}

interface AlertBannerProps {
  alert: AlertBannerState | null;
  onDismiss: () => void;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({ alert, onDismiss }) => {
  if (!alert) {
    return null;
  }

  const iconMap: Record<AlertBannerState['type'], string> = {
    success: 'bi-check-circle-fill',
    danger: 'bi-exclamation-octagon-fill',
    warning: 'bi-exclamation-triangle-fill',
    info: 'bi-info-circle-fill',
  };

  return (
    <div
      className={`alert alert-${alert.type} alert-dismissible fade show d-flex align-items-start gap-2 shadow-sm`}
      role="alert"
    >
      <i className={`bi ${iconMap[alert.type]} fs-5 mt-1`} />
      <div className="flex-grow-1">
        <div className="fw-bold d-flex align-items-center gap-2 flex-wrap">
          <span>{alert.title}</span>
          {alert.codigo !== undefined && (
            <span className="badge text-bg-dark bg-opacity-75">Código {alert.codigo}</span>
          )}
        </div>
        <div className="small mt-1">{alert.message}</div>
      </div>
      <button type="button" className="btn-close" onClick={onDismiss} aria-label="Cerrar" />
    </div>
  );
};
