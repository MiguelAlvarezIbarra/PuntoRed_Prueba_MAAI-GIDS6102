import 'package:flutter/material.dart';
import '../core/constants.dart';
import '../core/formatters.dart';
import '../state/app_state.dart';
import '../widgets/common_widgets.dart';

class AuthScreen extends StatefulWidget {
  final AppState appState;

  const AuthScreen({super.key, required this.appState});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  bool _isLogin = true;
  bool _loading = false;
  bool _obscureLoginPwd = true;
  bool _obscureRegPwd = true;
  String? _errorMessage;
  String? _successMessage;

  // Login controllers
  final _loginFormKey = GlobalKey<FormState>();
  final _loginEmailCtrl = TextEditingController();
  final _loginPwdCtrl = TextEditingController();

  // Register controllers
  final _regFormKey = GlobalKey<FormState>();
  final _nombreCtrl = TextEditingController();
  final _segundoNombreCtrl = TextEditingController();
  final _paternoCtrl = TextEditingController();
  final _maternoCtrl = TextEditingController();
  final _fechaNacCtrl = TextEditingController();
  final _curpCtrl = TextEditingController();
  final _rfcCtrl = TextEditingController();
  final _regCorreoCtrl = TextEditingController();
  final _telMovilCtrl = TextEditingController();
  final _telAltCtrl = TextEditingController();
  final _ocupacionCtrl = TextEditingController();
  final _empresaCtrl = TextEditingController();
  final _ingresoCtrl = TextEditingController();
  final _regPwdCtrl = TextEditingController();

  // Domicilio controllers
  final _calleCtrl = TextEditingController();
  final _numExtCtrl = TextEditingController();
  final _numIntCtrl = TextEditingController();
  final _coloniaCtrl = TextEditingController();
  final _municipioCtrl = TextEditingController();
  final _estadoCtrl = TextEditingController();
  final _cpCtrl = TextEditingController();

  String _sexo = 'H';
  String _nacionalidad = AppConstants.nacionalidades.first;
  String _estadoCivil = AppConstants.estadosCiviles.first;
  String _pais = AppConstants.paises.first;

  @override
  void dispose() {
    _loginEmailCtrl.dispose();
    _loginPwdCtrl.dispose();
    _nombreCtrl.dispose();
    _segundoNombreCtrl.dispose();
    _paternoCtrl.dispose();
    _maternoCtrl.dispose();
    _fechaNacCtrl.dispose();
    _curpCtrl.dispose();
    _rfcCtrl.dispose();
    _regCorreoCtrl.dispose();
    _telMovilCtrl.dispose();
    _telAltCtrl.dispose();
    _ocupacionCtrl.dispose();
    _empresaCtrl.dispose();
    _ingresoCtrl.dispose();
    _regPwdCtrl.dispose();
    _calleCtrl.dispose();
    _numExtCtrl.dispose();
    _numIntCtrl.dispose();
    _coloniaCtrl.dispose();
    _municipioCtrl.dispose();
    _estadoCtrl.dispose();
    _cpCtrl.dispose();
    super.dispose();
  }

  Future<void> _submitLogin() async {
    if (!(_loginFormKey.currentState?.validate() ?? false)) return;
    setState(() {
      _loading = true;
      _errorMessage = null;
      _successMessage = null;
    });
    try {
      await widget.appState.login(
        _loginEmailCtrl.text.trim(),
        _loginPwdCtrl.text,
      );
    } catch (e) {
      if (mounted) {
        setState(() => _errorMessage = e.toString());
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final initial = DateTime(now.year - 25, now.month, now.day);
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1920, 1, 1),
      lastDate: now,
    );
    if (picked != null) {
      final y = picked.year.toString().padLeft(4, '0');
      final m = picked.month.toString().padLeft(2, '0');
      final d = picked.day.toString().padLeft(2, '0');
      setState(() => _fechaNacCtrl.text = '$y-$m-$d');
    }
  }

