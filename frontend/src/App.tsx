import React, { useState } from 'react';
import { EnvironmentSelectorModal } from './components/EnvironmentSelectorModal';
import { useAuth } from './context/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { ClientesPage } from './pages/ClientesPage';
import { CuentasPage } from './pages/CuentasPage';
import { DashboardPage } from './pages/DashboardPage';
import { HistorialPage } from './pages/HistorialPage';
import { PagosPage } from './pages/PagosPage';
import { ProductosPage } from './pages/ProductosPage';
import type { AppSection, CatalogoServicioItem } from './types/api';

interface NavItem {
  id: AppSection;
  label: string;
  shortLabel: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Resumen Ejecutivo', shortLabel: 'Inicio', icon: 'bi-speedometer2' },
  { id: 'productos', label: 'Catálogo PuntoRed', shortLabel: 'Catálogo', icon: 'bi-grid-3x3-gap' },
  { id: 'pagos', label: 'Pagar Servicio', shortLabel: 'Pagar', icon: 'bi-shield-check' },
  { id: 'historial', label: 'Historial de Pagos', shortLabel: 'Historial', icon: 'bi-clock-history' },
  { id: 'cuentas', label: 'Cuentas & Saldos', shortLabel: 'Cuentas', icon: 'bi-wallet2' },
  { id: 'clientes', label: 'Clientes / Admin', shortLabel: 'Clientes', icon: 'bi-people' },
];

const AppShell: React.FC = () => {
  const { session, isAdmin, theme, apiBaseUrl, toggleTheme, logout } = useAuth();
  const [section, setSection] = useState<AppSection>('dashboard');
  const [prefillCuenta, setPrefillCuenta] = useState<string | undefined>(undefined);
  const [prefillServicio, setPrefillServicio] = useState<CatalogoServicioItem | null>(null);
  const [showEnvModal, setShowEnvModal] = useState(false);

  if (!session) {
    return <AuthPage />;
  }

  const handleNavigate = (target: AppSection, cuenta?: string) => {
    if (cuenta) {
      setPrefillCuenta(cuenta);
    }
    setSection(target);
  };

  const handleSelectServicioParaPago = (servicio: CatalogoServicioItem) => {
    setPrefillServicio(servicio);
    setSection('pagos');
  };

  return (
    <div className="pr-app-shell">
      {/* Sidebar Desktop */}
      <aside className="pr-sidebar d-none d-lg-flex p-3">
        <div className="d-flex align-items-center gap-2 px-2 py-2 mb-4">
          <span className="pr-kpi-icon bg-primary text-white">
            <i className="bi bi-bank2" />
          </span>
          <div>
            <div className="fw-bold fs-5 text-white brand-font">PuntoRed</div>
            <div className="small text-white-50">Portal Financiero</div>
          </div>
        </div>

        <nav className="nav flex-column gap-1 flex-grow-1">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-link text-start ${section === item.id ? 'active' : ''}`}
              onClick={() => handleNavigate(item.id)}
            >
              <i className={`bi ${item.icon}`} />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="border-top border-white border-opacity-10 pt-3 mt-3 small">
          <div className="px-2 mb-2">
            <div className="text-white fw-semibold text-truncate">{session.correo}</div>
            <div className="d-flex align-items-center gap-2 mt-1">
              <span className={`badge ${isAdmin ? 'pr-gold-pill' : 'text-bg-primary'}`}>
                {isAdmin ? 'ADMIN (Rol 1)' : 'CLIENTE (Rol 2)'}
              </span>
              <span className="text-white-50">ID #{session.clienteId}</span>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-outline-light btn-sm w-100 mt-2"
            onClick={() => void logout()}
          >
            <i className="bi bi-box-arrow-right me-2" />
            Cerrar Sesión
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="pr-main">
        <header className="pr-topbar d-flex justify-content-between align-items-center gap-2">
          <div className="d-flex align-items-center gap-2">
            <span className="d-lg-none pr-kpi-icon bg-primary text-white" style={{ width: 34, height: 34, fontSize: '1rem' }}>
              <i className="bi bi-bank2" />
            </span>
            <div>
              <div className="fw-bold d-lg-none brand-font">PuntoRed</div>
              <div className="fw-semibold d-none d-lg-block">
                {NAV_ITEMS.find((i) => i.id === section)?.label}
              </div>
              <div className="small text-body-secondary d-none d-sm-block">
                API: <span className="text-primary">{apiBaseUrl}</span>
              </div>
            </div>
          </div>

          <div className="d-flex align-items-center gap-2">
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => setShowEnvModal(true)}
              title="Cambiar servidor API"
            >
              <i className="bi bi-hdd-network" />
              <span className="d-none d-md-inline ms-1">Servidor</span>
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={toggleTheme}
              title="Cambiar tema"
            >
              <i className={`bi ${theme === 'dark' ? 'bi-sun' : 'bi-moon-stars'}`} />
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline-danger d-lg-none"
              onClick={() => void logout()}
              title="Cerrar sesión"
            >
              <i className="bi bi-box-arrow-right" />
            </button>
          </div>
        </header>

        <main className="pr-content-area flex-grow-1">
          {section === 'dashboard' && <DashboardPage onNavigate={handleNavigate} />}
          {section === 'clientes' && (
            <ClientesPage
              onGoToCuenta={(cta) => handleNavigate('cuentas', cta)}
              onGoToPago={(cta) => handleNavigate('pagos', cta)}
            />
          )}
          {section === 'cuentas' && (
            <CuentasPage
              prefillCuenta={prefillCuenta}
              onGoToPago={(cta) => handleNavigate('pagos', cta)}
            />
          )}
          {section === 'productos' && (
            <ProductosPage onSelectServicioParaPago={handleSelectServicioParaPago} />
          )}
          {section === 'pagos' && (
            <PagosPage
              prefillCuenta={prefillCuenta}
              prefillServicio={prefillServicio}
              onNavigate={handleNavigate}
            />
          )}
          {section === 'historial' && <HistorialPage />}
        </main>
      </div>

      {/* Bottom Navigation Móvil / APK */}
      <nav className="pr-bottom-nav d-lg-none" aria-label="Navegación móvil">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`pr-bottom-nav-btn ${section === item.id ? 'active' : ''}`}
            onClick={() => handleNavigate(item.id)}
          >
            <i className={`bi ${item.icon}`} />
            <span>{item.shortLabel}</span>
          </button>
        ))}
      </nav>

      <EnvironmentSelectorModal show={showEnvModal} onClose={() => setShowEnvModal(false)} />
    </div>
  );
};

export default function App() {
  return <AppShell />;
}
