import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';
import 'package:local_auth_android/local_auth_android.dart';

class BiometricAuthException implements Exception {
  final String message;
  final bool wasCanceled;

  const BiometricAuthException(this.message, {this.wasCanceled = false});

  @override
  String toString() => message;
}

class BiometricCredentials {
  final String correo;
  final String password;

  const BiometricCredentials({
    required this.correo,
    required this.password,
  });

  Map<String, dynamic> toJson() => {
        'correo': correo,
        'password': password,
      };

  factory BiometricCredentials.fromJson(Map<String, dynamic> json) {
    return BiometricCredentials(
      correo: (json['correo'] ?? '').toString(),
      password: (json['password'] ?? '').toString(),
    );
  }
}

class BiometricStatus {
  final bool hardwareAvailable;
  final bool hasEnrolledBiometrics;
  final List<BiometricType> availableTypes;

  const BiometricStatus({
    required this.hardwareAvailable,
    required this.hasEnrolledBiometrics,
    required this.availableTypes,
  });

  const BiometricStatus.unavailable()
      : hardwareAvailable = false,
        hasEnrolledBiometrics = false,
        availableTypes = const [];

  bool get isReady => hardwareAvailable && hasEnrolledBiometrics;

  bool get supportsFace => availableTypes.contains(BiometricType.face);

  bool get supportsFingerprint =>
      availableTypes.contains(BiometricType.fingerprint) ||
      availableTypes.contains(BiometricType.strong);

  String get shortLabel {
    if (!hardwareAvailable) return 'No disponible';
    if (!hasEnrolledBiometrics) return 'Sin registrar';
    if (supportsFace && supportsFingerprint) return 'Huella o Face ID';
    if (supportsFace) return 'Face ID';
    if (supportsFingerprint) return 'Huella digital';
    return 'Biometría';
  }

  String get description {
    if (!hardwareAvailable) {
      return 'Este dispositivo o entorno no reporta sensor biométrico compatible.';
    }
    if (!hasEnrolledBiometrics) {
      return 'No hay huellas ni Face ID registrados en Ajustes del teléfono.';
    }
    if (supportsFace && supportsFingerprint) {
      return 'Huella digital y reconocimiento facial listos para 2FA.';
    }
    if (supportsFace) {
      return 'Reconocimiento facial (Face ID) listo para 2FA.';
    }
    if (supportsFingerprint) {
      return 'Sensor de huella digital listo para 2FA.';
    }
    return 'Autenticación biométrica del sistema lista para 2FA.';
  }
}

abstract class SecureVault {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

class FlutterSecureVault implements SecureVault {
  static const AndroidOptions _androidOptions = AndroidOptions(
    resetOnError: true,
  );

  final FlutterSecureStorage _storage;

  const FlutterSecureVault([
    FlutterSecureStorage storage = const FlutterSecureStorage(
      aOptions: _androidOptions,
    ),
  ]) : _storage = storage;

  @override
  Future<String?> read(String key) async {
    try {
      return await _storage.read(key: key, aOptions: _androidOptions);
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }

  @override
  Future<void> write(String key, String value) async {
    try {
      await _storage.write(key: key, value: value, aOptions: _androidOptions);
    } on MissingPluginException {
      // No-op en entornos de prueba sin canal nativo.
    }
  }

  @override
  Future<void> delete(String key) async {
    try {
      await _storage.delete(key: key, aOptions: _androidOptions);
    } on MissingPluginException {
      // No-op en entornos de prueba sin canal nativo.
    }
  }
}

abstract class BiometricDriver {
  Future<bool> isDeviceSupported();
  Future<bool> get canCheckBiometrics;
  Future<List<BiometricType>> getAvailableBiometrics();
  Future<bool> authenticate({
    required String localizedReason,
    bool biometricOnly = true,
    String? signInTitle,
    String? signInHint,
    String? cancelButton,
  });
}

class LocalAuthBiometricDriver implements BiometricDriver {
  final LocalAuthentication _auth;

  LocalAuthBiometricDriver([LocalAuthentication? auth])
      : _auth = auth ?? LocalAuthentication();

  @override
  Future<bool> isDeviceSupported() async {
    try {
      return await _auth.isDeviceSupported();
    } on MissingPluginException {
      return false;
    } on LocalAuthException {
      return false;
    }
  }

  @override
  Future<bool> get canCheckBiometrics async {
    try {
      return await _auth.canCheckBiometrics;
    } on MissingPluginException {
      return false;
    } on LocalAuthException {
      return false;
    }
  }

  @override
  Future<List<BiometricType>> getAvailableBiometrics() async {
    try {
      return await _auth.getAvailableBiometrics();
    } on MissingPluginException {
      return const [];
    } on LocalAuthException {
      return const [];
    }
  }

  @override
  Future<bool> authenticate({
    required String localizedReason,
    bool biometricOnly = true,
    String? signInTitle,
    String? signInHint,
    String? cancelButton,
  }) async {
    try {
      return await _auth.authenticate(
        localizedReason: localizedReason,
        authMessages: <AuthMessages>[
          AndroidAuthMessages(
            signInTitle: signInTitle ?? 'Verificación Biométrica PuntoRed',
            signInHint:
                signInHint ?? 'Confirma tu identidad con Huella o Face ID',
            cancelButton: cancelButton ?? 'Cancelar',
          ),
        ],
        biometricOnly: biometricOnly,
        sensitiveTransaction: true,
        persistAcrossBackgrounding: true,
      );
    } on LocalAuthException catch (e) {
      throw BiometricAuthException(
        _mapExceptionMessage(e),
        wasCanceled: _isCanceledException(e),
      );
    } on MissingPluginException {
      throw const BiometricAuthException(
        'El lector biométrico no está disponible en este entorno.',
      );
    }
  }

