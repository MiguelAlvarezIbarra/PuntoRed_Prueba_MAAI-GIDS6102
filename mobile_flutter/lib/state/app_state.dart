import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/constants.dart';
import '../models/api_models.dart';
import '../services/api_service.dart';
import '../services/biometric_service.dart';

class AppState extends ChangeNotifier {
  static const _keySession = 'puntored_session_json';
  static const _keyBaseUrl = 'puntored_api_base_url';
  static const _keyDarkMode = 'puntored_dark_mode';

  final BiometricService biometric;
  BiometricStatus _biometricStatus = const BiometricStatus.unavailable();
  bool _biometric2faEnabled = false;
  String? _savedBiometricEmail;
  bool _lastLogoutWasManual = false;

  BiometricStatus get biometricStatus => _biometricStatus;
  bool get biometricAvailable => _biometricStatus.isReady;
  bool get biometric2faEnabled => _biometric2faEnabled;
  String? get savedBiometricEmail => _savedBiometricEmail;
  bool get hasSavedBiometricAccount =>
      _savedBiometricEmail != null && _savedBiometricEmail!.isNotEmpty;
  bool get lastLogoutWasManual => _lastLogoutWasManual;

  final ApiService api;
  AuthSession? _session;
  ThemeMode _themeMode = ThemeMode.light;
  String? sessionNotice;
  Timer? _expiryTimer;
  bool _initialized = false;

  AppState({ApiService? apiService, BiometricService? biometricService})
      : api = apiService ?? ApiService(initialBaseUrl: AppConstants.renderApiUrl),
        biometric = biometricService ?? BiometricService() {
    api.onUnauthorized = () {
      clearLocalSession(
        notice: 'Tu sesión expiró o fue cerrada por seguridad. Inicia sesión nuevamente.',
      );
    };
  }

  AuthSession? get session => _session;
  ThemeMode get themeMode => _themeMode;
  bool get isAdmin => _session?.isAdmin ?? false;
  bool get isInitialized => _initialized;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final savedUrl = prefs.getString(_keyBaseUrl);
    if (savedUrl != null && savedUrl.isNotEmpty) {
      api.setBaseUrl(savedUrl);
    }
    _themeMode = (prefs.getBool(_keyDarkMode) ?? false)
        ? ThemeMode.dark
        : ThemeMode.light;

