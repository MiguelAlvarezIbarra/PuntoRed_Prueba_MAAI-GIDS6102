import 'package:flutter/material.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';
import '../widgets/common_widgets.dart';

class PaymentScreen extends StatefulWidget {
  final AppState appState;
  final CatalogoServicioItem? selectedService;
  final String? preselectedAccount;
  final VoidCallback onPickFromCatalog;
  final VoidCallback onOpenHistory;

  const PaymentScreen({
    super.key,
    required this.appState,
    this.selectedService,
    this.preselectedAccount,
    required this.onPickFromCatalog,
    required this.onOpenHistory,
  });

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  final _ref1Ctrl = TextEditingController();
  final _ref2Ctrl = TextEditingController();
  final _montoCtrl = TextEditingController();
  final _iacCtrl = TextEditingController();

  List<CuentaData> _cuentas = [];
  List<CatalogoServicioItem> _catalogo = [];
  CuentaData? _cuentaSeleccionada;
  CatalogoServicioItem? _servicioSeleccionado;
  ValidacionReferenciaResponse? _validacion;
  OpcionMontoConvenio? _opcionSeleccionada;
  PagoPuntoredResponse? _resultadoPago;

  bool _loadingInit = true;
  bool _validando = false;
  bool _procesando = false;
  bool _consultandoEstado = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _servicioSeleccionado = widget.selectedService;
    if (_servicioSeleccionado?.precioSugerido != null) {
      _montoCtrl.text = _servicioSeleccionado!.precioSugerido!.toStringAsFixed(2);
    }
    _cargarInicial();
  }

  @override
  void didUpdateWidget(covariant PaymentScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.selectedService != null &&
        widget.selectedService!.claveUnica != _servicioSeleccionado?.claveUnica) {
      setState(() {
        _servicioSeleccionado = widget.selectedService;
        _validacion = null;
        _opcionSeleccionada = null;
        _resultadoPago = null;
        _error = null;
        _ref1Ctrl.clear();
        _ref2Ctrl.clear();
        _montoCtrl.text = widget.selectedService!.precioSugerido != null
            ? widget.selectedService!.precioSugerido!.toStringAsFixed(2)
            : '';
      });
    }
    if (widget.preselectedAccount != null && _cuentas.isNotEmpty) {
      final match = _cuentas.where((c) => c.numeroCuenta == widget.preselectedAccount);
      if (match.isNotEmpty) {
        setState(() => _cuentaSeleccionada = match.first);
      }
    }
  }

  @override
  void dispose() {
    _ref1Ctrl.dispose();
    _ref2Ctrl.dispose();
    _montoCtrl.dispose();
    _iacCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarInicial() async {
    setState(() {
      _loadingInit = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        widget.appState.api.obtenerMisCuentas(),
        widget.appState.api.buscarCatalogo(),
      ]);
      final cuentas = results[0] as List<CuentaData>;
      final catResp = results[1] as CatalogoServiciosResponse;

      if (mounted) {
        setState(() {
          _cuentas = cuentas;
          _catalogo = catResp.servicios;
          if (widget.preselectedAccount != null) {
            final match = cuentas.where((c) => c.numeroCuenta == widget.preselectedAccount);
            _cuentaSeleccionada = match.isNotEmpty
                ? match.first
                : (cuentas.isNotEmpty ? cuentas.first : null);
          } else if (cuentas.isNotEmpty) {
            _cuentaSeleccionada = cuentas.first;
          }
          _servicioSeleccionado ??=
              _catalogo.isNotEmpty ? _catalogo.first : null;
          if (_montoCtrl.text.isEmpty && _servicioSeleccionado?.precioSugerido != null) {
            _montoCtrl.text = _servicioSeleccionado!.precioSugerido!.toStringAsFixed(2);
          }
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loadingInit = false);
    }
  }

  void _invalidateReferenceValidation() {
    if (_validacion != null || _resultadoPago != null) {
      setState(() {
        _validacion = null;
        _opcionSeleccionada = null;
        _resultadoPago = null;
      });
    }
  }

  Future<void> _validarReferencia() async {
    final srv = _servicioSeleccionado;
    final ref1 = _ref1Ctrl.text.trim();
    if (srv == null) {
      setState(() => _error = 'Selecciona primero un servicio del catálogo.');
      return;
    }
    if (ref1.isEmpty) {
      setState(() => _error = 'Ingresa la referencia o línea a validar antes de pagar.');
      return;
    }
    setState(() {
      _validando = true;
      _error = null;
      _validacion = null;
      _opcionSeleccionada = null;
      _resultadoPago = null;
    });

    try {
      final resp = await widget.appState.api.validarReferencia({
        'tipoOperacion': srv.tipoOperacion,
        if (srv.idConvenio != null) 'idConvenio': srv.idConvenio,
        if (srv.codigoProducto != null) 'codigoProducto': srv.codigoProducto,
        if (srv.proveedorCodigo != null) 'codigoProveedor': srv.proveedorCodigo,
        'referencia1': ref1,
        if (_ref2Ctrl.text.trim().isNotEmpty) 'referencia2': _ref2Ctrl.text.trim(),
        if (double.tryParse(_montoCtrl.text.trim()) != null)
          'montoReferencia': double.parse(_montoCtrl.text.trim()),
        if (_iacCtrl.text.trim().isNotEmpty) 'codigoIac': _iacCtrl.text.trim(),
      });
      if (mounted) {
        setState(() {
          _validacion = resp;
          if (!resp.referenciaValida) {
            _error = resp.mensaje;
          } else {
            if (resp.opciones.isNotEmpty) {
              _opcionSeleccionada = resp.opciones.first;
              if (_opcionSeleccionada?.monto != null) {
                _montoCtrl.text = _opcionSeleccionada!.monto!.toStringAsFixed(2);
              }
            } else if (resp.montoSugerido != null) {
              _montoCtrl.text = resp.montoSugerido!.toStringAsFixed(2);
            }
          }
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _validando = false);
    }
  }

  Future<void> _abrirConfirmacionExplicita() async {
    final cuenta = _cuentaSeleccionada;
    final srv = _servicioSeleccionado;
    final val = _validacion;
    final monto = double.tryParse(_montoCtrl.text.trim());

    if (cuenta == null || srv == null || val == null || !val.referenciaValida) {
      setState(() => _error = 'Debes validar correctamente la referencia antes de pagar.');
      return;
    }
    if (monto == null || monto < 1) {
      setState(() => _error = 'El monto debe ser mayor o igual a \$1.00 MXN.');
      return;
    }
    if (monto > cuenta.saldo) {
      setState(() => _error = 'Saldo insuficiente en la cuenta seleccionada.');
      return;
    }

    final pinCtrl = TextEditingController();
    bool confirmedCheck = false;
    bool sendingPin = false;
    String? pinHint;
    String? dialogError;

    final confirmedPayload = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => AlertDialog(
          title: const Text('Confirmación Explícita de Pago'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    ServiceLogoWidget(
                      logoUrl: srv.logoUrl,
                      name: srv.nombreServicio,
                      tipoOperacion: srv.tipoOperacion,
                      size: 42,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(srv.nombreServicio,
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                          Text(srv.nombreProducto, style: Theme.of(ctx).textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ],
                ),
                const Divider(height: 18),
                _summaryRow('Cuenta origen', AppFormatters.formatAccountNumber(cuenta.numeroCuenta)),
                _summaryRow('Referencia', _ref1Ctrl.text.trim()),
                _summaryRow('Importe a pagar', AppFormatters.formatCurrency(monto)),
                _summaryRow('Nuevo saldo estimado',
                    AppFormatters.formatCurrency(cuenta.saldo - monto)),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: sendingPin
                      ? null
                      : () async {
                          setModalState(() {
                            sendingPin = true;
                            dialogError = null;
                          });
                          try {
                            final r = await widget.appState.api.enviarPin2fa(cuenta.numeroCuenta);
                            final demoPin = r['pin2fa']?.toString();
                            setModalState(() {
                              pinHint = demoPin != null && demoPin.isNotEmpty
                                  ? 'PIN enviado a ${r['correoDestino']} (Código: $demoPin)'
                                  : 'PIN 2FA enviado a ${r['correoDestino'] ?? 'tu correo'}.';
                            });
                          } catch (e) {
                            setModalState(() => dialogError = e.toString());
                          } finally {
                            setModalState(() => sendingPin = false);
                          }
                        },
                  icon: sendingPin
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.mark_email_read_rounded),
                  label: const Text('Enviar PIN 2FA a mi correo'),
                ),
                if (pinHint != null) ...[
                  const SizedBox(height: 6),
                  Text(
                    pinHint!,
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(ctx).colorScheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
                const SizedBox(height: 10),
                TextField(
                  controller: pinCtrl,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  decoration: const InputDecoration(
                    labelText: 'PIN de seguridad 2FA (6 dígitos)',
                    border: OutlineInputBorder(),
                    counterText: '',
                  ),
                ),
                const SizedBox(height: 8),
                CheckboxListTile(
                  value: confirmedCheck,
                  contentPadding: EdgeInsets.zero,
                  controlAffinity: ListTileControlAffinity.leading,
                  title: const Text(
                    'Confirmo explícitamente el servicio, referencia e importe de esta operación.',
                    style: TextStyle(fontSize: 13),
                  ),
                  onChanged: (v) => setModalState(() => confirmedCheck = v ?? false),
                ),
                if (dialogError != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      dialogError!,
                      style: TextStyle(color: Theme.of(ctx).colorScheme.error, fontSize: 12),
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(null),
              child: const Text('Cancelar'),
            ),
            FilledButton.icon(
              onPressed: () {
                final pin = pinCtrl.text.trim();
                if (!confirmedCheck) {
                  setModalState(() => dialogError = 'Marca la casilla de confirmación explícita.');
                  return;
                }
                if (!RegExp(r'^\d{6}$').hasMatch(pin)) {
                  setModalState(() => dialogError = 'Ingresa el PIN 2FA de 6 dígitos.');
                  return;
                }
                Navigator.of(ctx).pop(pin);
              },
              icon: const Icon(Icons.verified_user_rounded),
              label: const Text('Autorizar y Pagar'),
            ),
          ],
        ),
      ),
    );
    pinCtrl.dispose();

    if (confirmedPayload != null) {
      await _ejecutarPago(confirmedPayload);
    }
  }

  Future<void> _ejecutarPago(String pin2fa) async {
    final cuenta = _cuentaSeleccionada!;
    final srv = _servicioSeleccionado!;
    final val = _validacion!;
    final aval = val.camposAdicionalesAval ?? {};
    final monto = double.parse(_montoCtrl.text.trim());

    setState(() {
      _procesando = true;
      _error = null;
    });

    try {
      final resp = await widget.appState.api.realizarPagoPuntored({
        'numeroCuenta': cuenta.numeroCuenta,
        'tipoOperacion': srv.tipoOperacion,
        'nombreServicio': srv.nombreServicio,
        'nombreProducto': srv.nombreProducto,
        'categoria': srv.categoria,
        if (srv.logoUrl != null) 'logoUrl': srv.logoUrl,
        'referencia1': _ref1Ctrl.text.trim(),
        if (_ref2Ctrl.text.trim().isNotEmpty) 'referencia2': _ref2Ctrl.text.trim(),
        'monto': monto,
        if (srv.idConvenio != null) 'idConvenio': srv.idConvenio,
        if (srv.proveedorCodigo != null) 'codigoProveedor': srv.proveedorCodigo,
        if (srv.codigoCategoria != null) 'codigoCategoria': srv.codigoCategoria,
        if (srv.codigoProducto != null) 'codigoProducto': srv.codigoProducto,
        if (srv.sku != null) 'sku': srv.sku,
        if (srv.idPin != null) 'idPin': srv.idPin,
        if (srv.idSubcategoriaPin != null) 'idSubcategoriaPin': srv.idSubcategoriaPin,
        if (_opcionSeleccionada?.codigoRegistro != null)
          'codigoRegistro': _opcionSeleccionada!.codigoRegistro,
        if (_iacCtrl.text.trim().isNotEmpty) 'codigoIac': _iacCtrl.text.trim(),
        if (val.trace != null) 'traceValidacion': val.trace,
        'tokenValidacion': val.tokenValidacion ?? '',
        if (aval['montoQuery'] != null) 'montoQuery': aval['montoQuery'],
        if (aval['aditionalValue54Eco'] != null)
          'aditionalValue54Eco': aval['aditionalValue54Eco'],
        if (aval['aditionalValue62Eco'] != null)
          'aditionalValue62Eco': aval['aditionalValue62Eco'],
        if (aval['accountDataAcredited'] != null)
          'accountDataAcredited': aval['accountDataAcredited'],
        if (aval['additionalCompanyCode'] != null)
          'additionalCompanyCode': aval['additionalCompanyCode'],
        'confirmacionExplicita': true,
        'pin2fa': pin2fa,
      });

      await _refrescarSaldosSinBloquear();
      if (mounted) {
        setState(() {
          _resultadoPago = resp;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _procesando = false);
    }
  }

  Future<void> _refrescarSaldosSinBloquear() async {
    try {
      final cuentas = await widget.appState.api.obtenerMisCuentas();
      if (mounted) {
        setState(() {
          _cuentas = cuentas;
          if (_cuentaSeleccionada != null) {
            final m = cuentas.where((c) => c.numeroCuenta == _cuentaSeleccionada!.numeroCuenta);
            if (m.isNotEmpty) _cuentaSeleccionada = m.first;
          }
        });
      }
    } catch (_) {
      // Mantiene resultado del pago aunque falle recarga secundaria.
    }
  }

  Future<void> _verificarEstadoPendiente() async {
    final id = _resultadoPago?.idTransaccionLocal;
    if (id == null) return;
    setState(() {
      _consultandoEstado = true;
      _error = null;
    });
    try {
      final det = await widget.appState.api.consultarDetallePago(
        id,
        consultarEstadoProveedor: true,
      );
      final tx = det.transaccion;
      if (mounted) {
        setState(() {
          _resultadoPago = PagoPuntoredResponse(
            codigo: tx.estado == 'EXITOSA' ? 0 : (tx.estado == 'RECHAZADA' ? 29 : 30),
            mensaje: det.mensaje.isNotEmpty ? det.mensaje : tx.mensajeRespuesta,
            estado: tx.estado,
            transaccionConfirmada: tx.estado == 'EXITOSA',
            idTransaccionLocal: tx.id,
            idTransaccionProveedor: tx.idTransaccionProveedor,
            codigoAutorizacion: tx.codigoAutorizacion,
            trace: tx.trace,
            mensajeProveedor: tx.mensajeRespuesta,
            numeroCuenta: tx.numeroCuenta,
            nombreServicio: tx.nombreServicio,
            nombreProducto: tx.nombreProducto,
            logoUrl: tx.logoUrl,
            referencia1: tx.referencia1,
            monto: tx.monto,
            saldoAnterior: _resultadoPago!.saldoAnterior,
            saldoNuevo: _resultadoPago!.saldoNuevo,
            ticket: tx.ticket,
            fechaRegistro: tx.fechaRegistro,
          );
        });
      }
      await _refrescarSaldosSinBloquear();
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _consultandoEstado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    if (_loadingInit) {
      return const Center(child: CircularProgressIndicator());
    }

    final validacionOk = _validacion?.referenciaValida == true;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Realizar Pago', style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
            TextButton.icon(
              onPressed: widget.onPickFromCatalog,
              icon: const Icon(Icons.grid_view_rounded),
              label: const Text('Catálogo'),
            ),
          ],
        ),
        if (_error != null)
          Card(
            color: cs.errorContainer,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Text(_error!, style: TextStyle(color: cs.onErrorContainer)),
            ),
          ),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('1. Cuenta de cargo y Servicio',
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _cuentaSeleccionada?.numeroCuenta,
                  decoration: const InputDecoration(
                    labelText: 'Cuenta de origen',
                    border: OutlineInputBorder(),
                  ),
                  items: _cuentas
                      .map((c) => DropdownMenuItem(
                            value: c.numeroCuenta,
                            child: Text(
                              '${AppFormatters.formatAccountNumber(c.numeroCuenta)} (${AppFormatters.formatCurrency(c.saldo)})',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ))
                      .toList(),
                  onChanged: (val) {
                    setState(() {
                      _cuentaSeleccionada = _cuentas.firstWhere((c) => c.numeroCuenta == val);
                    });
                  },
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _servicioSeleccionado?.claveUnica,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    labelText: 'Servicio / Producto Puntored',
                    border: OutlineInputBorder(),
                  ),
                  items: _catalogo
                      .map((s) => DropdownMenuItem(
                            value: s.claveUnica,
                            child: Text(
                              '${s.nombreServicio} • ${s.nombreProducto} (${s.tipoOperacion})',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ))
                      .toList(),
                  onChanged: (val) {
                    final found = _catalogo.firstWhere((s) => s.claveUnica == val);
                    setState(() {
                      _servicioSeleccionado = found;
                      _validacion = null;
                      _opcionSeleccionada = null;
                      _resultadoPago = null;
                      _error = null;
                      _montoCtrl.text = found.precioSugerido != null
                          ? found.precioSugerido!.toStringAsFixed(2)
                          : '';
                    });
                  },
                ),
                if (_servicioSeleccionado != null) ...[
                  const SizedBox(height: 12),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: ServiceLogoWidget(
                      logoUrl: _servicioSeleccionado!.logoUrl,
                      name: _servicioSeleccionado!.nombreServicio,
                      tipoOperacion: _servicioSeleccionado!.tipoOperacion,
                    ),
                    title: Text(_servicioSeleccionado!.nombreServicio,
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text(
                      '${_servicioSeleccionado!.categoria} • ${_servicioSeleccionado!.nombreProducto}',
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('2. Consulta y Validación de Referencia',
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(
                  'Es obligatorio validar la referencia en el proveedor antes de habilitar el pago.',
                  style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                ),
                const SizedBox(height: 12),
                TextField(
                  key: const ValueKey('payReferenceField'),
                  controller: _ref1Ctrl,
                  onChanged: (_) => _invalidateReferenceValidation(),
                  decoration: InputDecoration(
                    labelText: _servicioSeleccionado?.etiquetaReferencia1 ??
                        (_servicioSeleccionado?.tipoOperacion == 'RECARGA' ||
                                _servicioSeleccionado?.tipoOperacion == 'PAQUETE'
                            ? 'Número celular (10 dígitos)'
                            : 'Referencia o número de contrato'),
                    border: const OutlineInputBorder(),
                  ),
                ),
                if (_servicioSeleccionado?.etiquetaReferencia2 != null ||
                    _servicioSeleccionado?.tipoOperacion == 'RECAUDO_V2') ...[
                  const SizedBox(height: 10),
                  TextField(
                    controller: _ref2Ctrl,
                    onChanged: (_) => _invalidateReferenceValidation(),
                    decoration: InputDecoration(
                      labelText:
                          _servicioSeleccionado?.etiquetaReferencia2 ?? 'Referencia secundaria',
                      border: const OutlineInputBorder(),
                    ),
                  ),
                ],
                if (_servicioSeleccionado?.tipoOperacion == 'RECAUDO_V2') ...[
                  const SizedBox(height: 10),
                  TextField(
                    controller: _iacCtrl,
                    onChanged: (_) => _invalidateReferenceValidation(),
                    decoration: const InputDecoration(
                      labelText: 'Código IAC / Barra (opcional)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                FilledButton.tonalIcon(
                  key: const ValueKey('validateRefButton'),
                  onPressed: _validando ? null : _validarReferencia,
                  icon: _validando
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.fact_check_rounded),
                  label: Text(_validando ? 'Validando con Puntored...' : 'Validar Referencia'),
                ),
                if (validacionOk) ...[
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFD1FAE5),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.verified_rounded, color: Color(0xFF065F46), size: 18),
                            SizedBox(width: 6),
                            Text(
                              'Referencia válida — Pago habilitado',
                              style: TextStyle(
                                color: Color(0xFF065F46),
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                        if (_validacion?.mensajeConvenio != null)
                          Text(
                            _validacion!.mensajeConvenio!,
                            style: const TextStyle(color: Color(0xFF065F46), fontSize: 12),
                          ),
                        if (_validacion?.trace != null)
                          Text(
                            'Trace validación: ${_validacion!.trace}',
                            style: const TextStyle(color: Color(0xFF065F46), fontSize: 12),
                          ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('3. Importe y Confirmación',
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                if (_validacion != null && _validacion!.opciones.isNotEmpty) ...[
                  DropdownButtonFormField<String>(
                    initialValue: _opcionSeleccionada?.codigoRegistro,
                    decoration: const InputDecoration(
                      labelText: 'Opción devuelta por el convenio',
                      border: OutlineInputBorder(),
                    ),
                    items: _validacion!.opciones
                        .map((o) => DropdownMenuItem(
                              value: o.codigoRegistro ?? '',
                              child: Text(
                                '${o.descripcionRegistro ?? 'Opción'} (${AppFormatters.formatCurrency(o.monto)})',
                              ),
                            ))
                        .toList(),
                    onChanged: (val) {
                      final op = _validacion!.opciones
                          .firstWhere((o) => (o.codigoRegistro ?? '') == val);
                      setState(() {
                        _opcionSeleccionada = op;
                        if (op.monto != null) {
                          _montoCtrl.text = op.monto!.toStringAsFixed(2);
                        }
                      });
                    },
                  ),
                  const SizedBox(height: 12),
                ],
                TextField(
                  controller: _montoCtrl,
                  readOnly: !(_validacion?.montoEditable ?? true),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: InputDecoration(
                    labelText: 'Monto a pagar (MXN)',
                    prefixText: '\$ ',
                    helperText: (_validacion?.montoMinimo != null ||
                            _validacion?.montoMaximo != null)
                        ? 'Rango permitido: ${AppFormatters.formatCurrency(_validacion?.montoMinimo)} a ${AppFormatters.formatCurrency(_validacion?.montoMaximo)}'
                        : null,
                    border: const OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 14),
                FilledButton.icon(
                  key: const ValueKey('openConfirmPaymentButton'),
                  onPressed: (!validacionOk || _procesando) ? null : _abrirConfirmacionExplicita,
                  icon: _procesando
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.lock_clock_rounded),
                  label: Text(
                    validacionOk
                        ? (_procesando ? 'Procesando pago...' : 'Continuar a Confirmación')
                        : 'Valida la referencia para continuar',
                  ),
                ),
              ],
            ),
          ),
        ),
        if (_resultadoPago != null) ...[
          const SizedBox(height: 12),
          _buildResultCard(_resultadoPago!, tt, cs),
        ],
      ],
    );
  }

  Widget _buildResultCard(PagoPuntoredResponse r, TextTheme tt, ColorScheme cs) {
    final norm = r.estado.toUpperCase();
    final isOk = norm == 'EXITOSA';
    final isReject = norm == 'RECHAZADA';

    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(
          color: isOk
              ? const Color(0xFF10B981)
              : (isReject ? cs.error : const Color(0xFFF59E0B)),
          width: 1.5,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Resultado de la Operación',
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                StatusBadge(estado: r.estado),
              ],
            ),
            const SizedBox(height: 8),
            Text(r.mensaje, style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.w600)),
            const Divider(height: 18),
            _summaryRow('Servicio', r.nombreServicio),
            _summaryRow('Referencia', r.referencia1),
            _summaryRow('Importe', AppFormatters.formatCurrency(r.monto)),
            if (r.codigoAutorizacion != null && r.codigoAutorizacion!.isNotEmpty)
              _summaryRow('Autorización / Folio', r.codigoAutorizacion!),
            if (r.idTransaccionProveedor != null && r.idTransaccionProveedor!.isNotEmpty)
              _summaryRow('ID Proveedor', r.idTransaccionProveedor!),
            if (r.trace != null && r.trace!.isNotEmpty) _summaryRow('Trace', r.trace!),
            if (r.pinContenido != null && r.pinContenido!.isNotEmpty)
              _summaryRow('Contenido PIN', r.pinContenido!),
            _summaryRow('Saldo anterior', AppFormatters.formatCurrency(r.saldoAnterior)),
            _summaryRow('Saldo actual', AppFormatters.formatCurrency(r.saldoNuevo)),
            if (!isOk && !isReject) ...[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF3C7),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Text(
                  'Esta operación no ha sido confirmada por el proveedor. No se marca como exitosa hasta recibir confirmación definitiva.',
                  style: TextStyle(color: Color(0xFF92400E), fontSize: 12),
                ),
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: _consultandoEstado ? null : _verificarEstadoPendiente,
                icon: _consultandoEstado
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.sync_rounded),
                label: const Text('Consultar Estado en Tiempo Real'),
              ),
            ],
            if (r.ticket != null) ...[
              const SizedBox(height: 12),
              TicketCard(ticket: r.ticket!),
            ],
            const SizedBox(height: 10),
            TextButton.icon(
              onPressed: widget.onOpenHistory,
              icon: const Icon(Icons.history_rounded),
              label: const Text('Ver en Historial de Pagos'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _summaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.grey, fontSize: 13)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }
}
