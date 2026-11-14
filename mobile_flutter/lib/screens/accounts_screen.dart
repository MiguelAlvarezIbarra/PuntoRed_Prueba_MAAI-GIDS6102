import 'package:flutter/material.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';

class AccountsScreen extends StatefulWidget {
  final AppState appState;
  final void Function(String numeroCuenta) onSelectForPayment;

  const AccountsScreen({
    super.key,
    required this.appState,
    required this.onSelectForPayment,
  });

  @override
  State<AccountsScreen> createState() => _AccountsScreenState();
}

class _AccountsScreenState extends State<AccountsScreen> {
  final _manualAccountCtrl = TextEditingController();
  final _depositAmountCtrl = TextEditingController();
  List<CuentaData> _cuentas = [];
  CuentaData? _selectedAccount;
  bool _loading = true;
  bool _operating = false;
  String? _error;
  String? _success;

  @override
  void initState() {
    super.initState();
    _cargarMisCuentas();
  }

  @override
  void dispose() {
    _manualAccountCtrl.dispose();
    _depositAmountCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarMisCuentas() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await widget.appState.api.obtenerMisCuentas();
      if (mounted) {
        setState(() {
          _cuentas = list;
          if (_selectedAccount != null) {
            final match = list.where((c) => c.numeroCuenta == _selectedAccount!.numeroCuenta);
            _selectedAccount = match.isNotEmpty ? match.first : (list.isNotEmpty ? list.first : null);
          } else if (list.isNotEmpty) {
            _selectedAccount = list.first;
          }
          if (_selectedAccount != null && _manualAccountCtrl.text.isEmpty) {
            _manualAccountCtrl.text = _selectedAccount!.numeroCuenta;
          }
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _consultarPorNumero() async {
    final numCuenta = _manualAccountCtrl.text.replaceAll(RegExp(r'\s+'), '');
    if (numCuenta.isEmpty) {
      setState(() => _error = 'Ingresa un número de cuenta de 18 dígitos.');
      return;
    }
    setState(() {
      _operating = true;
      _error = null;
      _success = null;
    });
    try {
      final cuenta = await widget.appState.api.consultarCuenta(numCuenta);
      if (mounted) {
        setState(() {
          _selectedAccount = cuenta;
          _success = 'Cuenta consultada: saldo ${AppFormatters.formatCurrency(cuenta.saldo)}';
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _operating = false);
    }
  }

  Future<void> _abonarSaldo() async {
    final targetCuenta = (_selectedAccount?.numeroCuenta ?? _manualAccountCtrl.text)
        .replaceAll(RegExp(r'\s+'), '');
    final monto = double.tryParse(_depositAmountCtrl.text.trim());
    if (targetCuenta.isEmpty) {
      setState(() => _error = 'Selecciona o ingresa una cuenta para abonar.');
      return;
    }
    if (monto == null || monto <= 0) {
      setState(() => _error = 'Ingresa un monto válido mayor a 0.');
      return;
    }
    setState(() {
      _operating = true;
      _error = null;
      _success = null;
    });
    try {
      final nuevoSaldo = await widget.appState.api.abonarSaldo(targetCuenta, monto);
      _depositAmountCtrl.clear();
      await _cargarMisCuentas();
      if (mounted) {
        setState(() {
          _success =
              'Abono exitoso. Nuevo saldo de cuenta: ${AppFormatters.formatCurrency(nuevoSaldo)}';
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _operating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return RefreshIndicator(
      onRefresh: _cargarMisCuentas,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Mis Cuentas', style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
              IconButton(
                tooltip: 'Actualizar saldos',
                onPressed: _loading ? null : _cargarMisCuentas,
                icon: const Icon(Icons.refresh_rounded),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (_error != null)
            Card(
              color: cs.errorContainer,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(_error!, style: TextStyle(color: cs.onErrorContainer)),
              ),
            ),
          if (_success != null)
            Card(
              color: const Color(0xFFD1FAE5),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(
                  _success!,
                  style: const TextStyle(color: Color(0xFF065F46), fontWeight: FontWeight.w600),
                ),
              ),
            ),
          if (_loading)
            const Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_cuentas.isEmpty)
            const Card(
              child: Padding(
                padding: EdgeInsets.all(18),
                child: Text('No tienes cuentas activas asociadas a tu perfil.'),
              ),
            )
          else
            for (final cuenta in _cuentas)
              Card(
                margin: const EdgeInsets.only(bottom: 10),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                  side: BorderSide(
                    color: _selectedAccount?.numeroCuenta == cuenta.numeroCuenta
                        ? cs.primary
                        : cs.outlineVariant,
                    width: _selectedAccount?.numeroCuenta == cuenta.numeroCuenta ? 2 : 1,
                  ),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            AppFormatters.formatAccountNumber(cuenta.numeroCuenta),
                            style: tt.titleSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              fontFamily: 'monospace',
                            ),
                          ),
                          Chip(
                            label: Text(cuenta.estatus),
                            visualDensity: VisualDensity.compact,
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        AppFormatters.formatCurrency(cuenta.saldo),
                        style: tt.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: cs.primary,
                        ),
                      ),
                      if (cuenta.nombreCliente != null)
                        Text(cuenta.nombreCliente!, style: tt.bodySmall),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8,
                        children: [
                          OutlinedButton.icon(
                            onPressed: () {
                              setState(() {
                                _selectedAccount = cuenta;
                                _manualAccountCtrl.text = cuenta.numeroCuenta;
                              });
                            },
                            icon: const Icon(Icons.touch_app_rounded, size: 18),
                            label: const Text('Seleccionar'),
                          ),
                          FilledButton.tonalIcon(
                            onPressed: () => widget.onSelectForPayment(cuenta.numeroCuenta),
                            icon: const Icon(Icons.payments_rounded, size: 18),
                            label: const Text('Pagar desde aquí'),
                          ),
                        ],
                      ),
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
                  Text(
                    'Consulta y Abono de Saldo',
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _manualAccountCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Número de cuenta (18 dígitos)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: _operating ? null : _consultarPorNumero,
                    icon: const Icon(Icons.search_rounded),
                    label: const Text('Consultar Saldo / Estatus'),
                  ),
                  const Divider(height: 24),
                  TextField(
                    controller: _depositAmountCtrl,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(
                      labelText: 'Monto a abonar (MXN)',
                      prefixText: '\$ ',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 10),
                  FilledButton.icon(
                    onPressed: _operating ? null : _abonarSaldo,
                    icon: _operating
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.add_card_rounded),
                    label: const Text('Abonar a Cuenta Seleccionada'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
