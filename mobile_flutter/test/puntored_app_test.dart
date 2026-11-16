import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:local_auth/local_auth.dart';
import 'package:puntored_mobile/core/formatters.dart';
import 'package:puntored_mobile/models/api_models.dart';
import 'package:puntored_mobile/screens/auth_screen.dart';
import 'package:puntored_mobile/screens/payment_screen.dart';
import 'package:puntored_mobile/services/api_service.dart';
import 'package:puntored_mobile/services/biometric_service.dart';
import 'package:puntored_mobile/state/app_state.dart';
import 'package:puntored_mobile/widgets/common_widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _MemorySecureVault implements SecureVault {
  final Map<String, String> _store = {};

  @override
  Future<void> delete(String key) async {
    _store.remove(key);
  }

  @override
  Future<String?> read(String key) async => _store[key];

  @override
  Future<void> write(String key, String value) async {
    _store[key] = value;
  }
}

class _FakeBiometricDriver implements BiometricDriver {
  bool supported;
  List<BiometricType> enrolled;
  bool authenticateResult;
  BiometricAuthException? errorToThrow;
  int authenticateCalls = 0;

  _FakeBiometricDriver({
    this.supported = true,
    this.enrolled = const [BiometricType.fingerprint, BiometricType.face],
    this.authenticateResult = true,
    this.errorToThrow,
  });

  @override
  Future<bool> get canCheckBiometrics async => supported;

  @override
  Future<List<BiometricType>> getAvailableBiometrics() async => enrolled;

  @override
  Future<bool> isDeviceSupported() async => supported;

  @override
  Future<bool> authenticate({
    required String localizedReason,
    bool biometricOnly = true,
    String? signInTitle,
    String? signInHint,
    String? cancelButton,
  }) async {
    authenticateCalls++;
    if (errorToThrow != null) throw errorToThrow!;
    return authenticateResult;
  }
}

