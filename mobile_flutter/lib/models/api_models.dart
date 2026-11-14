class AuthSession {
  final String jwt;
  final int clienteId;
  final int rol;
  final String correo;
  final int expiraEnSegundos;
  final int loginAtMs;

  const AuthSession({
    required this.jwt,
    required this.clienteId,
    required this.rol,
    required this.correo,
    required this.expiraEnSegundos,
    required this.loginAtMs,
  });

  bool get isAdmin => rol == 1;

  Map<String, dynamic> toJson() => {
        'jwt': jwt,
        'clienteId': clienteId,
        'rol': rol,
        'correo': correo,
        'expiraEnSegundos': expiraEnSegundos,
        'loginAtMs': loginAtMs,
      };

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      jwt: json['jwt']?.toString() ?? '',
      clienteId: _asInt(json['clienteId']),
      rol: _asInt(json['rol'], fallback: 2),
      correo: json['correo']?.toString() ?? '',
      expiraEnSegundos: _asInt(json['expiraEnSegundos'], fallback: 900),
      loginAtMs: _asInt(json['loginAtMs']),
    );
  }
}

class ClienteData {
  final int id;
  final String nombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final String rfc;
  final String curp;
  final String correo;
  final String telefono;
  final String? numeroCuenta;
  final bool activo;

  const ClienteData({
    required this.id,
    required this.nombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.rfc,
    required this.curp,
    required this.correo,
    required this.telefono,
    this.numeroCuenta,
    required this.activo,
  });

  String get nombreCompleto => '$nombre $apellidoPaterno $apellidoMaterno'.trim();

  factory ClienteData.fromJson(Map<String, dynamic> json) {
    return ClienteData(
      id: _asInt(json['id']),
      nombre: json['nombre']?.toString() ?? '',
      apellidoPaterno: json['apellidoPaterno']?.toString() ?? '',
      apellidoMaterno: json['apellidoMaterno']?.toString() ?? '',
      rfc: json['rfc']?.toString() ?? '',
      curp: json['curp']?.toString() ?? '',
      correo: json['correo']?.toString() ?? '',
      telefono: json['telefono']?.toString() ?? '',
      numeroCuenta: json['numeroCuenta']?.toString(),
      activo: json['activo'] != false,
    );
  }
}

class CuentaData {
  final int id;
  final int clienteId;
  final String? nombreCliente;
  final String numeroCuenta;
  final double saldo;
  final String estatus;
  final String fechaApertura;

  const CuentaData({
    required this.id,
    required this.clienteId,
    this.nombreCliente,
    required this.numeroCuenta,
    required this.saldo,
    required this.estatus,
    required this.fechaApertura,
  });

  factory CuentaData.fromJson(Map<String, dynamic> json) {
    return CuentaData(
      id: _asInt(json['id']),
      clienteId: _asInt(json['clienteId']),
      nombreCliente: json['nombreCliente']?.toString(),
      numeroCuenta: json['numeroCuenta']?.toString() ?? '',
      saldo: _asDouble(json['saldo']) ?? 0.0,
      estatus: json['estatus']?.toString() ?? 'ACTIVA',
      fechaApertura: json['fechaApertura']?.toString() ?? '',
    );
  }
}

class CatalogoServicioItem {
  final String claveUnica;
  final String tipoOperacion;
  final String categoria;
  final String nombreServicio;
  final String nombreProducto;
  final String? descripcion;
  final String? proveedorCodigo;
  final String? proveedorNombre;
  final String? logoUrl;
  final String? codigoProducto;
  final String? codigoCategoria;
  final String? idConvenio;
  final String? sku;
  final String? idPin;
  final String? idSubcategoriaPin;
  final double? precioSugerido;
  final String? validez;
  final String? etiquetaReferencia1;
  final String? etiquetaReferencia2;
  final String? nombreBanco;

