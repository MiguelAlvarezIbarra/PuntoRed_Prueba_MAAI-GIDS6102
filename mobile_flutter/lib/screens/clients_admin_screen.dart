import 'package:flutter/material.dart';
import '../core/constants.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';

class ClientsAdminScreen extends StatefulWidget {
  final AppState appState;

  const ClientsAdminScreen({super.key, required this.appState});

  @override
  State<ClientsAdminScreen> createState() => _ClientsAdminScreenState();
}

class _ClientsAdminScreenState extends State<ClientsAdminScreen> {
  final _searchCtrl = TextEditingController();
  List<ClienteData> _clientes = [];
  bool _loading = true;
  String? _error;
  String? _notice;

  @override
  void initState() {
    super.initState();
    _buscar();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _buscar() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final q = _searchCtrl.text.trim();
      final list = await widget.appState.api.buscarClientes(
        q.isEmpty ? {} : {'nombre': q},
      );
      if (mounted) setState(() => _clientes = list);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleActivo(ClienteData c) async {
    setState(() {
      _error = null;
      _notice = null;
    });
    try {
      if (c.activo) {
        await widget.appState.api.darBajaCliente(c.rfc, motivo: 'Baja desde panel móvil');
        _notice = 'Cliente ${c.rfc} dado de baja.';
      } else {
        await widget.appState.api.reactivarCliente(c.rfc);
        _notice = 'Cliente ${c.rfc} reactivado.';
      }
      await _buscar();
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  Future<void> _abrirCrearAdmin() async {
    final formKey = GlobalKey<FormState>();
    final nombre = TextEditingController();
    final pat = TextEditingController();
    final mat = TextEditingController();
    final fecha = TextEditingController(text: '1995-05-15');
    final curp = TextEditingController();
    final rfc = TextEditingController();
    final correo = TextEditingController();
    final tel = TextEditingController();
    final ocup = TextEditingController(text: 'Especialista');
    final emp = TextEditingController(text: 'PuntoRed');
    final ing = TextEditingController(text: '18000');
    final pwd = TextEditingController();
    final calle = TextEditingController();
    final ext = TextEditingController();
    final col = TextEditingController();
    final mun = TextEditingController();
    final est = TextEditingController();
    final cp = TextEditingController();
    int rol = AppConstants.rolUsuario;
    String? dlgError;

    final created = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => AlertDialog(
          title: const Text('Nuevo Usuario (Admin)'),
          content: SizedBox(
            width: 420,
            child: SingleChildScrollView(
              child: Form(
                key: formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<int>(
                      initialValue: rol,
                      decoration: const InputDecoration(
                        labelText: 'Rol',
                        border: OutlineInputBorder(),
                      ),
                      items: const [
                        DropdownMenuItem(value: 2, child: Text('Usuario Normal (2)')),
                        DropdownMenuItem(value: 1, child: Text('Administrador (1)')),
                      ],
                      onChanged: (v) => setModalState(() => rol = v ?? 2),
                    ),
                    const SizedBox(height: 8),
                    _dlgField(nombre, 'Nombre *'),
                    _dlgField(pat, 'Apellido paterno *'),
                    _dlgField(mat, 'Apellido materno *'),
                    _dlgField(fecha, 'Fecha nacimiento (AAAA-MM-DD) *'),
                    _dlgField(curp, 'CURP (18) *'),
                    _dlgField(rfc, 'RFC (12-13) *'),
                    _dlgField(correo, 'Correo *'),
                    _dlgField(tel, 'Teléfono móvil (10) *'),
                    _dlgField(ocup, 'Ocupación *'),
                    _dlgField(emp, 'Empresa *'),
                    _dlgField(ing, 'Ingreso mensual *'),
                    _dlgField(pwd, 'Contraseña (mín 8) *', obscure: true),
                    _dlgField(calle, 'Calle *'),
                    _dlgField(ext, 'Núm. exterior *'),
                    _dlgField(col, 'Colonia *'),
                    _dlgField(mun, 'Municipio *'),
                    _dlgField(est, 'Estado *'),
                    _dlgField(cp, 'Código Postal (5) *'),
                    if (dlgError != null)
                      Text(dlgError!, style: TextStyle(color: Theme.of(ctx).colorScheme.error)),
                  ],
                ),
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cerrar')),
            FilledButton(
              onPressed: () async {
                if (!(formKey.currentState?.validate() ?? false)) return;
                try {
                  await widget.appState.api.registrarClienteAdmin({
                    'rol': rol,
                    'nombre': nombre.text.trim(),
                    'apellidoPaterno': pat.text.trim(),
                    'apellidoMaterno': mat.text.trim(),
                    'fechaNacimiento': fecha.text.trim(),
                    'curp': curp.text.trim().toUpperCase(),
                    'rfc': rfc.text.trim().toUpperCase(),
                    'sexo': 'H',
                    'nacionalidad': 'Mexicano',
                    'estadoCivil': 'Soltero',
                    'correo': correo.text.trim(),
                    'telefonoMovil': tel.text.trim(),
                    'ocupacion': ocup.text.trim(),
                    'empresa': emp.text.trim(),
                    'ingresoMensual': double.tryParse(ing.text.trim()) ?? 10000,
                    'password': pwd.text,
                    'domicilio': {
                      'calle': calle.text.trim(),
                      'numeroExterior': ext.text.trim(),
                      'colonia': col.text.trim(),
                      'municipio': mun.text.trim(),
                      'estado': est.text.trim(),
                      'codigoPostal': cp.text.trim(),
                      'pais': 'México',
                    },
                  });
                  if (ctx.mounted) Navigator.of(ctx).pop(true);
                } catch (e) {
                  setModalState(() => dlgError = e.toString());
                }
              },
              child: const Text('Crear Usuario'),
            ),
          ],
        ),
      ),
    );

    for (final c in [
      nombre, pat, mat, fecha, curp, rfc, correo, tel, ocup, emp, ing, pwd, calle, ext, col, mun,
      est, cp,
    ]) {
      c.dispose();
    }

    if (created == true && mounted) {
      setState(() => _notice = 'Cliente creado exitosamente.');
      await _buscar();
    }
  }

  Widget _dlgField(TextEditingController c, String label, {bool obscure = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: TextFormField(
        controller: c,
        obscureText: obscure,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          isDense: true,
        ),
        validator: (v) => (v == null || v.trim().isEmpty) ? 'Requerido' : null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Gestión de Clientes',
                      style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                  if (widget.appState.isAdmin)
                    FilledButton.icon(
                      onPressed: _abrirCrearAdmin,
                      icon: const Icon(Icons.person_add_alt_1_rounded),
                      label: const Text('Nuevo'),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _searchCtrl,
                      decoration: const InputDecoration(
                        hintText: 'Filtrar por nombre...',
                        prefixIcon: Icon(Icons.search_rounded),
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      onSubmitted: (_) => _buscar(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filledTonal(
                    onPressed: _loading ? null : _buscar,
                    icon: const Icon(Icons.refresh_rounded),
                  ),
                ],
              ),
            ],
          ),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Card(
              color: cs.errorContainer,
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Text(_error!, style: TextStyle(color: cs.onErrorContainer)),
              ),
            ),
          ),
        if (_notice != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Card(
              color: const Color(0xFFD1FAE5),
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Text(_notice!, style: const TextStyle(color: Color(0xFF065F46))),
              ),
            ),
          ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _clientes.isEmpty
                  ? const Center(child: Text('No hay clientes que coincidan con el filtro.'))
                  : ListView.builder(
                      padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                      itemCount: _clientes.length,
                      itemBuilder: (ctx, idx) {
                        final c = _clientes[idx];
                        return Card(
                          margin: const EdgeInsets.only(bottom: 10),
                          child: ListTile(
                            leading: CircleAvatar(
                              backgroundColor: c.activo ? cs.primaryContainer : cs.errorContainer,
                              child: Icon(
                                c.activo ? Icons.person_rounded : Icons.person_off_rounded,
                                color: c.activo ? cs.onPrimaryContainer : cs.onErrorContainer,
                              ),
                            ),
                            title: Text(c.nombreCompleto,
                                style: const TextStyle(fontWeight: FontWeight.bold)),
                            subtitle: Text(
                              'RFC: ${c.rfc} • ${c.correo}\nCuenta: ${AppFormatters.formatAccountNumber(c.numeroCuenta)}',
                            ),
                            isThreeLine: true,
                            trailing: widget.appState.isAdmin
                                ? TextButton(
                                    onPressed: () => _toggleActivo(c),
                                    child: Text(c.activo ? 'Baja' : 'Reactivar'),
                                  )
                                : Chip(label: Text(c.activo ? 'Activo' : 'Inactivo')),
                          ),
                        );
                      },
                    ),
        ),
      ],
    );
  }
}