void main() {
  group('AppFormatters', () {
    test('formatea moneda y cuentas de 18 digitos', () {
      expect(AppFormatters.formatCurrency(1250.5), r'$1,250.50');
      expect(
        AppFormatters.formatAccountNumber('400123456789012345'),
        '4001 2345 6789 0123 45',
      );
      expect(
        AppFormatters.prefijoRfcCurp(
          nombre: 'María Fernanda',
          apellidoPaterno: 'Pérez',
          apellidoMaterno: 'López',
          fechaNacimiento: '1998-07-14',
        ),
        'PELF980714',
      );
      expect(AppFormatters.esMayorDeEdad('1998-07-14'), isTrue);
      expect(AppFormatters.esMayorDeEdad('2020-01-01'), isFalse);
    });
  });

  group('Modelos Puntored H2H V2', () {
    test('parsea estados EXITOSA, RECHAZADA y NO_CONFIRMADA sin confundirlos', () {
      final validacion = ValidacionReferenciaResponse.fromJson({
        'codigo': 0,
        'mensaje': 'Referencia valida',
        'referenciaValida': true,
        'tokenValidacion': 'tok-123',
        'tipoOperacion': 'RECAUDO',
        'referencia1': '5512345678',
        'montoSugerido': 245.0,
        'montoEditable': false,
        'opciones': [
          {'codigoRegistro': '1', 'descripcionRegistro': 'Pago total', 'monto': 245.0, 'tipoMonto': 1}
        ],
      });
      expect(validacion.referenciaValida, isTrue);
      expect(validacion.tokenValidacion, 'tok-123');
      expect(validacion.opciones.single.monto, 245.0);

      final exitosa = PagoPuntoredResponse.fromJson({
        'codigo': 0,
        'mensaje': 'Pago exitoso',
        'estado': 'EXITOSA',
        'transaccionConfirmada': true,
        'idTransaccionLocal': 10,
        'codigoAutorizacion': 'AUT9988',
        'numeroCuenta': '400123456789012345',
        'nombreServicio': 'CFE',
        'referencia1': '5512345678',
        'monto': 245.0,
        'saldoAnterior': 1000.0,
        'saldoNuevo': 755.0,
        'ticket': {
          'titulo': 'TIRILLA CFE',
          'cuerpo': {'Folio': 'AUT9988'},
        },
      });
      expect(exitosa.transaccionConfirmada, isTrue);
      expect(exitosa.ticket?.cuerpo?['Folio'], 'AUT9988');

      final noConfirmada = PagoPuntoredResponse.fromJson({
        'codigo': 30,
        'mensaje': 'Timeout del proveedor',
        'estado': 'NO_CONFIRMADA',
        'transaccionConfirmada': false,
        'numeroCuenta': '400123456789012345',
        'nombreServicio': 'CFE',
        'referencia1': '5512345678',
        'monto': 245.0,
        'saldoAnterior': 1000.0,
        'saldoNuevo': 755.0,
      });
      expect(noConfirmada.estado, 'NO_CONFIRMADA');
      expect(noConfirmada.transaccionConfirmada, isFalse);
    });
  });

  group('ApiService HTTP', () {
    test('login exitoso y auto-logout al recibir HTTP 401', () async {
      bool unauthorizedCalled = false;
      final mockClient = MockClient((req) async {
        if (req.url.path.endsWith('/auth/login')) {
          expect(req.headers.containsKey('Authorization'), isFalse);
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-token-demo',
              'clienteId': 42,
              'rol': 2,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        expect(req.headers['Authorization'], 'Bearer jwt-token-demo');
        return http.Response(
          jsonEncode({'codigo': 9, 'mensaje': 'Token expirado'}),
          401,
        );
      });

      final service = ApiService(
        client: mockClient,
        initialBaseUrl: 'https://api.test',
        onUnauthorized: () => unauthorizedCalled = true,
      );

      final session = await service.login('user@test.com', 'Password123!');
      expect(session.jwt, 'jwt-token-demo');
      service.setToken(session.jwt);

      await expectLater(
        () => service.obtenerMisCuentas(),
        throwsA(isA<ApiClientException>()),
      );
      expect(unauthorizedCalled, isTrue);
    });
  });

  group('Widgets UI', () {
    testWidgets('StatusBadge diferencia los 4 estados visuales', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                StatusBadge(estado: 'EXITOSA'),
                StatusBadge(estado: 'RECHAZADA'),
                StatusBadge(estado: 'PENDIENTE'),
                StatusBadge(estado: 'NO_CONFIRMADA'),
              ],
            ),
          ),
        ),
      );

      expect(find.text('Exitosa'), findsOneWidget);
      expect(find.text('Rechazada'), findsOneWidget);
      expect(find.text('Pendiente'), findsOneWidget);
      expect(find.text('No Confirmada'), findsOneWidget);
    });

    testWidgets('AuthScreen permite alternar Login / Crear Cuenta y valida campos',
        (tester) async {
      final state = AppState(
        apiService: ApiService(
          client: MockClient((_) async => http.Response('{}', 200)),
          initialBaseUrl: 'https://api.test',
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(appState: state),
        ),
      );

      expect(find.text('Iniciar Sesión'), findsOneWidget);
      expect(find.text('Crear Cuenta'), findsOneWidget);

      await tester.tap(find.byKey(const ValueKey('loginSubmitButton')));
      await tester.pump();
      expect(find.text('Ingresa un correo electrónico válido'), findsOneWidget);

      await tester.tap(find.text('Crear Cuenta'));
      await tester.pumpAndSettle();
      expect(find.text('Registro de Nuevo Usuario'), findsOneWidget);
    });
  });

  group('Autenticación Biométrica 2FA', () {
    test('login exige biometría cuando 2FA está activo y guarda credenciales en vault',
        () async {
      SharedPreferences.setMockInitialValues({});
      final vault = _MemorySecureVault();
      final driver = _FakeBiometricDriver();
      final bioService = BiometricService(driver: driver, vault: vault);

      final client = MockClient((req) async {
        if (req.url.path.endsWith('/auth/login')) {
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-bio-ok',
              'clienteId': 1,
              'rol': 1,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        return http.Response('{}', 200);
      });

      final state = AppState(
        apiService: ApiService(client: client, initialBaseUrl: 'https://api.test'),
        biometricService: bioService,
      );
      addTearDown(state.dispose);
      await state.init();
      expect(state.biometricAvailable, isTrue);
      expect(state.biometric2faEnabled, isTrue);

      final session = await state.login('mijelito333@gmail.com', 'ClaveTest1!');
      expect(session.jwt, 'jwt-bio-ok');
      expect(driver.authenticateCalls, 1);
      expect(state.session, isNotNull);
      expect(state.savedBiometricEmail, 'mijelito333@gmail.com');
    });

    test('cancelar Huella / Face ID revoca sesión en servidor y bloquea el acceso',
        () async {
      SharedPreferences.setMockInitialValues({});
      final vault = _MemorySecureVault();
      final driver = _FakeBiometricDriver(
        errorToThrow: const BiometricAuthException(
          'Verificación biométrica cancelada por el usuario.',
          wasCanceled: true,
        ),
      );
      final bioService = BiometricService(driver: driver, vault: vault);
      bool logoutCalled = false;

      final client = MockClient((req) async {
        if (req.url.path.endsWith('/auth/login')) {
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-temp-2fa',
              'clienteId': 1,
              'rol': 1,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        if (req.url.path.endsWith('/auth/logout')) {
          logoutCalled = true;
          expect(req.headers['Authorization'], 'Bearer jwt-temp-2fa');
          return http.Response('{}', 200);
        }
        return http.Response('{}', 404);
      });

      final state = AppState(
        apiService: ApiService(client: client, initialBaseUrl: 'https://api.test'),
        biometricService: bioService,
      );
      addTearDown(state.dispose);
      await state.init();

      await expectLater(
        () => state.login('mijelito333@gmail.com', 'ClaveTest1!'),
        throwsA(isA<BiometricAuthException>()),
      );
      expect(logoutCalled, isTrue);
      expect(state.session, isNull);
      expect(state.hasSavedBiometricAccount, isFalse);
    });

    test('loginWithBiometrics autentica 1-tap usando vault cifrado', () async {
      SharedPreferences.setMockInitialValues({});
      final vault = _MemorySecureVault();
      final driver = _FakeBiometricDriver();
      final bioService = BiometricService(driver: driver, vault: vault);
      await bioService.set2faEnabled(true);
      await bioService.saveCredentials('mijelito333@gmail.com', 'ClaveTest1!');

      int loginCalls = 0;
      final client = MockClient((req) async {
        if (req.url.path.endsWith('/auth/login')) {
          loginCalls++;
          final body = jsonDecode(req.body) as Map<String, dynamic>;
          expect(body['correo'], 'mijelito333@gmail.com');
          expect(body['password'], 'ClaveTest1!');
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-quick-1tap',
              'clienteId': 1,
              'rol': 1,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        return http.Response('{}', 200);
      });

      final state = AppState(
        apiService: ApiService(client: client, initialBaseUrl: 'https://api.test'),
        biometricService: bioService,
      );
      addTearDown(state.dispose);
      await state.init();

      final session = await state.loginWithBiometrics();
      expect(driver.authenticateCalls, 1);
      expect(loginCalls, 1);
      expect(session.jwt, 'jwt-quick-1tap');
      expect(state.session, isNotNull);
    });

    test('dispositivo sin soporte ni biometría rechaza acceso rápido', () async {
      SharedPreferences.setMockInitialValues({});
      final noHwState = AppState(
        apiService: ApiService(
          client: MockClient((_) async => http.Response('{}', 200)),
          initialBaseUrl: 'https://api.test',
        ),
        biometricService: BiometricService(
          driver: _FakeBiometricDriver(supported: false, enrolled: const []),
          vault: _MemorySecureVault(),
        ),
      );
      await noHwState.init();
      expect(noHwState.biometricAvailable, isFalse);
      expect(noHwState.biometric2faEnabled, isFalse);

      final failDriver = _FakeBiometricDriver(authenticateResult: false);
      final rejectBio = BiometricService(
        driver: failDriver,
        vault: _MemorySecureVault(),
      );
      await expectLater(
        () => rejectBio.verify(localizedReason: 'Verificar'),
        throwsA(isA<BiometricAuthException>()),
      );
    });

    testWidgets('AuthScreen muestra acceso biométrico rápido y permite entrar',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      final vault = _MemorySecureVault();
      final driver = _FakeBiometricDriver();
      final bioService = BiometricService(driver: driver, vault: vault);
      await bioService.set2faEnabled(true);
      await bioService.saveCredentials('mijelito333@gmail.com', 'ClaveTest1!');

      final client = MockClient((req) async {
        if (req.url.path.endsWith('/auth/login')) {
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-widget-bio',
              'clienteId': 1,
              'rol': 1,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        return http.Response('{}', 200);
      });

      final state = AppState(
        apiService: ApiService(client: client, initialBaseUrl: 'https://api.test'),
        biometricService: bioService,
      );
      addTearDown(state.dispose);
      await state.init();

      await tester.pumpWidget(
        MaterialApp(
          home: AuthScreen(appState: state, autoPromptBiometrics: false),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const ValueKey('biometricLoginButton')), findsOneWidget);
      expect(find.text('Entrar con Huella o Face ID'), findsOneWidget);

      await tester.tap(find.byKey(const ValueKey('biometricLoginButton')));
      await tester.pumpAndSettle();

      expect(driver.authenticateCalls, 1);
      expect(state.session, isNotNull);
    });

    testWidgets(
        'PaymentScreen permite alternar entre Huella/Face y Correo y autocompleta PIN sin SMTP',
        (tester) async {
      SharedPreferences.setMockInitialValues({});
      final vault = _MemorySecureVault();
      final driver = _FakeBiometricDriver();
      final bioService = BiometricService(driver: driver, vault: vault);

      String? capturedMetodo2fa;
      bool? capturedBiometria;

      final client = MockClient((req) async {
        final path = req.url.path;
        if (path.endsWith('/auth/login')) {
          return http.Response(
            jsonEncode({
              'jwt': 'jwt-pay-test',
              'clienteId': 1,
              'rol': 1,
              'expiraEnSegundos': 900,
            }),
            200,
          );
        }
        if (path.endsWith('/cuentas/mias')) {
          return http.Response(
            jsonEncode({
              'cuentas': [
                {
                  'id': 1,
                  'numeroCuenta': '848380128311868821',
                  'saldo': 5000.0,
                  'estatus': 'ACTIVA',
                }
              ],
            }),
            200,
          );
        }
        if (path.endsWith('/api/catalogo/buscar')) {
          return http.Response(
            jsonEncode({
              'total': 1,
              'servicios': [
                {
                  'tipoOperacion': 'RECARGA',
                  'categoria': 'Recargas',
                  'codigoProducto': 737,
                  'nombreServicio': 'TELCEL',
                  'nombreProducto': 'Recarga 50',
                  'precioSugerido': 50.0,
                }
              ],
            }),
            200,
          );
        }
        if (path.endsWith('/pagos/validar-referencia')) {
          return http.Response(
            jsonEncode({
              'codigo': 0,
              'mensaje': 'OK',
              'referenciaValida': true,
              'trace': 'TRACE-1',
              'tokenValidacion': 'tok-val-1',
              'montoSugerido': 50.0,
              'montoEditable': true,
            }),
            200,
          );
        }
        if (path.endsWith('/pagos/enviar-pin')) {
          return http.Response(
            jsonEncode({
              'codigo': 0,
              'mensaje': 'Servidor sin proveedor de correo activo',
              'numeroCuenta': '848380128311868821',
              'correoDestino': 'mi***@gmail.com',
              'expiraEnSegundos': 300,
              'correoEnviado': false,
              'pin2fa': '654321',
            }),
            200,
          );
        }
        if (path.endsWith('/pagos/puntored')) {
          final body = jsonDecode(req.body) as Map<String, dynamic>;
          capturedMetodo2fa = body['metodo2fa']?.toString();
          capturedBiometria = body['biometriaVerificada'] as bool?;
          return http.Response(
            jsonEncode({
              'codigo': 0,
              'mensaje': 'Pago exitoso',
              'estado': 'EXITOSA',
              'transaccionConfirmada': true,
              'idTransaccionLocal': 99,
              'numeroCuenta': '848380128311868821',
              'nombreServicio': 'TELCEL',
              'nombreProducto': 'Recarga 50',
              'referencia1': '5512345678',
              'monto': 50.0,
              'saldoAnterior': 5000.0,
              'saldoNuevo': 4950.0,
            }),
            200,
          );
        }
        return http.Response('{}', 200);
      });

      final state = AppState(
        apiService: ApiService(client: client, initialBaseUrl: 'https://api.test'),
        biometricService: bioService,
      );
      addTearDown(state.dispose);
      await state.init();
      await state.login('mijelito333@gmail.com', 'AdminPuntoRed2026!');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PaymentScreen(
              appState: state,
              onPickFromCatalog: () {},
              onOpenHistory: () {},
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Ingresar referencia y validar
      await tester.enterText(
          find.byKey(const ValueKey('payReferenceField')), '5512345678');
      final mainScrollable = find.byType(Scrollable).first;
      final validateBtn = find.byKey(const ValueKey('validateRefButton'));
      await tester.scrollUntilVisible(validateBtn, 200,
          scrollable: mainScrollable);
      await tester.ensureVisible(validateBtn);
      await tester.pumpAndSettle();
      await tester.tap(validateBtn);
      await tester.pumpAndSettle();

      // Abrir diálogo de confirmación explícita
      final openBtn = find.byKey(const ValueKey('openConfirmPaymentButton'));
      await tester.scrollUntilVisible(openBtn, 200, scrollable: mainScrollable);
      await tester.ensureVisible(openBtn);
      await tester.pumpAndSettle();
      await tester.tap(openBtn);
      await tester.pumpAndSettle();

      // Inicia con Huella/Face seleccionado
      expect(find.byKey(const ValueKey('payment2faMethodToggle')), findsOneWidget);
      expect(find.byKey(const ValueKey('biometricPaymentBox')), findsOneWidget);
      expect(find.text('Autorizar con Huella'), findsOneWidget);

      // Cambiar toggle a Correo y solicitar PIN
      await tester.tap(find.text('Correo'));
      await tester.pumpAndSettle();
      expect(find.byKey(const ValueKey('sendPinEmailButton')), findsOneWidget);

      await tester.tap(find.byKey(const ValueKey('sendPinEmailButton')));
      await tester.pumpAndSettle();
      expect(find.textContaining('PIN temporal autocompletado: 654321'), findsOneWidget);

      // Volver a Huella/Face, marcar checkbox y autorizar pago biométrico
      await tester.tap(find.text('Huella/Face'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('confirmExplicitCheckbox')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const ValueKey('authorizePaymentButton')));
      await tester.pumpAndSettle();

      expect(driver.authenticateCalls, 2); // 1 en login + 1 al autorizar pago
      expect(capturedMetodo2fa, 'BIOMETRIA_MOVIL');
      expect(capturedBiometria, isTrue);
      await tester.scrollUntilVisible(
        find.text('Resultado de la Operación'),
        200,
        scrollable: mainScrollable,
      );
      expect(find.text('Resultado de la Operación'), findsOneWidget);
    });
  });
}
