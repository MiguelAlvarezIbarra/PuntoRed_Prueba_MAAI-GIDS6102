import 'package:flutter/material.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';
import '../widgets/common_widgets.dart';

class CatalogScreen extends StatefulWidget {
  final AppState appState;
  final void Function(CatalogoServicioItem servicio) onSelectServicio;

  const CatalogScreen({
    super.key,
    required this.appState,
    required this.onSelectServicio,
  });

  @override
  State<CatalogScreen> createState() => _CatalogScreenState();
}

class _CatalogScreenState extends State<CatalogScreen> {
  static const _modulos = [
    ('TODOS', 'Todos'),
    ('RECARGA', 'Recargas'),
    ('PAQUETE', 'Paquetes'),
    ('RECAUDO', 'Recaudos'),
    ('PIN', 'Pines'),
  ];

  List<CatalogoServicioItem> _servicios = [];
  String _fuente = 'PUNTORED_H2H_V2';
  String? _saldoComercio;
  String _moduloSeleccionado = 'TODOS';
  String _busqueda = '';
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar(forzar: false);
  }

  Future<void> _cargar({bool forzar = false}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await widget.appState.api.buscarCatalogo(forzarRecarga: forzar);
      if (!mounted) return;
      setState(() {
        _servicios = res.servicios;
        _fuente = res.fuenteDatos;
        _saldoComercio = res.saldoComercio;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<CatalogoServicioItem> get _filtrados {
    final q = _busqueda.trim().toLowerCase();
    return _servicios.where((s) {
      if (_moduloSeleccionado != 'TODOS') {
        final esRec = _moduloSeleccionado == 'RECAUDO' &&
            (s.tipoOperacion == 'RECAUDO' || s.tipoOperacion == 'RECAUDO_V2');
        if (!esRec && s.tipoOperacion != _moduloSeleccionado) return false;
      }
      if (q.isEmpty) return true;
      return s.nombreServicio.toLowerCase().contains(q) ||
          s.nombreProducto.toLowerCase().contains(q) ||
          s.categoria.toLowerCase().contains(q) ||
          (s.descripcion ?? '').toLowerCase().contains(q) ||
          (s.codigoProducto ?? '').contains(q) ||
          (s.idConvenio ?? '').contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filtrados = _filtrados;
    final cs = Theme.of(context).colorScheme;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Catálogo Dinámico PuntoRed',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        Text(
                          '${filtrados.length} servicios · $_fuente${_saldoComercio != null ? ' · Comercio ${AppFormatters.formatCurrency(double.tryParse(_saldoComercio!))}' : ''}',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  IconButton.filledTonal(
                    onPressed: _loading ? null : () => _cargar(forzar: true),
                    icon: const Icon(Icons.sync_rounded),
                    tooltip: 'Actualizar desde PuntoRed',
                  ),
                ],
              ),
              const SizedBox(height: 10),
              TextField(
                decoration: const InputDecoration(
                  hintText: 'Buscar servicio, operador, convenio o código...',
                  prefixIcon: Icon(Icons.search_rounded),
                  isDense: true,
                  border: OutlineInputBorder(),
                ),
                onChanged: (v) => setState(() => _busqueda = v),
              ),
              const SizedBox(height: 10),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: _modulos.map((m) {
                    final selected = _moduloSeleccionado == m.$1;
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: Text(m.$2),
                        selected: selected,
                        onSelected: (_) =>
                            setState(() => _moduloSeleccionado = m.$1),
                      ),
                    );
                  }).toList(),
                ),
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
                padding: const EdgeInsets.all(12),
                child: Text(_error!, style: TextStyle(color: cs.onErrorContainer)),
              ),
            ),
          ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : filtrados.isEmpty
                  ? const Center(
                      child: Text('No se encontraron servicios con este filtro.'),
                    )
                  : RefreshIndicator(
                      onRefresh: () => _cargar(forzar: true),
                      child: ListView.builder(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                        itemCount: filtrados.length > 150 ? 150 : filtrados.length,
                        itemBuilder: (ctx, idx) {
                          final item = filtrados[idx];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  ServiceLogoWidget(
                                    logoUrl: item.logoUrl,
                                    name: item.nombreServicio,
                                    tipoOperacion: item.tipoOperacion,
                                    size: 50,
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            Expanded(
                                              child: Text(
                                                item.nombreProducto,
                                                style: const TextStyle(
                                                  fontWeight: FontWeight.bold,
                                                  fontSize: 15,
                                                ),
                                              ),
                                            ),
                                            if (item.precioSugerido != null &&
                                                item.precioSugerido! > 0)
                                              Text(
                                                AppFormatters.formatCurrency(
                                                    item.precioSugerido),
                                                style: TextStyle(
                                                  color: cs.primary,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                          ],
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          '${item.nombreServicio} · ${item.categoria}',
                                          style: Theme.of(context).textTheme.bodySmall,
                                        ),
                                        if (item.descripcion != null &&
                                            item.descripcion!.isNotEmpty) ...[
                                          const SizedBox(height: 4),
                                          Text(
                                            item.descripcion!,
                                            maxLines: 2,
                                            overflow: TextOverflow.ellipsis,
                                            style: Theme.of(context).textTheme.bodySmall,
                                          ),
                                        ],
                                        const SizedBox(height: 8),
                                        Row(
                                          children: [
                                            Chip(
                                              visualDensity: VisualDensity.compact,
                                              label: Text(
                                                item.tipoOperacion,
                                                style: const TextStyle(fontSize: 11),
                                              ),
                                            ),
                                            if (item.codigoProducto != null) ...[
                                              const SizedBox(width: 6),
                                              Text(
                                                'Cód: ${item.codigoProducto}',
                                                style: Theme.of(context)
                                                    .textTheme
                                                    .labelSmall,
                                              ),
                                            ],
                                            const Spacer(),
                                            FilledButton.tonalIcon(
                                              onPressed: () =>
                                                  widget.onSelectServicio(item),
                                              icon: const Icon(
                                                Icons.verified_user_outlined,
                                                size: 16,
                                              ),
                                              label: const Text('Pagar'),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
        ),
      ],
    );
  }
}
