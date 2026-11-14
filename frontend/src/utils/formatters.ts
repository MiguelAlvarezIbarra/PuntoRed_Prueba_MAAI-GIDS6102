export function formatCurrency(value?: number | string | null): string {
  const num = typeof value === 'string' ? Number.parseFloat(value) : (value ?? 0);
  if (Number.isNaN(num)) {
    return '$0.00';
  }
  return new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num);
}

export function formatAccountNumber(account?: string | null): string {
  if (!account) {
    return '—';
  }
  const clean = account.replace(/\s+/g, '');
  return clean.replace(/(.{4})/g, '$1 ').trim();
}

export function formatDateTime(iso?: string | null): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return new Intl.DateTimeFormat('es-MX', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function cleanSpanishUpper(input: string): string {
  return input
    .trim()
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

/**
 * Replica exactamente la lógica del backend (ValidadorRfcCurp.java)
 * para mostrar al usuario la clave esperada de 10 caracteres al capturar RFC/CURP.
 */
export function calcularPrefijoRfcCurp(
  nombre: string,
  apellidoPaterno: string,
  apellidoMaterno: string,
  fechaNacimiento: string,
): string {
  if (!nombre || !apellidoPaterno || !apellidoMaterno || !fechaNacimiento) {
    return '';
  }
  const pat = cleanSpanishUpper(apellidoPaterno);
  const mat = cleanSpanishUpper(apellidoMaterno);
  const nom = cleanSpanishUpper(nombre);

  const primera = pat.charAt(0) || 'X';
  let vocal = 'X';
  for (let i = 1; i < pat.length; i += 1) {
    if ('AEIOU'.includes(pat.charAt(i))) {
      vocal = pat.charAt(i);
      break;
    }
  }
  const tercera = mat.charAt(0) || 'X';

  const partesNombre = nom.split(/\s+/).filter(Boolean);
  let nombreUsar = partesNombre[0] || '';
  if (partesNombre.length > 1 && (nombreUsar === 'JOSE' || nombreUsar === 'MARIA')) {
    nombreUsar = partesNombre[1];
  }
  const cuarta = nombreUsar.charAt(0) || 'X';

  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fechaNacimiento);
  if (!match) {
    return '';
  }
  const yy = match[1].slice(2);
  const mm = match[2];
  const dd = match[3];
  return `${primera}${vocal}${tercera}${cuarta}${yy}${mm}${dd}`;
}

export function esMayorDeEdad(fechaNacimiento: string): boolean {
  if (!fechaNacimiento) {
    return false;
  }
  const birth = new Date(`${fechaNacimiento}T00:00:00`);
  if (Number.isNaN(birth.getTime())) {
    return false;
  }
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age >= 18;
}