  const CatalogoServicioItem({
    required this.claveUnica,
    required this.tipoOperacion,
    required this.categoria,
    required this.nombreServicio,
    required this.nombreProducto,
    this.descripcion,
    this.proveedorCodigo,
    this.proveedorNombre,
    this.logoUrl,
    this.codigoProducto,
    this.codigoCategoria,
    this.idConvenio,
    this.sku,
    this.idPin,
    this.idSubcategoriaPin,
    this.precioSugerido,
    this.validez,
    this.etiquetaReferencia1,
    this.etiquetaReferencia2,
    this.nombreBanco,
  });

  factory CatalogoServicioItem.fromJson(Map<String, dynamic> json) {
    return CatalogoServicioItem(
      claveUnica: json['claveUnica']?.toString() ?? '',
      tipoOperacion: json['tipoOperacion']?.toString() ?? '',
      categoria: json['categoria']?.toString() ?? 'Servicios',
      nombreServicio: json['nombreServicio']?.toString() ?? '',
      nombreProducto: json['nombreProducto']?.toString() ?? '',
      descripcion: json['descripcion']?.toString(),
      proveedorCodigo: json['proveedorCodigo']?.toString(),
      proveedorNombre: json['proveedorNombre']?.toString(),
      logoUrl: json['logoUrl']?.toString(),
      codigoProducto: json['codigoProducto']?.toString(),
      codigoCategoria: json['codigoCategoria']?.toString(),
      idConvenio: json['idConvenio']?.toString(),
      sku: json['sku']?.toString(),
      idPin: json['idPin']?.toString(),
      idSubcategoriaPin: json['idSubcategoriaPin']?.toString(),
      precioSugerido: _asDouble(json['precioSugerido']),
      validez: json['validez']?.toString(),
      etiquetaReferencia1: json['etiquetaReferencia1']?.toString(),
      etiquetaReferencia2: json['etiquetaReferencia2']?.toString(),
      nombreBanco: json['nombreBanco']?.toString(),
    );
  }
}

class CatalogoServiciosResponse {
  final String fuenteDatos;
  final String? saldoComercio;
  final List<CatalogoServicioItem> servicios;

  const CatalogoServiciosResponse({
    required this.fuenteDatos,
    this.saldoComercio,
    required this.servicios,
  });

  factory CatalogoServiciosResponse.fromJson(Map<String, dynamic> json) {
    final list = (json['servicios'] as List<dynamic>? ?? [])
        .map((e) => CatalogoServicioItem.fromJson(e as Map<String, dynamic>))
        .toList();
    return CatalogoServiciosResponse(
      fuenteDatos: json['fuenteDatos']?.toString() ?? 'PUNTORED_H2H_V2',
      saldoComercio: json['saldoComercio']?.toString(),
      servicios: list,
    );
  }
}

class OpcionMontoConvenio {
  final String? codigoRegistro;
  final String? descripcionRegistro;
  final double? monto;
  final int? tipoMonto;

  const OpcionMontoConvenio({
    this.codigoRegistro,
    this.descripcionRegistro,
    this.monto,
    this.tipoMonto,
  });

  factory OpcionMontoConvenio.fromJson(Map<String, dynamic> json) {
    return OpcionMontoConvenio(
      codigoRegistro: json['codigoRegistro']?.toString(),
      descripcionRegistro: json['descripcionRegistro']?.toString(),
      monto: _asDouble(json['monto']),
      tipoMonto: json['tipoMonto'] == null ? null : _asInt(json['tipoMonto']),
    );
  }
}

class ValidacionReferenciaResponse {
  final int codigo;
  final String mensaje;
  final bool referenciaValida;
  final String? tokenValidacion;
  final String tipoOperacion;
  final String referencia1;
  final String? mensajeConvenio;
  final double? montoSugerido;
  final double? montoMinimo;
  final double? montoMaximo;
  final bool montoEditable;
  final String? trace;
  final List<OpcionMontoConvenio> opciones;
  final Map<String, dynamic>? camposAdicionalesAval;

