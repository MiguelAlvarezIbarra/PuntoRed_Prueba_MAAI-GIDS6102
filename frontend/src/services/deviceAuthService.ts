const STORAGE_DEVICE_2FA_KEY = 'puntored_web_device_2fa_v1';

interface StoredDevice2faState {
  enabled: boolean;
  email: string;
  credentialId: string;
  rpId: string;
}

export interface DeviceAuthStatus {
  supported: boolean;
  secureContext: boolean;
  platformAvailable: boolean;
  enabled: boolean;
  linkedEmail: string | null;
  hasCredential: boolean;
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function fromBase64Url(value: string): Uint8Array {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/');
  const padLen = (4 - (padded.length % 4)) % 4;
  const base64 = padded + '='.repeat(padLen);
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function readStoredState(): StoredDevice2faState | null {
  try {
    const raw = localStorage.getItem(STORAGE_DEVICE_2FA_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredDevice2faState;
    if (!parsed?.credentialId || !parsed?.email) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeStoredState(state: StoredDevice2faState | null): void {
  if (!state) {
    localStorage.removeItem(STORAGE_DEVICE_2FA_KEY);
    return;
  }
  localStorage.setItem(STORAGE_DEVICE_2FA_KEY, JSON.stringify(state));
}

function getRpId(): string {
  const host = window.location.hostname || 'localhost';
  return host === '127.0.0.1' ? 'localhost' : host;
}

export async function getDeviceAuthStatus(): Promise<DeviceAuthStatus> {
  const secureContext = typeof window !== 'undefined' && Boolean(window.isSecureContext);
  const supported =
    typeof window !== 'undefined' &&
    secureContext &&
    typeof window.PublicKeyCredential !== 'undefined' &&
    Boolean(navigator.credentials);

  let platformAvailable = false;
  if (supported && typeof PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === 'function') {
    try {
      platformAvailable = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    } catch {
      platformAvailable = false;
    }
  }

  const stored = readStoredState();
  const currentRp = typeof window !== 'undefined' ? getRpId() : 'localhost';
  const sameRp = stored?.rpId === currentRp;

  return {
    supported,
    secureContext,
    platformAvailable,
    enabled: Boolean(stored?.enabled && sameRp && platformAvailable),
    linkedEmail: sameRp ? stored?.email ?? null : null,
    hasCredential: Boolean(stored?.credentialId && sameRp),
  };
}

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

function mapWebAuthnError(err: unknown): Error {
  if (err instanceof DOMException) {
    if (err.name === 'NotAllowedError' || err.name === 'AbortError') {
      return new Error('Verificación del equipo cancelada o tiempo agotado.');
    }
    if (err.name === 'SecurityError') {
      return new Error('El navegador requiere HTTPS o localhost para verificar con seguridad del equipo.');
    }
    if (err.name === 'NotSupportedError') {
      return new Error('Este equipo no tiene configurado PIN, contraseña o biometría de Windows Hello.');
    }
  }
  return err instanceof Error
    ? err
    : new Error('No se pudo completar la verificación del dispositivo.');
}

async function enrollPlatformCredential(email: string, keepEnabled: boolean): Promise<StoredDevice2faState> {
  const cleanEmail = normalizeEmail(email);
  const rpId = getRpId();
  const challenge = crypto.getRandomValues(new Uint8Array(32));
  const userId = new TextEncoder().encode(cleanEmail).slice(0, 64);

  let credential: Credential | null;
  try {
    credential = await navigator.credentials.create({
      publicKey: {
        challenge,
        rp: {
          name: 'PuntoRed H2H V2',
          id: rpId,
        },
        user: {
          id: userId,
          name: cleanEmail,
          displayName: cleanEmail,
        },
        pubKeyCredParams: [
          { type: 'public-key', alg: -7 },
          { type: 'public-key', alg: -257 },
        ],
        authenticatorSelection: {
          authenticatorAttachment: 'platform',
          userVerification: 'required',
          residentKey: 'preferred',
        },
        timeout: 60000,
        attestation: 'none',
      },
    });
  } catch (err) {
    throw mapWebAuthnError(err);
  }

  if (!credential || !(credential instanceof PublicKeyCredential)) {
    throw new Error('No se obtuvo credencial válida de seguridad del equipo.');
  }

  const next: StoredDevice2faState = {
    enabled: keepEnabled,
    email: cleanEmail,
    credentialId: toBase64Url(new Uint8Array(credential.rawId)),
    rpId,
  };
  writeStoredState(next);
  return next;
}

/**
 * Solicita al navegador la verificación nativa del sistema operativo
 * (contraseña/PIN de Windows, Windows Hello, Touch ID o biometría móvil) mediante WebAuthn.
 */
export async function verifyWithDevicePrompt(
  email: string,
  options: { enable2faPreference?: boolean } = {},
): Promise<void> {
  const status = await getDeviceAuthStatus();
  if (!status.supported || !status.platformAvailable) {
    throw new Error(
      status.secureContext
        ? 'Configura primero PIN, huella o reconocimiento facial en tu computadora (Windows Hello / Touch ID).'
        : 'La verificación del equipo requiere ejecutar la web por HTTPS o localhost.',
    );
  }

  const cleanEmail = normalizeEmail(email);
  const currentRp = getRpId();
  const stored = readStoredState();
  const keepEnabled = options.enable2faPreference ?? stored?.enabled ?? true;

  if (!stored || stored.rpId !== currentRp || normalizeEmail(stored.email) !== cleanEmail) {
    await enrollPlatformCredential(cleanEmail, keepEnabled);
    return;
  }

  const challenge = crypto.getRandomValues(new Uint8Array(32));
  const expectedChallengeB64 = toBase64Url(challenge);

  let assertion: Credential | null;
  try {
    const credIdBytes = fromBase64Url(stored.credentialId);
    assertion = await navigator.credentials.get({
      publicKey: {
        challenge,
        rpId: currentRp,
        allowCredentials: [
          {
            type: 'public-key',
            id: credIdBytes.buffer.slice(
              credIdBytes.byteOffset,
              credIdBytes.byteOffset + credIdBytes.byteLength,
            ) as ArrayBuffer,
            transports: ['internal'],
          },
        ],
        userVerification: 'required',
        timeout: 60000,
      },
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'InvalidStateError') {
      await enrollPlatformCredential(cleanEmail, keepEnabled);
      return;
    }
    throw mapWebAuthnError(err);
  }

  if (!assertion || !(assertion instanceof PublicKeyCredential)) {
    throw new Error('No se confirmó la identidad en este dispositivo.');
  }

  const response = assertion.response as AuthenticatorAssertionResponse;
  const clientDataRaw = new TextDecoder().decode(response.clientDataJSON);
  const clientData = JSON.parse(clientDataRaw) as {
    type?: string;
    challenge?: string;
    origin?: string;
  };

  if (clientData.type !== 'webauthn.get' || clientData.challenge !== expectedChallengeB64) {
    throw new Error('La respuesta WebAuthn no coincide con el reto de seguridad generado.');
  }

  const authData = new Uint8Array(response.authenticatorData);
  const flags = authData.length > 32 ? authData[32] : 0;
  const userPresent = (flags & 0x01) !== 0;
  const userVerified = (flags & 0x04) !== 0;
  if (!userPresent || !userVerified) {
    throw new Error('El sistema operativo no confirmó verificación de usuario (PIN/contraseña/huella).');
  }

  writeStoredState({
    ...stored,
    enabled: keepEnabled,
  });
}

export function setDevice2faEnabledOnly(enabled: boolean): void {
  const stored = readStoredState();
  if (!stored) return;
  writeStoredState({
    ...stored,
    enabled,
  });
}

export function clearDevice2fa(): void {
  writeStoredState(null);
}
