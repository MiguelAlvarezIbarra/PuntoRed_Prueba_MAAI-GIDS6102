import 'package:flutter/material.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';

class DashboardScreen extends StatefulWidget {
  final AppState appState;
  final void Function(int tabIndex, {String? accountNumber}) onNavigate;

  const DashboardScreen({
    super.key,
    required this.appState,
    required this.onNavigate,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool _loading = true;
  String? _error;
  List<ClienteData> _clientes = [];
  List<CuentaData> _cuentas = [];
  int _totalServicios = 0;
  int _totalPagos = 0;

  @override
  void initState() {
    super.initState();
    _cargarResumen();
  }

  Future<void> _cargarResumen() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        widget.appState.api.buscarClientes(),
        widget.appState.api.obtenerMisCuentas().catchError((_) => <CuentaData>[]),
        widget.appState.api
            .buscarCatalogo()
            .then((r) => r.servicios.length)
            .catchError((_) => 0),
        widget.appState.api
            .consultarHistorialPagos()
            .then((r) => r.length)
            .catchError((_) => 0),
      ]);
      if (!mounted) return;
      setState(() {
        _clientes = results[0] as List<ClienteData>;
        _cuentas = results[1] as List<CuentaData>;
        _totalServicios = results[2] as int;
        _totalPagos = results[3] as int;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final session = widget.appState.session;
    final cliente = _clientes.isNotEmpty ? _clientes.first : null;
    final saldoTotal = _cuentas.fold<double>(0.0, (acc, c) => acc + c.saldo);
    final cuentaPrincipal = _cuentas.isNotEmpty ? _cuentas.first.numeroCuenta : cliente?.numeroCuenta;

    return RefreshIndicator(
      onRefresh: _cargarResumen,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_error != null)
            Card(
              color: cs.errorContainer,
              margin: const EdgeInsets.only(bottom: 12),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(_error!, style: TextStyle(color: cs.onErrorContainer)),
              ),
            ),

          // Hero Financiero
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [cs.primary, cs.primary.withValues(alpha: 0.78)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Chip(
                      label: Text(
                        widget.appState.isAdmin ? 'Administrador' : 'Cliente Verificado',
                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'ID #${session?.clienteId ?? 0}',
                      style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.85)),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  'Hola${cliente != null ? ', ${cliente.nombre}' : ''}',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        color: cs.onPrimary,
                        fontWeight: FontWeight.bold,
                      ),
                ),
                const SizedBox(height: 4),
                Text(
                  widget.appState.isAdmin
                      ? 'Saldo global en cuentas activas'
                      : 'Tu saldo disponible',
                  style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.85)),
                ),
                const SizedBox(height: 4),
                Text(
                  _loading ? '...' : AppFormatters.formatCurrency(saldoTotal),
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        color: cs.onPrimary,
                        fontWeight: FontWeight.w800,
                      ),
                ),
                if (cuentaPrincipal != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Cuenta: ${AppFormatters.formatAccountNumber(cuentaPrincipal)}',
                    style: TextStyle(
                      color: cs.onPrimary.withValues(alpha: 0.9),
                      fontFamily: 'monospace',
                    ),
                  ),
                ],
              ],
            ),
          ),

          const SizedBox(height: 16),

          // KPIs
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 1.75,
            children: [
              _KpiCard(
                title: 'Cuentas Activas',
                value: _loading ? '—' : '${_cuentas.length}',
                icon: Icons.account_balance_wallet_rounded,
              ),
              _KpiCard(
                title: 'Catálogo PuntoRed',
                value: _loading ? '—' : '$_totalServicios',
                icon: Icons.grid_view_rounded,
              ),
              _KpiCard(
                title: 'Pagos Realizados',
                value: _loading ? '—' : '$_totalPagos',
                icon: Icons.history_rounded,
              ),
              _KpiCard(
                title: widget.appState.isAdmin ? 'Clientes' : 'Mi Perfil',
                value: _loading ? '—' : '${_clientes.length}',
                icon: Icons.people_alt_rounded,
              ),
            ],
          ),

          const SizedBox(height: 20),
          Text(
            'Operaciones Principales',
            style: Theme.of(context)
                .textTheme
                .titleMedium
                ?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),

          _QuickActionTile(
            icon: Icons.grid_view_rounded,
            title: 'Explorar Catálogo de Servicios',
            subtitle: 'Recargas, paquetes, recaudos y pines con logos reales',
            onTap: () => widget.onNavigate(1),
          ),
          _QuickActionTile(
            icon: Icons.verified_user_rounded,
            title: 'Validar Referencia y Pagar',
            subtitle: 'Consulta referencia, confirma resumen y autoriza con PIN 2FA',
            onTap: () => widget.onNavigate(2, accountNumber: cuentaPrincipal),
          ),
          _QuickActionTile(
            icon: Icons.receipt_long_rounded,
            title: 'Historial y Comprobantes',
            subtitle: 'Consulta folios, tirillas digitales y verifica pagos pendientes',
            onTap: () => widget.onNavigate(3),
          ),
          _QuickActionTile(
            icon: Icons.savings_rounded,
            title: 'Cuentas, Saldos y Abonos',
            subtitle: 'Consulta tu cuenta de 18 dígitos y realiza abonos en línea',
            onTap: () => widget.onNavigate(4, accountNumber: cuentaPrincipal),
          ),
          const SizedBox(height: 10),
          _BiometricSecurityCard(appState: widget.appState),
        ],
      ),
    );
  }
}

