import 'package:flutter/material.dart';
import 'models/api_models.dart';
import 'screens/accounts_screen.dart';
import 'screens/auth_screen.dart';
import 'screens/catalog_screen.dart';
import 'screens/clients_admin_screen.dart';
import 'screens/dashboard_screen.dart';
import 'screens/history_screen.dart';
import 'screens/payment_screen.dart';
import 'state/app_state.dart';
import 'widgets/common_widgets.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final state = AppState();
  state.init();
  runApp(PuntoRedApp(appState: state));
}

class PuntoRedApp extends StatefulWidget {
  final AppState appState;

  const PuntoRedApp({super.key, required this.appState});

  @override
  State<PuntoRedApp> createState() => _PuntoRedAppState();
}

class _PuntoRedAppState extends State<PuntoRedApp> {
  @override
  void initState() {
    super.initState();
    widget.appState.addListener(_onStateChanged);
  }

  @override
  void dispose() {
    widget.appState.removeListener(_onStateChanged);
    super.dispose();
  }

  void _onStateChanged() {
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF0F4C81);
    final lightScheme = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: Brightness.light,
    );
    final darkScheme = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: Brightness.dark,
    );

    return MaterialApp(
      title: 'PuntoRed Pagos H2H',
      debugShowCheckedModeBanner: false,
      themeMode: widget.appState.themeMode,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: lightScheme,
        cardTheme: CardThemeData(
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: BorderSide(color: lightScheme.outlineVariant),
          ),
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: darkScheme,
        cardTheme: CardThemeData(
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: BorderSide(color: darkScheme.outlineVariant),
          ),
        ),
      ),
      home: _buildHome(),
    );
  }

  Widget _buildHome() {
    if (!widget.appState.isInitialized) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (widget.appState.session == null) {
      return AuthScreen(appState: widget.appState);
    }
    return MainShell(appState: widget.appState);
  }
}

class MainShell extends StatefulWidget {
  final AppState appState;

  const MainShell({super.key, required this.appState});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _tabIndex = 0;
  CatalogoServicioItem? _selectedService;
  String? _preselectedAccount;

  void _navigateTo(int tabIndex, {String? accountNumber}) {
    setState(() {
      _tabIndex = tabIndex;
      if (accountNumber != null && accountNumber.isNotEmpty) {
        _preselectedAccount = accountNumber;
      }
    });
  }