  Future<void> _submitRegister() async {
    if (!(_regFormKey.currentState?.validate() ?? false)) return;
    if (!AppFormatters.esMayorDeEdad(_fechaNacCtrl.text.trim())) {
      setState(() => _errorMessage = 'El cliente debe ser mayor de 18 años.');
      return;
    }

    setState(() {
      _loading = true;
      _errorMessage = null;
      _successMessage = null;
    });

    final payload = <String, dynamic>{
      'nombre': _nombreCtrl.text.trim(),
      if (_segundoNombreCtrl.text.trim().isNotEmpty)
        'segundoNombre': _segundoNombreCtrl.text.trim(),
      'apellidoPaterno': _paternoCtrl.text.trim(),
      'apellidoMaterno': _maternoCtrl.text.trim(),
      'fechaNacimiento': _fechaNacCtrl.text.trim(),
      'curp': _curpCtrl.text.trim().toUpperCase(),
      'rfc': _rfcCtrl.text.trim().toUpperCase(),
      'sexo': _sexo,
      'nacionalidad': _nacionalidad,
      'estadoCivil': _estadoCivil,
      'correo': _regCorreoCtrl.text.trim(),
      'telefonoMovil': _telMovilCtrl.text.trim(),
      if (_telAltCtrl.text.trim().isNotEmpty)
        'telefonoAlternativo': _telAltCtrl.text.trim(),
      'ocupacion': _ocupacionCtrl.text.trim(),
      'empresa': _empresaCtrl.text.trim(),
      'ingresoMensual': double.tryParse(_ingresoCtrl.text.trim()) ?? 0.0,
      'password': _regPwdCtrl.text,
      'domicilio': {
        'calle': _calleCtrl.text.trim(),
        'numeroExterior': _numExtCtrl.text.trim(),
        if (_numIntCtrl.text.trim().isNotEmpty)
          'numeroInterior': _numIntCtrl.text.trim(),
        'colonia': _coloniaCtrl.text.trim(),
        'municipio': _municipioCtrl.text.trim(),
        'estado': _estadoCtrl.text.trim(),
        'codigoPostal': _cpCtrl.text.trim(),
        'pais': _pais,
      },
    };

    try {
      final resp = await widget.appState.api.registrarClientePublico(payload);
      final cuenta = resp['numeroCuenta']?.toString() ?? '';
      if (mounted) {
        setState(() {
          _loginEmailCtrl.text = _regCorreoCtrl.text.trim();
          _isLogin = true;
          _successMessage = cuenta.isNotEmpty
              ? 'Registro exitoso. Se creó tu cuenta $cuenta. Ya puedes iniciar sesión.'
              : (resp['mensaje']?.toString() ??
                  'Registro exitoso. Ya puedes iniciar sesión con tus credenciales.');
        });
      }
    } catch (e) {
      if (mounted) setState(() => _errorMessage = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final prefixHint = AppFormatters.prefijoRfcCurp(
      nombre: _nombreCtrl.text,
      apellidoPaterno: _paternoCtrl.text,
      apellidoMaterno: _maternoCtrl.text,
      fechaNacimiento: _fechaNacCtrl.text,
    );

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 540),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: cs.primary,
                                borderRadius: BorderRadius.circular(14),
                              ),
                              child: Icon(Icons.bolt_rounded, color: cs.onPrimary, size: 28),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'PuntoRed',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                                  ),
                                  Text(
                                    'Plataforma H2H V2 • Web & Móvil',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
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
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  if (widget.appState.sessionNotice != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: cs.tertiaryContainer,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.info_outline_rounded, color: cs.onTertiaryContainer),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              widget.appState.sessionNotice!,
                              style: TextStyle(color: cs.onTertiaryContainer),
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (_errorMessage != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: cs.errorContainer,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.error_outline_rounded, color: cs.onErrorContainer),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: TextStyle(color: cs.onErrorContainer),
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (_successMessage != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFD1FAE5),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.check_circle_rounded, color: Color(0xFF065F46)),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              _successMessage!,
                              style: const TextStyle(color: Color(0xFF065F46)),
                            ),
                          ),
                        ],
                      ),
                    ),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          SegmentedButton<bool>(
                            segments: const [
                              ButtonSegment<bool>(
                                value: true,
                                label: Text('Iniciar Sesión'),
                                icon: Icon(Icons.login_rounded),
                              ),
                              ButtonSegment<bool>(
                                value: false,
                                label: Text('Crear Cuenta'),
                                icon: Icon(Icons.person_add_alt_1_rounded),
                              ),
                            ],
                            selected: {_isLogin},
                            onSelectionChanged: (sel) {
                              setState(() {
                                _isLogin = sel.first;
                                _errorMessage = null;
                                _successMessage = null;
                              });
                            },
                          ),
                          const SizedBox(height: 20),
                          if (_isLogin)
                            _buildLoginForm(tt, cs)
                          else
                            _buildRegisterForm(tt, cs, prefixHint),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'API actual: ${widget.appState.api.baseUrl}',
                    textAlign: TextAlign.center,
                    style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLoginForm(TextTheme tt, ColorScheme cs) {
    return Form(
      key: _loginFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Acceso unificado Web & Móvil',
            style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            'Usa las mismas credenciales registradas en la plataforma para consultar servicios, validar referencias y pagar.',
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
          const SizedBox(height: 16),
          TextFormField(
            key: const ValueKey('loginEmailField'),
            controller: _loginEmailCtrl,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'Correo electrónico',
              prefixIcon: Icon(Icons.email_outlined),
              border: OutlineInputBorder(),
            ),
            validator: (v) {
              if (v == null || v.trim().isEmpty || !v.contains('@')) {
                return 'Ingresa un correo electrónico válido';
              }
              return null;
            },
          ),
          const SizedBox(height: 14),
          TextFormField(
            key: const ValueKey('loginPasswordField'),
            controller: _loginPwdCtrl,
            obscureText: _obscureLoginPwd,
            decoration: InputDecoration(
              labelText: 'Contraseña',
              prefixIcon: const Icon(Icons.lock_outline_rounded),
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                onPressed: () => setState(() => _obscureLoginPwd = !_obscureLoginPwd),
                icon: Icon(
                  _obscureLoginPwd ? Icons.visibility_rounded : Icons.visibility_off_rounded,
                ),
              ),
            ),
            validator: (v) {
              if (v == null || v.isEmpty) return 'Ingresa tu contraseña';
              return null;
            },
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            key: const ValueKey('loginSubmitButton'),
            onPressed: _loading ? null : _submitLogin,
            icon: _loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.login_rounded),
            label: Text(_loading ? 'Validando acceso...' : 'Entrar a la Plataforma'),
          ),
        ],
      ),
    );
  }

  Widget _buildRegisterForm(TextTheme tt, ColorScheme cs, String prefixHint) {
    return Form(
      key: _regFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Registro de Nuevo Usuario',
            style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            'Tu cuenta funcionará automáticamente tanto en Web como en esta App Móvil.',
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
          const SizedBox(height: 14),
          _field(_nombreCtrl, 'Nombre(s) *', validator: _minLenValidator('Nombre', 3)),
          _field(_segundoNombreCtrl, 'Segundo nombre (opcional)'),
          _field(_paternoCtrl, 'Apellido paterno *',
              validator: _minLenValidator('Apellido paterno', 3)),
          _field(_maternoCtrl, 'Apellido materno *',
              validator: _minLenValidator('Apellido materno', 3)),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: TextFormField(
              controller: _fechaNacCtrl,
              readOnly: true,
              onTap: _pickBirthDate,
              decoration: InputDecoration(
                labelText: 'Fecha de nacimiento *',
                hintText: 'AAAA-MM-DD',
                border: const OutlineInputBorder(),
                suffixIcon: IconButton(
                  onPressed: _pickBirthDate,
                  icon: const Icon(Icons.calendar_month_rounded),
                ),
              ),
              validator: (v) =>
                  (v == null || v.trim().isEmpty) ? 'Selecciona tu fecha de nacimiento' : null,
            ),
          ),
          if (prefixHint.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Text(
                'Prefijo sugerido CURP/RFC: $prefixHint',
                style: tt.bodySmall?.copyWith(color: cs.primary, fontWeight: FontWeight.w600),
              ),
            ),
          _field(
            _curpCtrl,
            'CURP (18 caracteres) *',
            caps: TextCapitalization.characters,
            validator: (v) => (v == null || v.trim().length != 18)
                ? 'La CURP debe tener 18 caracteres'
                : null,
          ),
          _field(
            _rfcCtrl,
            'RFC (12 o 13 caracteres) *',
            caps: TextCapitalization.characters,
            validator: (v) => (v == null || v.trim().length < 12 || v.trim().length > 13)
                ? 'El RFC debe tener 12 o 13 caracteres'
                : null,
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: DropdownButtonFormField<String>(
              initialValue: _sexo,
              decoration: const InputDecoration(
                labelText: 'Sexo *',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'H', child: Text('Hombre (H)')),
                DropdownMenuItem(value: 'M', child: Text('Mujer (M)')),
              ],
              onChanged: (val) => setState(() => _sexo = val ?? 'H'),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: DropdownButtonFormField<String>(
              initialValue: _nacionalidad,
              decoration: const InputDecoration(
                labelText: 'Nacionalidad *',
                border: OutlineInputBorder(),
              ),
              items: AppConstants.nacionalidades
                  .map((n) => DropdownMenuItem(value: n, child: Text(n)))
                  .toList(),
              onChanged: (val) =>
                  setState(() => _nacionalidad = val ?? AppConstants.nacionalidades.first),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: DropdownButtonFormField<String>(
              initialValue: _estadoCivil,
              decoration: const InputDecoration(
                labelText: 'Estado civil *',
                border: OutlineInputBorder(),
              ),
              items: AppConstants.estadosCiviles
                  .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                  .toList(),
              onChanged: (val) =>
                  setState(() => _estadoCivil = val ?? AppConstants.estadosCiviles.first),
            ),
          ),
          _field(
            _regCorreoCtrl,
            'Correo electrónico *',
            keyboard: TextInputType.emailAddress,
            validator: (v) =>
                (v == null || !v.contains('@')) ? 'Correo electrónico inválido' : null,
          ),
          _field(
            _telMovilCtrl,
            'Teléfono móvil (10 dígitos) *',
            keyboard: TextInputType.phone,
            validator: (v) =>
                (v == null || !RegExp(r'^\d{10}$').hasMatch(v.trim()))
                    ? 'Debe contener 10 dígitos'
                    : null,
          ),
          _field(_telAltCtrl, 'Teléfono alternativo (10 dígitos, opcional)',
              keyboard: TextInputType.phone),
          _field(_ocupacionCtrl, 'Ocupación *', validator: _requiredValidator('Ocupación')),
          _field(_empresaCtrl, 'Empresa *', validator: _requiredValidator('Empresa')),
          _field(
            _ingresoCtrl,
            'Ingreso mensual (MXN) *',
            keyboard: const TextInputType.numberWithOptions(decimal: true),
            validator: (v) {
              final d = double.tryParse(v?.trim() ?? '');
              if (d == null || d <= 0) return 'Ingresa un monto mayor a 0';
              return null;
            },
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 14),
            child: TextFormField(
              controller: _regPwdCtrl,
              obscureText: _obscureRegPwd,
              decoration: InputDecoration(
                labelText: 'Contraseña (mín. 8 caracteres) *',
                border: const OutlineInputBorder(),
                suffixIcon: IconButton(
                  onPressed: () => setState(() => _obscureRegPwd = !_obscureRegPwd),
                  icon: Icon(
                    _obscureRegPwd ? Icons.visibility_rounded : Icons.visibility_off_rounded,
                  ),
                ),
              ),
              validator: (v) =>
                  (v == null || v.length < 8) ? 'Debe tener al menos 8 caracteres' : null,
            ),
          ),
          Text(
            'Domicilio',
            style: tt.titleSmall?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          _field(_calleCtrl, 'Calle *', validator: _requiredValidator('Calle')),
          _field(_numExtCtrl, 'Número exterior *',
              validator: _requiredValidator('Número exterior')),
          _field(_numIntCtrl, 'Número interior (opcional)'),
          _field(_coloniaCtrl, 'Colonia *', validator: _requiredValidator('Colonia')),
          _field(_municipioCtrl, 'Municipio / Alcaldía *',
              validator: _requiredValidator('Municipio')),
          _field(_estadoCtrl, 'Estado *', validator: _requiredValidator('Estado')),
          _field(
            _cpCtrl,
            'Código Postal (5 dígitos) *',
            keyboard: TextInputType.number,
            validator: (v) =>
                (v == null || !RegExp(r'^\d{5}$').hasMatch(v.trim()))
                    ? 'Código postal de 5 dígitos requerido'
                    : null,
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: DropdownButtonFormField<String>(
              initialValue: _pais,
              decoration: const InputDecoration(
                labelText: 'País *',
                border: OutlineInputBorder(),
              ),
              items: AppConstants.paises
                  .map((p) => DropdownMenuItem(value: p, child: Text(p)))
                  .toList(),
              onChanged: (val) => setState(() => _pais = val ?? AppConstants.paises.first),
            ),
          ),
          FilledButton.icon(
            onPressed: _loading ? null : _submitRegister,
            icon: _loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.person_add_alt_1_rounded),
            label: Text(_loading ? 'Creando cuenta...' : 'Registrarme y Abrir Cuenta'),
          ),
        ],
      ),
    );
  }

  Widget _field(
    TextEditingController ctrl,
    String label, {
    TextInputType? keyboard,
    TextCapitalization caps = TextCapitalization.none,
    String? Function(String?)? validator,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        controller: ctrl,
        keyboardType: keyboard,
        textCapitalization: caps,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
        ),
        validator: validator,
        onChanged: (_) => setState(() {}),
      ),
    );
  }

  String? Function(String?) _requiredValidator(String field) {
    return (v) => (v == null || v.trim().isEmpty) ? '$field es obligatorio' : null;
  }

  String? Function(String?) _minLenValidator(String field, int min) {
    return (v) => (v == null || v.trim().length < min)
        ? '$field requiere al menos $min caracteres'
        : null;
  }
}
