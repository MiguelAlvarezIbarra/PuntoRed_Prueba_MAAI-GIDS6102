import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants.dart';
import '../models/api_models.dart';

class ApiClientException implements Exception {
  final String message;
  final int codigo;
  final int status;

  ApiClientException(this.message, {this.codigo = 99, this.status = 500});

  @override
  String toString() => message;
}

class ApiService {
  final http.Client _client;
  String _baseUrl;
  String? _token;
  void Function()? onUnauthorized;

  ApiService({
    http.Client? client,
    String initialBaseUrl = AppConstants.renderApiUrl,
    this.onUnauthorized,
  })  : _client = client ?? http.Client(),
        _baseUrl = initialBaseUrl;

  String get baseUrl => _baseUrl;

  void setBaseUrl(String url) {
    _baseUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
  }

  void setToken(String? token) {
    _token = token;
  }

  Future<Map<String, dynamic>> _request(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
    bool auth = true,
  }) async {
    final uri = Uri.parse('$_baseUrl$path');
    final headers = <String, String>{
      'Accept': 'application/json',
      if (body != null) 'Content-Type': 'application/json',
      if (auth && _token != null && _token!.isNotEmpty)
        'Authorization': 'Bearer $_token',
    };

    try {
      late http.Response resp;
      final encoded = body == null ? null : jsonEncode(body);
      const timeout = Duration(seconds: 45);
      switch (method) {
        case 'POST':
          resp = await _client.post(uri, headers: headers, body: encoded).timeout(timeout);
          break;
        case 'PUT':
          resp = await _client.put(uri, headers: headers, body: encoded).timeout(timeout);
          break;
        case 'PATCH':
          resp = await _client.patch(uri, headers: headers, body: encoded).timeout(timeout);
          break;
        case 'DELETE':
          resp = await _client.delete(uri, headers: headers, body: encoded).timeout(timeout);
          break;
        default:
          resp = await _client.get(uri, headers: headers).timeout(timeout);
      }

      Map<String, dynamic> payload = {};
      if (resp.body.isNotEmpty) {
        try {
          final decoded = jsonDecode(resp.body);
          if (decoded is Map<String, dynamic>) {
            payload = decoded;
          }
        } catch (_) {
          payload = {'codigo': resp.statusCode, 'mensaje': resp.body};
        }
      }

      if (resp.statusCode < 200 || resp.statusCode >= 300) {
        final code = payload['codigo'] is int
            ? payload['codigo'] as int
            : resp.statusCode;
        if (auth && (resp.statusCode == 401 || code == 9) && path != '/auth/logout') {
          onUnauthorized?.call();
        }
        throw ApiClientException(
          payload['mensaje']?.toString() ?? 'Error HTTP ${resp.statusCode}',
          codigo: code,
          status: resp.statusCode,
        );
      }

      return payload;
    } on ApiClientException {
      rethrow;
    } on TimeoutException {
      throw ApiClientException(
        'Tiempo de espera agotado al conectar con el servidor.',
        codigo: 21,
        status: 504,
      );
    } catch (_) {
      throw ApiClientException(
        'No fue posible conectar con el servidor API. Verifica la URL y tu red.',
        codigo: 22,
        status: 503,
      );
    }
  }

  Future<AuthSession> login(String correo, String password) async {
    final json = await _request(
      '/auth/login',
      method: 'POST',
      auth: false,
      body: {'correo': correo, 'password': password},
    );
    return AuthSession(
      jwt: json['jwt']?.toString() ?? '',
      clienteId: (json['clienteId'] as num?)?.toInt() ?? 0,
      rol: (json['rol'] as num?)?.toInt() ?? 2,
      correo: correo,
      expiraEnSegundos: (json['expiraEnSegundos'] as num?)?.toInt() ?? 900,
      loginAtMs: DateTime.now().millisecondsSinceEpoch,
    );
  }

  Future<void> logout() async {
    await _request('/auth/logout', method: 'POST', auth: true);
  }

  Future<Map<String, dynamic>> registrarClientePublico(Map<String, dynamic> payload) {
    return _request('/clientes', method: 'POST', body: payload, auth: false);
  }

  Future<Map<String, dynamic>> registrarClienteAdmin(Map<String, dynamic> payload) {
    return _request('/admin/clientes', method: 'POST', body: payload, auth: true);
  }

  Future<List<ClienteData>> buscarClientes([Map<String, dynamic>? filtros]) async {
    final json = await _request(
      '/clientes/search',
      method: 'POST',
      body: filtros ?? {},
      auth: true,
    );
    final list = json['clientes'] as List<dynamic>? ?? [];
    return list.map((e) => ClienteData.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Map<String, dynamic>> actualizarCliente(Map<String, dynamic> payload) {
    return _request('/clientes', method: 'PUT', body: payload, auth: true);
  }

  Future<Map<String, dynamic>> darBajaCliente(String rfc, {String? motivo}) {
    return _request(
      '/clientes',
      method: 'DELETE',
      body: {'rfc': rfc, 'motivo': ?motivo},
      auth: true,
    );
  }

  Future<Map<String, dynamic>> reactivarCliente(String rfc) {
    return _request('/clientes/reactivar', method: 'PATCH', body: {'rfc': rfc}, auth: true);
  }

  Future<List<CuentaData>> obtenerMisCuentas() async {
    final json = await _request('/cuentas/mias', method: 'GET', auth: true);
    final list = json['cuentas'] as List<dynamic>? ?? [];
    return list.map((e) => CuentaData.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<CuentaData> consultarCuenta(String numeroCuenta) async {
    final json = await _request(
      '/cuentas/consulta',
      method: 'POST',
      body: {'numeroCuenta': numeroCuenta},
      auth: true,
    );
    return CuentaData.fromJson(json['cuenta'] as Map<String, dynamic>);
  }

  Future<double> consultarSaldo(String numeroCuenta) async {
    final json = await _request(
      '/cuentas/saldo',
      method: 'POST',
      body: {'numeroCuenta': numeroCuenta},
      auth: true,
    );
    return (json['saldo'] as num?)?.toDouble() ?? 0.0;
  }

  Future<double> abonarSaldo(String numeroCuenta, double monto) async {
    final json = await _request(
      '/cuentas/abonar',
      method: 'POST',
      body: {'numeroCuenta': numeroCuenta, 'monto': monto},
      auth: true,
    );
    return (json['saldo'] as num?)?.toDouble() ?? 0.0;
  }

  Future<CatalogoServiciosResponse> buscarCatalogo({
    bool forzarRecarga = false,
    String? tipoOperacion,
    String? texto,
  }) async {
    final json = await _request(
      '/api/catalogo/buscar',
      method: 'POST',
      body: {
        'forzarRecarga': forzarRecarga,
        if (tipoOperacion != null && tipoOperacion.isNotEmpty)
          'tipoOperacion': tipoOperacion,
        if (texto != null && texto.isNotEmpty) 'texto': texto,
      },
      auth: true,
    );
    return CatalogoServiciosResponse.fromJson(json);
  }

  Future<ValidacionReferenciaResponse> validarReferencia(
      Map<String, dynamic> payload) async {
    final json = await _request(
      '/pagos/validar-referencia',
      method: 'POST',
      body: payload,
      auth: true,
    );
    return ValidacionReferenciaResponse.fromJson(json);
  }

  Future<Map<String, dynamic>> enviarPin2fa(String numeroCuenta) {
    return _request(
      '/pagos/enviar-pin',
      method: 'POST',
      body: {'numeroCuenta': numeroCuenta},
      auth: true,
    );
  }

  Future<PagoPuntoredResponse> realizarPagoPuntored(
      Map<String, dynamic> payload) async {
    final json = await _request(
      '/pagos/puntored',
      method: 'POST',
      body: payload,
      auth: true,
    );
    return PagoPuntoredResponse.fromJson(json);
  }

  Future<List<HistorialPagoItem>> consultarHistorialPagos({
    String? estado,
    String? tipoOperacion,
    String? numeroCuenta,
  }) async {
    final json = await _request(
      '/pagos/historial',
      method: 'POST',
      body: {
        if (estado != null && estado.isNotEmpty) 'estado': estado,
        if (tipoOperacion != null && tipoOperacion.isNotEmpty)
          'tipoOperacion': tipoOperacion,
        if (numeroCuenta != null && numeroCuenta.isNotEmpty)
          'numeroCuenta': numeroCuenta,
      },
      auth: true,
    );
    final list = json['transacciones'] as List<dynamic>? ?? [];
    return list
        .map((e) => HistorialPagoItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<DetallePagoResponsePayload> consultarDetallePago(
    int idTransaccion, {
    bool consultarEstadoProveedor = false,
  }) async {
    final json = await _request(
      '/pagos/detalle',
      method: 'POST',
      body: {
        'idTransaccion': idTransaccion,
        'consultarEstadoProveedor': consultarEstadoProveedor,
      },
      auth: true,
    );
    return DetallePagoResponsePayload(
      mensaje: json['mensaje']?.toString() ?? '',
      estadoActualizado: json['estadoActualizado'] == true,
      transaccion: HistorialPagoItem.fromJson(
        json['transaccion'] as Map<String, dynamic>,
      ),
    );
  }
}

class DetallePagoResponsePayload {
  final String mensaje;
  final bool estadoActualizado;
  final HistorialPagoItem transaccion;

  const DetallePagoResponsePayload({
    required this.mensaje,
    required this.estadoActualizado,
    required this.transaccion,
  });
}