  Future<void> _confirmLogout() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar Sesión'),
        content: const Text('¿Deseas cerrar tu sesión segura en este dispositivo?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      await widget.appState.logout();
    }
  }

  Widget _buildCurrentTab() {
    switch (_tabIndex) {
      case 1:
        return CatalogScreen(
          appState: widget.appState,
          onSelectServicio: (srv) {
            setState(() {
              _selectedService = srv;
              _tabIndex = 2;
            });
          },
        );
      case 2:
        return PaymentScreen(
          appState: widget.appState,
          selectedService: _selectedService,
          preselectedAccount: _preselectedAccount,
          onPickFromCatalog: () => _navigateTo(1),
          onOpenHistory: () => _navigateTo(3),
        );
      case 3:
        return HistoryScreen(appState: widget.appState);
      case 4:
        return AccountsScreen(
          appState: widget.appState,
          onSelectForPayment: (account) => _navigateTo(2, accountNumber: account),
        );
      case 5:
        return ClientsAdminScreen(appState: widget.appState);
      case 0:
      default:
        return DashboardScreen(
          appState: widget.appState,
          onNavigate: _navigateTo,
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isWide = MediaQuery.sizeOf(context).width >= 760;
    final isAdmin = widget.appState.isAdmin;
    final currentNavIndex = _tabIndex > 4 ? 0 : _tabIndex;

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            const Icon(Icons.bolt_rounded),
            const SizedBox(width: 8),
            const Text('PuntoRed', style: TextStyle(fontWeight: FontWeight.bold)),
            if (isAdmin) ...[
              const SizedBox(width: 8),
              Chip(
                label: const Text('Admin', style: TextStyle(fontSize: 11)),
                visualDensity: VisualDensity.compact,
              ),
            ],
          ],
        ),
        actions: [
          if (isAdmin && !isWide)
            IconButton(
              tooltip: 'Gestión de Clientes',
              onPressed: () => _navigateTo(5),
              icon: Icon(
                Icons.manage_accounts_rounded,
                color: _tabIndex == 5 ? Theme.of(context).colorScheme.primary : null,
              ),
            ),
          IconButton(
            tooltip: 'Cambiar tema',
            onPressed: widget.appState.toggleTheme,
            icon: Icon(
              widget.appState.themeMode == ThemeMode.dark
                  ? Icons.light_mode_rounded
                  : Icons.dark_mode_rounded,
            ),
          ),
          IconButton(
            tooltip: 'Configurar Servidor API',
            onPressed: () => showEnvironmentDialog(context, widget.appState),
            icon: const Icon(Icons.dns_rounded),
          ),
          IconButton(
            tooltip: 'Cerrar sesión',
            onPressed: _confirmLogout,
            icon: const Icon(Icons.logout_rounded),
          ),
        ],
      ),
      body: isWide
          ? Row(
              children: [
                NavigationRail(
                  selectedIndex: _tabIndex,
                  onDestinationSelected: (idx) => _navigateTo(idx),
                  labelType: NavigationRailLabelType.all,
                  destinations: [
                    const NavigationRailDestination(
                      icon: Icon(Icons.dashboard_outlined),
                      selectedIcon: Icon(Icons.dashboard_rounded),
                      label: Text('Inicio'),
                    ),
                    const NavigationRailDestination(
                      icon: Icon(Icons.grid_view_outlined),
                      selectedIcon: Icon(Icons.grid_view_rounded),
                      label: Text('Catálogo'),
                    ),
                    const NavigationRailDestination(
                      icon: Icon(Icons.payments_outlined),
                      selectedIcon: Icon(Icons.payments_rounded),
                      label: Text('Pagar'),
                    ),
                    const NavigationRailDestination(
                      icon: Icon(Icons.receipt_long_outlined),
                      selectedIcon: Icon(Icons.receipt_long_rounded),
                      label: Text('Historial'),
                    ),
                    const NavigationRailDestination(
                      icon: Icon(Icons.account_balance_wallet_outlined),
                      selectedIcon: Icon(Icons.account_balance_wallet_rounded),
                      label: Text('Cuentas'),
                    ),
                    if (isAdmin)
                      const NavigationRailDestination(
                        icon: Icon(Icons.manage_accounts_outlined),
                        selectedIcon: Icon(Icons.manage_accounts_rounded),
                        label: Text('Clientes'),
                      ),
                  ],
                ),
                const VerticalDivider(width: 1),
                Expanded(child: _buildCurrentTab()),
              ],
            )
          : _buildCurrentTab(),
      bottomNavigationBar: isWide
          ? null
          : NavigationBar(
              selectedIndex: currentNavIndex,
              onDestinationSelected: (idx) => _navigateTo(idx),
              destinations: const [
                NavigationDestination(
                  icon: Icon(Icons.dashboard_outlined),
                  selectedIcon: Icon(Icons.dashboard_rounded),
                  label: 'Inicio',
                ),
                NavigationDestination(
                  icon: Icon(Icons.grid_view_outlined),
                  selectedIcon: Icon(Icons.grid_view_rounded),
                  label: 'Catálogo',
                ),
                NavigationDestination(
                  icon: Icon(Icons.payments_outlined),
                  selectedIcon: Icon(Icons.payments_rounded),
                  label: 'Pagar',
                ),
                NavigationDestination(
                  icon: Icon(Icons.receipt_long_outlined),
                  selectedIcon: Icon(Icons.receipt_long_rounded),
                  label: 'Historial',
                ),
                NavigationDestination(
                  icon: Icon(Icons.account_balance_wallet_outlined),
                  selectedIcon: Icon(Icons.account_balance_wallet_rounded),
                  label: 'Cuentas',
                ),
              ],
            ),
    );
  }
}