  const ValidacionReferenciaResponse({
    required this.codigo,
    required this.mensaje,
    required this.referenciaValida,
    this.tokenValidacion,
    required this.tipoOperacion,
    required this.referencia1,
    this.mensajeConvenio,
    this.montoSugerido,
    this.montoMinimo,
    this.montoMaximo,
    required this.montoEditable,
    this.trace,
    required this.opciones,
    this.camposAdicionalesAval,
  });

  factory ValidacionReferenciaResponse.fromJson(Map<String, dynamic> json) {
    final ops = (json['opciones'] as List<dynamic>? ?? [])
        .map((e) => OpcionMontoConvenio.fromJson(e as Map<String, dynamic>))
        .toList();
    return ValidacionReferenciaResponse(
      codigo: _asInt(json['codigo']),
      mensaje: json['mensaje']?.toString() ?? '',
      referenciaValida: json['referenciaValida'] == true,
      tokenValidacion: json['tokenValidacion']?.toString(),
      tipoOperacion: json['tipoOperacion']?.toString() ?? '',
      referencia1: json['referencia1']?.toString() ?? '',
      mensajeConvenio: json['mensajeConvenio']?.toString(),
      montoSugerido: _asDouble(json['montoSugerido']),
      montoMinimo: _asDouble(json['montoMinimo']),
      montoMaximo: _asDouble(json['montoMaximo']),
      montoEditable: json['montoEditable'] != false,
      trace: json['trace']?.toString(),
      opciones: ops,
      camposAdicionalesAval: json['camposAdicionalesAval'] as Map<String, dynamic>?,
    );
  }
}

class TicketData {
  final String? encabezado;
  final String? titulo;
  final Map<String, dynamic>? cuerpo;
  final String? piePagina;
  final String? logoUrl;

  const TicketData({
    this.encabezado,
    this.titulo,
    this.cuerpo,
    this.piePagina,
    this.logoUrl,
  });

  factory TicketData.fromJson(Map<String, dynamic> json) {
    return TicketData(
      encabezado: json['encabezado']?.toString(),
      titulo: json['titulo']?.toString(),
      cuerpo: json['cuerpo'] as Map<String, dynamic>?,
      piePagina: json['piePagina']?.toString(),
      logoUrl: json['logoUrl']?.toString(),
    );
  }
}

class PagoPuntoredResponse {
  final int codigo;
  final String mensaje;
  final String estado;
  final bool transaccionConfirmada;
  final int? idTransaccionLocal;
  final String? idTransaccionProveedor;
  final String? codigoAutorizacion;
  final String? trace;
  final String? pinContenido;
  final String? mensajeProveedor;
  final String numeroCuenta;
  final String nombreServicio;
  final String? nombreProducto;
  final String? logoUrl;
  final String referencia1;
  final double monto;
  final double saldoAnterior;
  final double saldoNuevo;
  final TicketData? ticket;
  final String? fechaRegistro;

  const PagoPuntoredResponse({
    required this.codigo,
    required this.mensaje,
    required this.estado,
    required this.transaccionConfirmada,
    this.idTransaccionLocal,
    this.idTransaccionProveedor,
    this.codigoAutorizacion,
    this.trace,
    this.pinContenido,
    this.mensajeProveedor,
    required this.numeroCuenta,
    required this.nombreServicio,
    this.nombreProducto,
    this.logoUrl,
    required this.referencia1,
    required this.monto,
    required this.saldoAnterior,
    required this.saldoNuevo,
    this.ticket,
    this.fechaRegistro,
  });