    final rawSession = prefs.getString(_keySession);
    if (rawSession != null && rawSession.isNotEmpty) {
      try {
        final decoded = AuthSession.fromJson(
          jsonDecode(rawSession) as Map<String, dynamic>,
        );
        final elapsed = DateTime.now().millisecondsSinceEpoch - decoded.loginAtMs;
        final ttlMs = decoded.expiraEnSegundos * 1000;
        if (decoded.jwt.isNotEmpty && elapsed < ttlMs) {
          _session = decoded;
          api.setToken(decoded.jwt);
          _scheduleExpiry(Duration(milliseconds: ttlMs - elapsed));
        } else {
          await prefs.remove(_keySession);
        }
      } catch (_) {
        await prefs.remove(_keySession);
      }
    }
    await refreshBiometricStatus();
    _initialized = true;
    notifyListeners();
  }

  Future<void> toggleTheme() async {
    _themeMode =
        _themeMode == ThemeMode.light ? ThemeMode.dark : ThemeMode.light;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyDarkMode, _themeMode == ThemeMode.dark);
  }

  Future<void> updateBaseUrl(String url) async {
    api.setBaseUrl(url);
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyBaseUrl, api.baseUrl);
  }

  Future<void> refreshBiometricStatus() async {
    _biometricStatus = await biometric.getStatus();
    if (_biometricStatus.isReady) {
      _biometric2faEnabled = await biometric.is2faEnabled(defaultValue: true);
    } else {
      _biometric2faEnabled = false;
    }
    final creds = await biometric.readCredentials();
    _savedBiometricEmail = creds?.correo;
  }

  /// Inicia sesión con correo y contraseña; si 2FA biométrico está activo,
  /// exige Huella o Face ID antes de habilitar la sesión.
  Future<AuthSession> login(
    String correo,
    String password, {
    bool? enableBiometric2fa,
  }) async {
    sessionNotice = null;
    _lastLogoutWasManual = false;
    final cleanEmail = correo.trim();
    final logged = await api.login(cleanEmail, password);

    final bool shouldVerifyBiometric =
        enableBiometric2fa ?? (_biometricStatus.isReady && _biometric2faEnabled);

    if (shouldVerifyBiometric) {
      api.setToken(logged.jwt);
      try {
        await biometric.verify(
          localizedReason:
              'Confirma tu huella o Face ID para completar el acceso 2FA a $cleanEmail',
          signInTitle: 'Verificación 2FA PuntoRed',
          signInHint: 'Verifica tu huella o Face ID',
        );
      } catch (e) {
        try {
          await api.logout();
        } catch (_) {}
        api.setToken(null);
        rethrow;
      }
      await biometric.set2faEnabled(true);
      await biometric.saveCredentials(cleanEmail, password);
      _biometric2faEnabled = true;
      _savedBiometricEmail = cleanEmail;
    } else if (enableBiometric2fa == false) {
      await biometric.set2faEnabled(false);
      _biometric2faEnabled = false;
    }

    return _activateSession(logged);
  }

  /// Autenticación rápida 1-tap con Huella / Face ID usando credenciales
  /// resguardadas de forma cifrada en Android Keystore.
  Future<AuthSession> loginWithBiometrics() async {
    sessionNotice = null;
    _lastLogoutWasManual = false;
    await refreshBiometricStatus();
    final creds = await biometric.readCredentials();
    if (creds == null) {
      throw const BiometricAuthException(
        'No hay una cuenta vinculada con biometría. Inicia sesión con correo y contraseña una vez.',
      );
    }
    await biometric.verify(
      localizedReason:
          'Usa tu huella o Face ID para ingresar a ${creds.correo}',
      signInTitle: 'Acceso Biométrico PuntoRed',
      signInHint: creds.correo,
    );
    final logged = await api.login(creds.correo, creds.password);
    return _activateSession(logged);
  }

  Future<void> setBiometric2faEnabled(
    bool enabled, {
    String? confirmPassword,
  }) async {
    await refreshBiometricStatus();
    if (enabled) {
      if (!_biometricStatus.isReady) {
        throw BiometricAuthException(_biometricStatus.description);
      }
      await biometric.verify(
        localizedReason:
            'Confirma tu huella o Face ID para activar 2FA biométrico',
        signInTitle: 'Activar 2FA Biométrico',
      );
      final email = _session?.correo ?? '';
      if (confirmPassword != null &&
          confirmPassword.isNotEmpty &&
          email.isNotEmpty) {
        final refreshed = await api.login(email, confirmPassword);
        await biometric.saveCredentials(email, confirmPassword);
        _savedBiometricEmail = email;
        await biometric.set2faEnabled(true);
        _biometric2faEnabled = true;
        await _activateSession(refreshed);
        return;
      }
      await biometric.set2faEnabled(true);
      _biometric2faEnabled = true;
    } else {
      await biometric.set2faEnabled(false);
      _biometric2faEnabled = false;
    }
    notifyListeners();
  }

  Future<void> unlinkBiometricAccount() async {
    await biometric.clearCredentials();
    await biometric.set2faEnabled(false);
    _savedBiometricEmail = null;
    _biometric2faEnabled = false;
    notifyListeners();
  }

  Future<AuthSession> _activateSession(AuthSession logged) async {
    _session = logged;
    api.setToken(logged.jwt);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keySession, jsonEncode(logged.toJson()));
    _scheduleExpiry(Duration(seconds: logged.expiraEnSegundos));
    notifyListeners();
    return logged;
  }

  Future<void> logout({bool manual = false}) async {
    _lastLogoutWasManual = manual;
    try {
      await api.logout();
    } catch (_) {
      // Si el token ya expiró, igual limpiamos la sesión local.
    } finally {
      await clearLocalSession();
    }
  }

  Future<void> clearLocalSession({String? notice}) async {
    _expiryTimer?.cancel();
    _expiryTimer = null;
    _session = null;
    api.setToken(null);
    sessionNotice = notice;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keySession);
  }

  void _scheduleExpiry(Duration duration) {
    _expiryTimer?.cancel();
    _expiryTimer = Timer(duration, () {
      clearLocalSession(
        notice: 'La sesión expiró por tiempo límite del token JWT.',
      );
    });
  }

  @override
  void dispose() {
    _expiryTimer?.cancel();
    super.dispose();
  }
}
