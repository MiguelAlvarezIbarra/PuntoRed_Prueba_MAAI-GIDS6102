import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { ROLES } from '../constants/catalogs';
import {
  api,
  getApiBaseUrl,
  setApiBaseUrl,
  setStoredToken,
  setUnauthorizedHandler,
} from '../services/apiClient';
import type { AuthSession, LoginRequest } from '../types/api';

const STORAGE_SESSION_KEY = 'puntored_auth_session';
const STORAGE_THEME_KEY = 'puntored_theme_mode';

export type ThemeMode = 'light' | 'dark';

interface AuthContextValue {
  session: AuthSession | null;
  isAdmin: boolean;
  theme: ThemeMode;
  apiBaseUrl: string;
  toggleTheme: () => void;
  updateApiBaseUrl: (url: string) => void;
  login: (credentials: LoginRequest) => Promise<AuthSession>;
  logout: () => Promise<void>;
  clearLocalSession: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_SESSION_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as AuthSession;
    if (!parsed?.jwt) {
      return null;
    }
    setStoredToken(parsed.jwt);
    return parsed;
  } catch {
    return null;
  }
}

function readInitialTheme(): ThemeMode {
  const stored = localStorage.getItem(STORAGE_THEME_KEY);
  return stored === 'dark' ? 'dark' : 'light';
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [session, setSession] = useState<AuthSession | null>(() => readStoredSession());
  const [theme, setTheme] = useState<ThemeMode>(() => readInitialTheme());
  const [apiBaseUrl, setApiBaseUrlState] = useState<string>(() => getApiBaseUrl());

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem(STORAGE_THEME_KEY, theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  }, []);

  const updateApiBaseUrl = useCallback((url: string) => {
    const clean = setApiBaseUrl(url);
    setApiBaseUrlState(clean);
  }, []);

  const clearLocalSession = useCallback(() => {
    setStoredToken(null);
    localStorage.removeItem(STORAGE_SESSION_KEY);
    setSession(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => clearLocalSession());
    return () => setUnauthorizedHandler(null);
  }, [clearLocalSession]);

  useEffect(() => {
    if (!session) {
      return;
    }
    const elapsedMs = Date.now() - session.loginAt;
    const ttlMs = (session.expiraEnSegundos || 900) * 1000;
    const remaining = ttlMs - elapsedMs;
    if (remaining <= 0) {
      clearLocalSession();
      return;
    }
    const timer = window.setTimeout(() => clearLocalSession(), remaining);
    return () => window.clearTimeout(timer);
  }, [session, clearLocalSession]);

  const login = useCallback(async (credentials: LoginRequest): Promise<AuthSession> => {
    const resp = await api.login(credentials);
    const nextSession: AuthSession = {
      jwt: resp.jwt,
      clienteId: resp.clienteId,
      rol: resp.rol === ROLES.ADMIN ? 1 : 2,
      correo: credentials.correo,
      expiraEnSegundos: resp.expiraEnSegundos || 900,
      loginAt: Date.now(),
    };
    setStoredToken(nextSession.jwt);
    localStorage.setItem(STORAGE_SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
    return nextSession;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // Si el token ya expiro, de todas formas limpiamos la sesion local.
    } finally {
      clearLocalSession();
    }
  }, [clearLocalSession]);

  return (
    <AuthContext.Provider
      value={{
        session,
        isAdmin: session?.rol === ROLES.ADMIN,
        theme,
        apiBaseUrl,
        toggleTheme,
        updateApiBaseUrl,
        login,
        logout,
        clearLocalSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return ctx;
}
