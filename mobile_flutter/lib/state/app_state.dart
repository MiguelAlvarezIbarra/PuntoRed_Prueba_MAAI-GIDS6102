import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/constants.dart';
import '../models/api_models.dart';
import '../services/api_service.dart';

class AppState extends ChangeNotifier {
  static const _keySession = 'puntored_session_json';
  static const _keyBaseUrl = 'puntored_api_base_url';
  static const _keyDarkMode = 'puntored_dark_mode';

  final ApiService api;
  AuthSession? _session;
  ThemeMode _themeMode = ThemeMode.light;
  String? sessionNotice;
  Timer? _expiryTimer;
  bool _initialized = false;

  AppState({ApiService? apiService})
      : api = apiService ?? ApiService(initialBaseUrl: AppConstants.renderApiUrl) {
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

  Future<AuthSession> login(String correo, String password) async {
    sessionNotice = null;
    final logged = await api.login(correo, password);
    _session = logged;
    api.setToken(logged.jwt);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keySession, jsonEncode(logged.toJson()));
    _scheduleExpiry(Duration(seconds: logged.expiraEnSegundos));
    notifyListeners();
    return logged;
  }

  Future<void> logout() async {
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
