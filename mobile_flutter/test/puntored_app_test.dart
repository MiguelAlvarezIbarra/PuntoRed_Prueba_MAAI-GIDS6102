import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:puntored_mobile/core/formatters.dart';
import 'package:puntored_mobile/models/api_models.dart';
import 'package:puntored_mobile/screens/auth_screen.dart';
import 'package:puntored_mobile/services/api_service.dart';
import 'package:puntored_mobile/state/app_state.dart';
import 'package:puntored_mobile/widgets/common_widgets.dart';

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
}