  bool _isCanceledException(LocalAuthException e) {
    return e.code == LocalAuthExceptionCode.userCanceled ||
        e.code == LocalAuthExceptionCode.systemCanceled ||
        e.code == LocalAuthExceptionCode.userRequestedFallback;
  }

  String _mapExceptionMessage(LocalAuthException e) {
    switch (e.code) {
      case LocalAuthExceptionCode.userCanceled:
      case LocalAuthExceptionCode.userRequestedFallback:
        return 'Verificación biométrica cancelada por el usuario.';
      case LocalAuthExceptionCode.systemCanceled:
        return 'La verificación biométrica fue interrumpida por el sistema.';
      case LocalAuthExceptionCode.timeout:
        return 'Tiempo agotado esperando tu huella o Face ID.';
      case LocalAuthExceptionCode.noBiometricsEnrolled:
        return 'No tienes huellas ni Face ID configurados en Ajustes del teléfono.';
      case LocalAuthExceptionCode.noCredentialsSet:
        return 'Configura un bloqueo de pantalla y huella/rostro en tu teléfono.';
      case LocalAuthExceptionCode.noBiometricHardware:
        return 'Este dispositivo no cuenta con hardware biométrico compatible.';
      case LocalAuthExceptionCode.biometricHardwareTemporarilyUnavailable:
        return 'El sensor biométrico está temporalmente ocupado o no disponible.';
      case LocalAuthExceptionCode.temporaryLockout:
        return 'Demasiados intentos fallidos. Espera unos segundos e intenta de nuevo.';
      case LocalAuthExceptionCode.biometricLockout:
        return 'Sensor biométrico bloqueado por demasiados intentos. Desbloquea tu teléfono primero.';
      case LocalAuthExceptionCode.authInProgress:
        return 'Ya hay una solicitud biométrica activa.';
      case LocalAuthExceptionCode.uiUnavailable:
      case LocalAuthExceptionCode.deviceError:
      case LocalAuthExceptionCode.unknownError:
        final desc = e.description?.trim() ?? '';
        return desc.isNotEmpty
            ? 'No se pudo completar la biometría: $desc'
            : 'No se pudo validar tu identidad biométrica.';
    }
  }
}

/// Gestiona la verificación biométrica (Huella / Face ID) y el resguardo cifrado
/// de credenciales en Android Keystore para el segundo factor 2FA móvil.
class BiometricService {
  static const _key2faEnabled = 'puntored_biometric_2fa_enabled';
  static const _keyCredentials = 'puntored_biometric_credentials_v1';

  final BiometricDriver _driver;
  final SecureVault _vault;

  BiometricService({
    BiometricDriver? driver,
    SecureVault? vault,
  })  : _driver = driver ?? LocalAuthBiometricDriver(),
        _vault = vault ?? const FlutterSecureVault();

  Future<BiometricStatus> getStatus() async {
    final supported = await _driver.isDeviceSupported();
    final canCheck = await _driver.canCheckBiometrics;
    final available = await _driver.getAvailableBiometrics();
    return BiometricStatus(
      hardwareAvailable: supported || canCheck || available.isNotEmpty,
      hasEnrolledBiometrics: available.isNotEmpty,
      availableTypes: List<BiometricType>.unmodifiable(available),
    );
  }

  Future<bool> is2faEnabled({bool defaultValue = true}) async {
    final raw = await _vault.read(_key2faEnabled);
    if (raw == null) return defaultValue;
    return raw == 'true';
  }

  Future<void> set2faEnabled(bool enabled) async {
    await _vault.write(_key2faEnabled, enabled ? 'true' : 'false');
  }

  Future<void> saveCredentials(String correo, String password) async {
    final payload = BiometricCredentials(
      correo: correo.trim(),
      password: password,
    );
    await _vault.write(_keyCredentials, jsonEncode(payload.toJson()));
  }

  Future<BiometricCredentials?> readCredentials() async {
    final raw = await _vault.read(_keyCredentials);
    if (raw == null || raw.isEmpty) return null;
    try {
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      final creds = BiometricCredentials.fromJson(decoded);
      if (creds.correo.isEmpty || creds.password.isEmpty) return null;
      return creds;
    } catch (_) {
      await _vault.delete(_keyCredentials);
      return null;
    }
  }

  Future<void> clearCredentials() async {
    await _vault.delete(_keyCredentials);
  }

  /// Ejecuta el prompt biométrico nativo del dispositivo.
  /// Lanza [BiometricAuthException] si se cancela o falla la verificación.
  Future<void> verify({
    required String localizedReason,
    String? signInTitle,
    String? signInHint,
  }) async {
    final status = await getStatus();
    if (!status.hardwareAvailable) {
      throw const BiometricAuthException(
        'Este dispositivo no soporta verificación con huella o Face ID.',
      );
    }
    if (!status.hasEnrolledBiometrics) {
      throw const BiometricAuthException(
        'Registra al menos una huella o Face ID en Ajustes del celular.',
      );
    }

    final ok = await _driver.authenticate(
      localizedReason: localizedReason,
      biometricOnly: true,
      signInTitle: signInTitle,
      signInHint: signInHint,
      cancelButton: 'Cancelar',
    );
    if (!ok) {
      throw const BiometricAuthException(
        'No se confirmó tu identidad biométrica.',
        wasCanceled: true,
      );
    }
  }
}
