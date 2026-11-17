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
  final _rfcCtrl = TextEditingController();
  final _curpCtrl = TextEditingController();
  final _correoFilterCtrl = TextEditingController();
  bool _showFilters = false;

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
    _rfcCtrl.dispose();
    _curpCtrl.dispose();
    _correoFilterCtrl.dispose();
    super.dispose();
  }

  Future<void> _buscar() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final filtros = <String, dynamic>{};
      if (_searchCtrl.text.trim().isNotEmpty) {
        filtros['nombre'] = _searchCtrl.text.trim();
      }
      if (_rfcCtrl.text.trim().isNotEmpty) {
        filtros['rfc'] = _rfcCtrl.text.trim().toUpperCase();
      }
      if (_curpCtrl.text.trim().isNotEmpty) {
        filtros['curp'] = _curpCtrl.text.trim().toUpperCase();
      }
      if (_correoFilterCtrl.text.trim().isNotEmpty) {
        filtros['correo'] = _correoFilterCtrl.text.trim();
      }
      final list = await widget.appState.api.buscarClientes(filtros);
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

  Future<void> _abrirFormularioCliente({ClienteData? existing}) async {
    final isEdit = existing != null;
    final dom = existing?.domicilio ?? <String, dynamic>{};

    final formKey = GlobalKey<FormState>();
    final nombre = TextEditingController(text: existing?.nombre ?? '');
    final pat = TextEditingController(text: existing?.apellidoPaterno ?? '');
    final mat = TextEditingController(text: existing?.apellidoMaterno ?? '');
    final fecha = TextEditingController(text: '1995-05-15');
    final curp = TextEditingController(text: existing?.curp ?? '');
    final rfc = TextEditingController(text: existing?.rfc ?? '');
    final correo = TextEditingController(text: existing?.correo ?? '');
    final tel = TextEditingController(text: existing?.telefono ?? '');
    final ocup = TextEditingController(text: existing?.ocupacion ?? 'Especialista');
    final emp = TextEditingController(text: existing?.empresa ?? 'PuntoRed');
    final ing = TextEditingController(
      text: existing != null ? existing.ingresoMensual.toStringAsFixed(0) : '18000',
    );
    final pwd = TextEditingController();
    final calle = TextEditingController(text: dom['calle']?.toString() ?? '');
    final ext = TextEditingController(text: dom['numeroExterior']?.toString() ?? '');
    final col = TextEditingController(text: dom['colonia']?.toString() ?? '');
    final mun = TextEditingController(text: dom['municipio']?.toString() ?? '');
    final est = TextEditingController(text: dom['estado']?.toString() ?? '');
    final cp = TextEditingController(text: dom['codigoPostal']?.toString() ?? '');

    String sexo = existing?.sexo == 'M' ? 'M' : 'H';
    String nacionalidad = AppConstants.nacionalidades.contains(existing?.nacionalidad)
        ? existing!.nacionalidad
        : 'Mexicano';
    String estadoCivil = AppConstants.estadosCiviles.contains(existing?.estadoCivil)
        ? existing!.estadoCivil
        : 'Soltero';
    String pais = AppConstants.paises.contains(dom['pais']?.toString())
        ? dom['pais'].toString()
        : 'México';
    int rol = AppConstants.rolUsuario;
    String? dlgError;

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => AlertDialog(
          insetPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 20),
          title: Text(isEdit ? 'Editar Cliente (${existing.rfc})' : 'Nuevo Usuario (Admin)'),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Form(
                key: formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (!isEdit) ...[
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
                    ],
                    _dlgField(nombre, 'Nombre *'),
                    _dlgField(pat, 'Apellido paterno *'),
                    _dlgField(mat, 'Apellido materno *'),
                    if (!isEdit) ...[
                      _dlgField(fecha, 'Fecha nacimiento (AAAA-MM-DD) *'),
                      _dlgField(curp, 'CURP (18) *'),
                      _dlgField(rfc, 'RFC (12-13) *'),
                    ],
                    DropdownButtonFormField<String>(
                      initialValue: sexo,
                      decoration: const InputDecoration(
                        labelText: 'Sexo',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      items: const [
                        DropdownMenuItem(value: 'H', child: Text('Hombre (H)')),
                        DropdownMenuItem(value: 'M', child: Text('Mujer (M)')),
                      ],
                      onChanged: (v) => setModalState(() => sexo = v ?? 'H'),
                    ),
                    const SizedBox(height: 8),
                    DropdownButtonFormField<String>(
                      initialValue: nacionalidad,
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: 'Nacionalidad',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      items: AppConstants.nacionalidades
                          .map((n) => DropdownMenuItem(value: n, child: Text(n)))
                          .toList(),
                      onChanged: (v) => setModalState(() => nacionalidad = v ?? 'Mexicano'),
                    ),
                    const SizedBox(height: 8),
                    DropdownButtonFormField<String>(
                      initialValue: estadoCivil,
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: 'Estado civil',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      items: AppConstants.estadosCiviles
                          .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                          .toList(),
                      onChanged: (v) => setModalState(() => estadoCivil = v ?? 'Soltero'),
                    ),
                    const SizedBox(height: 8),
                    _dlgField(correo, 'Correo *'),
                    _dlgField(tel, 'Teléfono móvil (10) *'),
                    _dlgField(ocup, 'Ocupación *'),
                    _dlgField(emp, 'Empresa *'),
                    _dlgField(ing, 'Ingreso mensual *'),
                    if (!isEdit) _dlgField(pwd, 'Contraseña (mín 8) *', obscure: true),
                    _dlgField(calle, 'Calle *'),
                    _dlgField(ext, 'Núm. exterior *'),
                    _dlgField(col, 'Colonia *'),
                    _dlgField(mun, 'Municipio *'),
                    _dlgField(est, 'Estado *'),
                    _dlgField(cp, 'Código Postal (5) *'),
                    DropdownButtonFormField<String>(
                      initialValue: pais,
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: 'País',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                      items: AppConstants.paises
                          .map((p) => DropdownMenuItem(value: p, child: Text(p)))
                          .toList(),
                      onChanged: (v) => setModalState(() => pais = v ?? 'México'),
                    ),
                    if (dlgError != null) ...[
                      const SizedBox(height: 8),
                      Text(dlgError!, style: TextStyle(color: Theme.of(ctx).colorScheme.error)),
                    ],
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
                  final domicilioPayload = <String, dynamic>{
                    'calle': calle.text.trim(),
                    'numeroExterior': ext.text.trim(),
                    'colonia': col.text.trim(),
                    'municipio': mun.text.trim(),
                    'estado': est.text.trim(),
                    'codigoPostal': cp.text.trim(),
                    'pais': pais,
                  };
                  if (isEdit) {
                    await widget.appState.api.actualizarCliente({
                      'rfc': existing.rfc,
                      'nombre': nombre.text.trim(),
                      if (existing.segundoNombre != null) 'segundoNombre': existing.segundoNombre,
                      'apellidoPaterno': pat.text.trim(),
                      'apellidoMaterno': mat.text.trim(),
                      'sexo': sexo,
                      'nacionalidad': nacionalidad,
                      'estadoCivil': estadoCivil,
                      'correo': correo.text.trim(),
                      'telefonoMovil': tel.text.trim(),
                      if (existing.telefonoAlternativo != null)
                        'telefonoAlternativo': existing.telefonoAlternativo,
                      'ocupacion': ocup.text.trim(),
                      'empresa': emp.text.trim(),
                      'ingresoMensual': double.tryParse(ing.text.trim()) ?? 10000,
                      'domicilio': domicilioPayload,
                    });
                  } else {
                    await widget.appState.api.registrarClienteAdmin({
                      'rol': rol,
                      'nombre': nombre.text.trim(),
                      'apellidoPaterno': pat.text.trim(),
                      'apellidoMaterno': mat.text.trim(),
                      'fechaNacimiento': fecha.text.trim(),
                      'curp': curp.text.trim().toUpperCase(),
                      'rfc': rfc.text.trim().toUpperCase(),
                      'sexo': sexo,
                      'nacionalidad': nacionalidad,
                      'estadoCivil': estadoCivil,
                      'correo': correo.text.trim(),
                      'telefonoMovil': tel.text.trim(),
                      'ocupacion': ocup.text.trim(),
                      'empresa': emp.text.trim(),
                      'ingresoMensual': double.tryParse(ing.text.trim()) ?? 10000,
                      'password': pwd.text,
                      'domicilio': domicilioPayload,
                    });
                  }
                  if (ctx.mounted) Navigator.of(ctx).pop(true);
                } catch (e) {
                  setModalState(() => dlgError = e.toString());
                }
              },
              child: Text(isEdit ? 'Guardar Cambios' : 'Crear Usuario'),
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

    if (saved == true && mounted) {
      setState(() => _notice = isEdit ? 'Cliente actualizado correctamente.' : 'Cliente creado exitosamente.');
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
                  Expanded(
                    child: Text(
                      'Gestión de Clientes',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ),
                  if (widget.appState.isAdmin)
                    FilledButton.icon(
                      onPressed: () => _abrirFormularioCliente(),
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
                  const SizedBox(width: 6),
                  IconButton.outlined(
                    tooltip: 'Más filtros (RFC, CURP, Correo)',
                    onPressed: () => setState(() => _showFilters = !_showFilters),
                    icon: Icon(_showFilters ? Icons.filter_alt_off_rounded : Icons.tune_rounded),
                  ),
                  const SizedBox(width: 4),
                  IconButton.filledTonal(
                    onPressed: _loading ? null : _buscar,
                    icon: const Icon(Icons.refresh_rounded),
                  ),
                ],
              ),
              if (_showFilters) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _rfcCtrl,
                        textCapitalization: TextCapitalization.characters,
                        decoration: const InputDecoration(
                          labelText: 'RFC',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _buscar(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _curpCtrl,
                        textCapitalization: TextCapitalization.characters,
                        decoration: const InputDecoration(
                          labelText: 'CURP',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _buscar(),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _correoFilterCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Correo',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        onSubmitted: (_) => _buscar(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    TextButton(
                      onPressed: () {
                        _searchCtrl.clear();
                        _rfcCtrl.clear();
                        _curpCtrl.clear();
                        _correoFilterCtrl.clear();
                        _buscar();
                      },
                      child: const Text('Limpiar'),
                    ),
                  ],
                ),
              ],
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
                          child: Padding(
                            padding: const EdgeInsets.all(12),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor:
                                          c.activo ? cs.primaryContainer : cs.errorContainer,
                                      child: Icon(
                                        c.activo ? Icons.person_rounded : Icons.person_off_rounded,
                                        color: c.activo
                                            ? cs.onPrimaryContainer
                                            : cs.onErrorContainer,
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            c.nombreCompleto,
                                            style: const TextStyle(fontWeight: FontWeight.bold),
                                          ),
                                          Text(
                                            'RFC: ${c.rfc} • ${c.correo}',
                                            style: tt.bodySmall,
                                          ),
                                          if (c.numeroCuenta != null)
                                            Text(
                                              'Cuenta: ${AppFormatters.formatAccountNumber(c.numeroCuenta)}',
                                              style: tt.bodySmall?.copyWith(fontFamily: 'monospace'),
                                            ),
                                        ],
                                      ),
                                    ),
                                    Chip(
                                      label: Text(c.activo ? 'Activo' : 'Inactivo'),
                                      visualDensity: VisualDensity.compact,
                                    ),
                                  ],
                                ),
                                if (widget.appState.isAdmin) ...[
                                  const SizedBox(height: 8),
                                  Wrap(
                                    spacing: 8,
                                    children: [
                                      OutlinedButton.icon(
                                        onPressed: () => _abrirFormularioCliente(existing: c),
                                        icon: const Icon(Icons.edit_outlined, size: 16),
                                        label: const Text('Editar'),
                                      ),
                                      TextButton(
                                        onPressed: () => _toggleActivo(c),
                                        child: Text(c.activo ? 'Dar de baja' : 'Reactivar'),
                                      ),
                                    ],
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      },
                    ),
        ),
      ],
    );
  }
}
