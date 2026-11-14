import React, { useState } from 'react';

interface ServiceLogoProps {
  logoUrl?: string | null;
  name: string;
  tipoOperacion?: string;
  size?: number;
}

function resolverIcono(tipo?: string): string {
  switch (tipo?.toUpperCase()) {
    case 'RECARGA':
      return 'bi-phone-fill';
    case 'PAQUETE':
      return 'bi-wifi';
    case 'RECAUDO':
    case 'RECAUDO_V2':
      return 'bi-receipt-cutoff';
    case 'PIN':
      return 'bi-controller';
    default:
      return 'bi-building-check';
  }
}

export const ServiceLogo: React.FC<ServiceLogoProps> = ({
  logoUrl,
  name,
  tipoOperacion,
  size = 44,
}) => {
  const [imgError, setImgError] = useState(false);

  if (logoUrl && !imgError) {
    return (
      <div
        className="rounded-3 bg-white border d-flex align-items-center justify-content-center flex-shrink-0 overflow-hidden p-1 shadow-xs"
        style={{ width: size, height: size }}
      >
        <img
          src={logoUrl}
          alt={name}
          onError={() => setImgError(true)}
          style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
        />
      </div>
    );
  }

  return (
    <div
      className="rounded-3 bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 d-flex align-items-center justify-content-center flex-shrink-0"
      style={{ width: size, height: size }}
      title={name}
    >
      <i className={`bi ${resolverIcono(tipoOperacion)} fs-5`} />
    </div>
  );
};