  factory PagoPuntoredResponse.fromJson(Map<String, dynamic> json) {
    return PagoPuntoredResponse(
      codigo: _asInt(json['codigo']),
      mensaje: json['mensaje']?.toString() ?? '',
      estado: json['estado']?.toString() ?? 'PENDIENTE',
      transaccionConfirmada: json['transaccionConfirmada'] == true,
      idTransaccionLocal:
          json['idTransaccionLocal'] == null ? null : _asInt(json['idTransaccionLocal']),
      idTransaccionProveedor: json['idTransaccionProveedor']?.toString(),
      codigoAutorizacion: json['codigoAutorizacion']?.toString(),
      trace: json['trace']?.toString(),
      pinContenido: json['pinContenido']?.toString(),
      mensajeProveedor: json['mensajeProveedor']?.toString(),
      numeroCuenta: json['numeroCuenta']?.toString() ?? '',
      nombreServicio: json['nombreServicio']?.toString() ?? '',
      nombreProducto: json['nombreProducto']?.toString(),
      logoUrl: json['logoUrl']?.toString(),
      referencia1: json['referencia1']?.toString() ?? '',
      monto: _asDouble(json['monto']) ?? 0.0,
      saldoAnterior: _asDouble(json['saldoAnterior']) ?? 0.0,
      saldoNuevo: _asDouble(json['saldoNuevo']) ?? 0.0,
      ticket: json['ticket'] is Map<String, dynamic>
          ? TicketData.fromJson(json['ticket'] as Map<String, dynamic>)
          : null,
      fechaRegistro: json['fechaRegistro']?.toString(),
    );
  }
}

class HistorialPagoItem {
  final int id;
  final String numeroCuenta;
  final String proveedor;
  final String tipoOperacion;
  final String nombreServicio;
  final String? nombreProducto;
  final String? categoria;
  final String? logoUrl;
  final String referencia1;
  final double monto;
  final String? idTransaccionProveedor;
  final String? codigoAutorizacion;
  final String? trace;
  final String codigoRespuesta;
  final String mensajeRespuesta;
  final String estado;
  final TicketData? ticket;
  final String fechaRegistro;

  const HistorialPagoItem({
    required this.id,
    required this.numeroCuenta,
    required this.proveedor,
    required this.tipoOperacion,
    required this.nombreServicio,
    this.nombreProducto,
    this.categoria,
    this.logoUrl,
    required this.referencia1,
    required this.monto,
    this.idTransaccionProveedor,
    this.codigoAutorizacion,
    this.trace,
    required this.codigoRespuesta,
    required this.mensajeRespuesta,
    required this.estado,
    this.ticket,
    required this.fechaRegistro,
  });

  factory HistorialPagoItem.fromJson(Map<String, dynamic> json) {
    return HistorialPagoItem(
      id: _asInt(json['id']),
      numeroCuenta: json['numeroCuenta']?.toString() ?? '',
      proveedor: json['proveedor']?.toString() ?? 'PUNTORED',
      tipoOperacion: json['tipoOperacion']?.toString() ?? '',
      nombreServicio: json['nombreServicio']?.toString() ?? '',
      nombreProducto: json['nombreProducto']?.toString(),
      categoria: json['categoria']?.toString(),
      logoUrl: json['logoUrl']?.toString(),
      referencia1: json['referencia1']?.toString() ?? '',
      monto: _asDouble(json['monto']) ?? 0.0,
      idTransaccionProveedor: json['idTransaccionProveedor']?.toString(),
      codigoAutorizacion: json['codigoAutorizacion']?.toString(),
      trace: json['trace']?.toString(),
      codigoRespuesta: json['codigoRespuesta']?.toString() ?? '',
      mensajeRespuesta: json['mensajeRespuesta']?.toString() ?? '',
      estado: json['estado']?.toString() ?? 'EXITOSA',
      ticket: json['ticket'] is Map<String, dynamic>
          ? TicketData.fromJson(json['ticket'] as Map<String, dynamic>)
          : null,
      fechaRegistro: json['fechaRegistro']?.toString() ?? '',
    );
  }
}

int _asInt(dynamic value, {int fallback = 0}) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? fallback;
}

double? _asDouble(dynamic value) {
  if (value == null) return null;
  if (value is double) return value;
  if (value is num) return value.toDouble();
  return double.tryParse(value.toString());
}