class _KpiCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;

  const _KpiCard({
    required this.title,
    required this.value,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Row(
              children: [
                Icon(icon, color: cs.primary, size: 20),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              value,
              style: Theme.of(context)
                  .textTheme
                  .titleLarge
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickActionTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _QuickActionTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: CircleAvatar(child: Icon(icon)),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: onTap,
      ),
    );
  }
}

class _BiometricSecurityCard extends StatefulWidget {
  final AppState appState;

  const _BiometricSecurityCard({required this.appState});

  @override
  State<_BiometricSecurityCard> createState() => _BiometricSecurityCardState();
}

class _BiometricSecurityCardState extends State<_BiometricSecurityCard> {
  bool _busy = false;

  Future<String?> _askCurrentPassword() async {
    final ctrl = TextEditingController();
    final pwd = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Activar 2FA Biométrico'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Confirma tu contraseña una vez para resguardarla cifrada en Android Keystore junto con tu huella o Face ID.',
            ),
            const SizedBox(height: 12),
            TextField(
              controller: ctrl,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Contraseña actual',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(null),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(ctrl.text),
            child: const Text('Continuar'),
          ),
        ],
      ),
    );
    ctrl.dispose();
    return pwd;
  }

  Future<void> _onToggle(bool value) async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      String? confirmPwd;
      if (value) {
        final sameAccount = widget.appState.hasSavedBiometricAccount &&
            widget.appState.savedBiometricEmail ==
                widget.appState.session?.correo;
        if (!sameAccount) {
          confirmPwd = await _askCurrentPassword();
          if (confirmPwd == null || confirmPwd.isEmpty) {
            return;
          }
        }
      }
      await widget.appState.setBiometric2faEnabled(
        value,
        confirmPassword: confirmPwd,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              value
                  ? '2FA con Huella / Face ID activado correctamente.'
                  : '2FA biométrico desactivado.',
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;

    return ListenableBuilder(
      listenable: widget.appState,
      builder: (context, _) {
        final appState = widget.appState;
        final status = appState.biometricStatus;
        final isReady = appState.biometricAvailable;
        final isActive = isReady && appState.biometric2faEnabled;
        final currentEmail = appState.session?.correo;

        return Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      backgroundColor: isActive
                          ? const Color(0xFFD1FAE5)
                          : cs.surfaceContainerHighest,
                      foregroundColor:
                          isActive ? const Color(0xFF065F46) : cs.onSurface,
                      child: Icon(
                        status.supportsFace && !status.supportsFingerprint
                            ? Icons.face_retouching_natural_rounded
                            : Icons.fingerprint_rounded,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Seguridad Biométrica (2FA)',
                            style: tt.titleSmall
                                ?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          Text(
                            status.description,
                            style: tt.bodySmall
                                ?.copyWith(color: cs.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                if (isReady) ...[
                  SwitchListTile.adaptive(
                    key: const ValueKey('dashboardBiometricSwitch'),
                    contentPadding: EdgeInsets.zero,
                    title: Text(
                      'Exigir ${status.shortLabel} al iniciar sesión',
                      style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      appState.hasSavedBiometricAccount &&
                              appState.savedBiometricEmail == currentEmail
                          ? 'Acceso rápido vinculado a $currentEmail'
                          : 'Resguarda tu acceso en Android Keystore',
                      style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                    ),
                    value: isActive,
                    onChanged: _busy ? null : _onToggle,
                  ),
                  if (appState.hasSavedBiometricAccount)
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton.icon(
                        onPressed: _busy
                            ? null
                            : () async {
                                await appState.unlinkBiometricAccount();
                                if (context.mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text(
                                        'Credenciales biométricas desvinculadas de este equipo.',
                                      ),
                                    ),
                                  );
                                }
                              },
                        icon: const Icon(Icons.link_off_rounded, size: 18),
                        label: const Text('Desvincular cuenta biométrica'),
                      ),
                    ),
                ] else
                  Align(
                    alignment: Alignment.centerLeft,
                    child: OutlinedButton.icon(
                      onPressed: _busy
                          ? null
                          : () async {
                              setState(() => _busy = true);
                              try {
                                await appState.refreshBiometricStatus();
                              } finally {
                                if (mounted) setState(() => _busy = false);
                              }
                            },
                      icon: const Icon(Icons.refresh_rounded, size: 18),
                      label: const Text('Reverificar sensores del teléfono'),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}
