import 'package:flutter/material.dart';
import '../core/formatters.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';
import '../widgets/common_widgets.dart';

class HistoryScreen extends StatefulWidget {
  final AppState appState;

  const HistoryScreen({super.key, required this.appState});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  static const _estados = [
    ('', 'Todos'),
    ('EXITOSA', 'Exitosos'),
    ('RECHAZADA', 'Rechazados'),
    ('PENDIENTE', 'Pendientes'),
    ('NO_CONFIRMADA', 'No confirmados'),
  ];

  List<HistorialPagoItem> _items = [];
  String _estadoFiltro = '';
  String _busqueda = '';
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await widget.appState.api.consultarHistorialPagos(
        estado: _estadoFiltro.isEmpty ? null : _estadoFiltro,
      );
      if (mounted) {
        setState(() => _items = list);
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _abrirDetalle(HistorialPagoItem item) async {
    HistorialPagoItem current = item;
    bool checkingLive = false;
    String? liveMsg;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) {
          final isPending =
              current.estado == 'PENDIENTE' || current.estado == 'NO_CONFIRMADA';
          return Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      ServiceLogoWidget(
                        logoUrl: current.logoUrl,
                        name: current.nombreServicio,
                        tipoOperacion: current.tipoOperacion,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              current.nombreServicio,
                              style: Theme.of(ctx)
                                  .textTheme
                                  .titleMedium
                                  ?.copyWith(fontWeight: FontWeight.bold),
                            ),
                            Text(
                              current.nombreProducto ?? current.tipoOperacion,
                              style: Theme.of(ctx).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      ),
                      StatusBadge(estado: current.estado),
                    ],
                  ),
                  const Divider(height: 20),
                  _detailRow('Fecha', AppFormatters.formatDateTime(current.fechaRegistro)),
                  _detailRow('Referencia', current.referencia1),
                  _detailRow('Importe', AppFormatters.formatCurrency(current.monto)),
                  _detailRow('Cuenta cargo', AppFormatters.formatAccountNumber(current.numeroCuenta)),
                  _detailRow('Proveedor', current.proveedor),
                  if (current.codigoAutorizacion != null)
                    _detailRow('Folio / Autorización', current.codigoAutorizacion!),
                  if (current.idTransaccionProveedor != null)
                    _detailRow('ID Transacción Proveedor', current.idTransaccionProveedor!),
                  if (current.trace != null) _detailRow('Trace', current.trace!),
                  _detailRow('Código / Respuesta',
                      '${current.codigoRespuesta} • ${current.mensajeRespuesta}'),
                  if (liveMsg != null) ...[
                    const SizedBox(height: 8),
                    Text(
                      liveMsg!,
                      style: TextStyle(
                        color: Theme.of(ctx).colorScheme.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                  if (isPending) ...[
                    const SizedBox(height: 12),
                    FilledButton.tonalIcon(
                      onPressed: checkingLive
                          ? null
                          : () async {
                              setSheetState(() => checkingLive = true);
                              try {
                                final det = await widget.appState.api.consultarDetallePago(
                                  current.id,
                                  consultarEstadoProveedor: true,
                                );
                                setSheetState(() {
                                  current = det.transaccion;
                                  liveMsg = det.mensaje;
                                });
                                _cargar();
                              } catch (e) {
                                setSheetState(() => liveMsg = e.toString());
                              } finally {
                                setSheetState(() => checkingLive = false);
                              }
                            },
                      icon: const Icon(Icons.sync_rounded),
                      label: const Text('Consultar Estado Actual en Puntored'),
                    ),
                  ],
                  if (current.ticket != null) ...[
                    const SizedBox(height: 12),
                    TicketCard(ticket: current.ticket!),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _detailRow(String k, String v) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(k, style: const TextStyle(color: Colors.grey, fontSize: 13)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              v,
              textAlign: TextAlign.end,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final filtered = _items.where((i) {
      if (_busqueda.trim().isEmpty) return true;
      final q = _busqueda.trim().toLowerCase();
      return i.nombreServicio.toLowerCase().contains(q) ||
          (i.nombreProducto ?? '').toLowerCase().contains(q) ||
          i.referencia1.toLowerCase().contains(q) ||
          (i.codigoAutorizacion ?? '').toLowerCase().contains(q);
    }).toList();

    return RefreshIndicator(
      onRefresh: _cargar,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Historial de Pagos',
                        style: tt.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                    IconButton(
                      tooltip: 'Actualizar historial',
                      onPressed: _loading ? null : _cargar,
                      icon: const Icon(Icons.refresh_rounded),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                TextField(
                  decoration: const InputDecoration(
                    hintText: 'Buscar por servicio, referencia o folio...',
                    prefixIcon: Icon(Icons.search_rounded),
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  onChanged: (v) => setState(() => _busqueda = v),
                ),
                const SizedBox(height: 10),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: [
                      for (final est in _estados)
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: ChoiceChip(
                            label: Text(est.$2),
                            selected: _estadoFiltro == est.$1,
                            onSelected: (_) {
                              setState(() => _estadoFiltro = est.$1);
                              _cargar();
                            },
                          ),
                        ),
                    ],
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
                : filtered.isEmpty
                    ? ListView(
                        children: const [
                          SizedBox(height: 60),
                          Center(
                            child: Text('No se encontraron operaciones registradas para tu usuario.'),
                          ),
                        ],
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                        itemCount: filtered.length,
                        itemBuilder: (ctx, idx) {
                          final item = filtered[idx];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(14),
                              onTap: () => _abrirDetalle(item),
                              child: Padding(
                                padding: const EdgeInsets.all(14),
                                child: Row(
                                  children: [
                                    ServiceLogoWidget(
                                      logoUrl: item.logoUrl,
                                      name: item.nombreServicio,
                                      tipoOperacion: item.tipoOperacion,
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            item.nombreServicio,
                                            style: const TextStyle(fontWeight: FontWeight.bold),
                                          ),
                                          Text(
                                            'Ref: ${item.referencia1} • ${AppFormatters.formatDateTime(item.fechaRegistro)}',
                                            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                          ),
                                          if (item.codigoAutorizacion != null)
                                            Text(
                                              'Folio: ${item.codigoAutorizacion}',
                                              style: tt.bodySmall,
                                            ),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Column(
                                      crossAxisAlignment: CrossAxisAlignment.end,
                                      children: [
                                        Text(
                                          AppFormatters.formatCurrency(item.monto),
                                          style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                                        ),
                                        const SizedBox(height: 4),
                                        StatusBadge(estado: item.estado),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
