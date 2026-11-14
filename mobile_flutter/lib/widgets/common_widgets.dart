import 'package:flutter/material.dart';
import '../core/constants.dart';
import '../models/api_models.dart';
import '../state/app_state.dart';

class ServiceLogoWidget extends StatelessWidget {
  final String? logoUrl;
  final String name;
  final String? tipoOperacion;
  final double size;

  const ServiceLogoWidget({
    super.key,
    required this.logoUrl,
    required this.name,
    this.tipoOperacion,
    this.size = 46,
  });

  IconData _fallbackIcon() {
    switch ((tipoOperacion ?? '').toUpperCase()) {
      case 'RECARGA':
        return Icons.phone_android_rounded;
      case 'PAQUETE':
        return Icons.wifi_rounded;
      case 'RECAUDO':
      case 'RECAUDO_V2':
        return Icons.receipt_long_rounded;
      case 'PIN':
        return Icons.sports_esports_rounded;
      default:
        return Icons.account_balance_rounded;
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final cleanUrl = logoUrl?.trim();

    if (cleanUrl != null && cleanUrl.startsWith('http')) {
      return Container(
        width: size,
        height: size,
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: cs.outlineVariant),
        ),
        child: Image.network(
          cleanUrl,
          fit: BoxFit.contain,
          errorBuilder: (_, _, _) => Icon(_fallbackIcon(), color: cs.primary),
        ),
      );
    }

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: cs.primaryContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(_fallbackIcon(), color: cs.onPrimaryContainer),
    );
  }
}

class StatusBadge extends StatelessWidget {
  final String estado;

  const StatusBadge({super.key, required this.estado});

  @override
  Widget build(BuildContext context) {
    final norm = estado.toUpperCase();
    Color bg;
    Color fg;
    IconData icon;
    String label;

    if (norm == 'EXITOSA') {
      bg = const Color(0xFFD1FAE5);
      fg = const Color(0xFF065F46);
      icon = Icons.check_circle_rounded;
      label = 'Exitosa';
    } else if (norm == 'RECHAZADA') {
      bg = const Color(0xFFFEE2E2);
      fg = const Color(0xFF991B1B);
      icon = Icons.cancel_rounded;
      label = 'Rechazada';
    } else {
      bg = const Color(0xFFFEF3C7);
      fg = const Color(0xFF92400E);
      icon = Icons.hourglass_top_rounded;
      label = norm == 'NO_CONFIRMADA' ? 'No Confirmada' : 'Pendiente';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(color: fg, fontSize: 12, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class TicketCard extends StatelessWidget {
  final TicketData ticket;

  const TicketCard({super.key, required this.ticket});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final entries = ticket.cuerpo?.entries.toList() ?? [];

    return Card(
      elevation: 0,
      color: cs.surfaceContainerHighest.withValues(alpha: 0.45),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: cs.outlineVariant),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (ticket.encabezado != null && ticket.encabezado!.isNotEmpty)
              Text(
                ticket.encabezado!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            const SizedBox(height: 4),
            Text(
              ticket.titulo ?? 'COMPROBANTE DIGITAL PUNTORED',
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .titleSmall
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            const Divider(height: 16),
            for (final entry in entries)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '${entry.key}:',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(width: 12),
                    Flexible(
                      child: Text(
                        '${entry.value ?? ''}',
                        textAlign: TextAlign.end,
                        style: Theme.of(context)
                            .textTheme
                            .bodySmall
                            ?.copyWith(fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
            if (ticket.piePagina != null && ticket.piePagina!.isNotEmpty) ...[
              const Divider(height: 16),
              Text(
                ticket.piePagina!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

Future<void> showEnvironmentDialog(BuildContext context, AppState state) async {
  final ctrl = TextEditingController(text: state.api.baseUrl);
  await showDialog<void>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Configurar Servidor API'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ActionChip(
                label: const Text('Render Cloud'),
                onPressed: () => ctrl.text = AppConstants.renderApiUrl,
              ),
              ActionChip(
                label: const Text('Emulador Android'),
                onPressed: () => ctrl.text = AppConstants.emulatorApiUrl,
              ),
              ActionChip(
                label: const Text('Localhost'),
                onPressed: () => ctrl.text = AppConstants.localApiUrl,
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: ctrl,
            decoration: const InputDecoration(
              labelText: 'URL Base del Backend',
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () async {
            await state.updateBaseUrl(ctrl.text);
            if (ctx.mounted) Navigator.of(ctx).pop();
          },
          child: const Text('Guardar'),
        ),
      ],
    ),
  );
}
