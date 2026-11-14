class AppFormatters {
  AppFormatters._();

  static String formatCurrency(num? value) {
    final amount = (value ?? 0).toDouble();
    final negative = amount < 0;
    final absVal = amount.abs().toStringAsFixed(2);
    final parts = absVal.split('.');
    final intPart = parts[0];
    final decPart = parts[1];

    final buffer = StringBuffer();
    for (int i = 0; i < intPart.length; i++) {
      final posFromRight = intPart.length - i;
      buffer.write(intPart[i]);
      if (posFromRight > 1 && posFromRight % 3 == 1) {
        buffer.write(',');
      }
    }
    return '${negative ? '-' : ''}\$${buffer.toString()}.$decPart';
  }

  static String formatAccountNumber(String? account) {
    if (account == null || account.trim().isEmpty) {
      return '—';
    }
    final clean = account.replaceAll(RegExp(r'\s+'), '');
    final groups = <String>[];
    for (int i = 0; i < clean.length; i += 4) {
      final end = (i + 4 > clean.length) ? clean.length : i + 4;
      groups.add(clean.substring(i, end));
    }
    return groups.join(' ');
  }

  static String formatDateTime(String? iso) {
    if (iso == null || iso.trim().isEmpty) {
      return '—';
    }
    final parsed = DateTime.tryParse(iso);
    if (parsed == null) {
      return iso;
    }
    final local = parsed.toLocal();
    final dd = local.day.toString().padLeft(2, '0');
    final mm = local.month.toString().padLeft(2, '0');
    final hh = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$dd/$mm/${local.year} $hh:$min';
  }

  static String cleanUpper(String value) {
    const map = {
      'Á': 'A',
      'É': 'E',
      'Í': 'I',
      'Ó': 'O',
      'Ú': 'U',
      'Ü': 'U',
      'À': 'A',
      'È': 'E',
      'Ì': 'I',
      'Ò': 'O',
      'Ù': 'U',
    };
    final upper = value.trim().toUpperCase();
    final sb = StringBuffer();
    for (int i = 0; i < upper.length; i++) {
      final ch = upper[i];
      sb.write(map[ch] ?? ch);
    }
    return sb.toString().replaceAll(RegExp(r'[^A-ZÑ&\s]'), '');
  }

  static String prefijoRfcCurp({
    required String nombre,
    required String apellidoPaterno,
    required String apellidoMaterno,
    required String fechaNacimiento,
  }) {
    if (nombre.trim().isEmpty ||
        apellidoPaterno.trim().isEmpty ||
        fechaNacimiento.trim().isEmpty) {
      return '';
    }
    final pat = cleanUpper(apellidoPaterno);
    final mat = cleanUpper(apellidoMaterno);
    final nom = cleanUpper(nombre);

    final primera = pat.isNotEmpty ? pat[0] : 'X';
    String vocal = 'X';
    for (int i = 1; i < pat.length; i++) {
      if ('AEIOU'.contains(pat[i])) {
        vocal = pat[i];
        break;
      }
    }
    final tercera = mat.isNotEmpty ? mat[0] : 'X';
    final partes = nom.split(RegExp(r'\s+')).where((e) => e.isNotEmpty).toList();
    String usar = partes.isNotEmpty ? partes.first : '';
    if (partes.length > 1 && (usar == 'JOSE' || usar == 'MARIA')) {
      usar = partes[1];
    }
    final cuarta = usar.isNotEmpty ? usar[0] : 'X';
    final m = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(fechaNacimiento.trim());
    if (m == null) return '';
    return '$primera$vocal$tercera$cuarta${m.group(1)!.substring(2)}${m.group(2)}${m.group(3)}';
  }

  static bool esMayorDeEdad(String fechaNacimiento) {
    final m = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(fechaNacimiento.trim());
    if (m == null) return false;
    final y = int.tryParse(m.group(1)!);
    final mo = int.tryParse(m.group(2)!);
    final d = int.tryParse(m.group(3)!);
    if (y == null || mo == null || d == null) return false;
    final birth = DateTime(y, mo, d);
    final now = DateTime.now();
    int age = now.year - birth.year;
    if (now.month < birth.month || (now.month == birth.month && now.day < birth.day)) {
      age--;
    }
    return age >= 18;
  }
}
