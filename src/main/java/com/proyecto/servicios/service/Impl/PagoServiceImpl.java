package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.exception.gestopago.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.gestopago.GestoPagoResponseException;
import com.proyecto.servicios.exception.gestopago.GestoPagoTimeoutException;
import com.proyecto.servicios.model.PagoData;
import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;
import com.proyecto.servicios.model.gestopago.SenTxRequest;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.Correo2faService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.util.GestoPagoCryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CODIGO_EXITO_GESTOPAGO = "01";
    private static final String TIPO_TELEFONO = "TELEFONO";
    private static final String TIPO_REFERENCIA = "REFERENCIA";

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionPagoRepository transaccionPagoRepository;

    @Autowired
    private GestoPagoTokenService gestoPagoTokenService;

    @Autowired
    private GestoPagoServiceClient gestoPagoServiceClient;

    @Autowired
    private GestoPagoCryptoUtil gestoPagoCryptoUtil;

    @Autowired
    private Correo2faService correo2faService;

    @Autowired
    private HashUtil hashUtil;

    @Autowired
    private SeguridadGuard seguridadGuard;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.tx.upc-default:0}")
    private String upcDefault;

    @Value("${seguridad.2fa.expiracion-minutos:5}")
    private long expiracionPinMinutos;

    @Value("${seguridad.2fa.mostrar-pin-respuesta:true}")
    private boolean mostrarPinEnRespuesta;

    @Override
    public Pin2faResponse enviarPin2fa(SolicitudPin2faRequest request, String authorizationHeader) {
        log.info("Inicio enviarPin2fa - numeroCuenta={}", request.getNumeroCuenta());

        Cuenta cuenta = obtenerCuentaValida(request.getNumeroCuenta());
        Cliente cliente = validarClienteYCuentaActivos(cuenta);
        seguridadGuard.validarAccesoClienteIdSiPresente(authorizationHeader, cliente.getId());

        Usuario usuario = usuarioRepository.findByCliente_Id(cliente.getId())
                .orElseThrow(() -> new ClienteNoEncontradoException(MensajeError.CLIENTE_NO_ENCONTRADO));

        String pin = generarPinSeisDigitos();
        usuario.setPin2faHash(hashUtil.sha256(pin));
        usuario.setPin2faExpiracion(LocalDateTime.now().plusMinutes(expiracionPinMinutos));
        usuarioRepository.save(usuario);

        boolean enviadoSmtp = correo2faService.enviarPinPorCorreo(cliente.getCorreo(), cliente.getNombre(), pin);

        Pin2faResponse response = new Pin2faResponse();
        response.setCodigo(0);
        response.setMensaje(enviadoSmtp
                ? "PIN de autenticación 2FA enviado al correo del cliente"
                : "PIN de autenticación 2FA generado para el correo del cliente");
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        response.setCorreoDestino(enmascararCorreo(cliente.getCorreo()));
        response.setExpiraEnSegundos(expiracionPinMinutos * 60);
        if (mostrarPinEnRespuesta || !enviadoSmtp) {
            response.setPin2fa(pin);
        }

        log.info("Fin enviarPin2fa - clienteId={}", cliente.getId());
        return response;
    }

    @Override
    public PagoResponse realizarPago(PagoRequest request, String authorizationHeader) {
        log.info("Inicio realizarPago - numeroCuenta={} idServicio={} idProducto={} tipoPago={}",
                request.getNumeroCuenta(), request.getIdServicio(), request.getIdProducto(), request.getTipoPago());

        // 1. Verificar que la cuenta exista y que el cliente este ACTIVO
        Cuenta cuenta = obtenerCuentaValida(request.getNumeroCuenta());
        Cliente cliente = validarClienteYCuentaActivos(cuenta);
        seguridadGuard.validarAccesoClienteIdSiPresente(authorizationHeader, cliente.getId());

        // 2. Validar bandera Teléfono (Recarga) o Referencia (Pago Bancario)
        String telefonoReferencia = obtenerTelefonoOReferenciaSegunBandera(request);

        // Normalizar monto eliminando ceros innecesarios
        BigDecimal montoLimpio = limpiarMonto(request.getMonto());

        // 3. Verificar que el saldo de la cuenta sea suficiente
        if (cuenta.getSaldo().compareTo(montoLimpio) < 0) {
            throw new SaldoInsuficienteException(MensajeError.SALDO_INSUFICIENTE);
        }

        // 4. Verificación final de 2FA (PIN enviado al correo)
        verificarPin2fa(cliente.getId(), request.getPin2fa());

        // 5. Construir y cifrar la petición SenTx para GestoPago
        String horaLocal = LocalDateTime.now().format(FORMATO_HORA_LOCAL);
        String upc = (request.getUpc() != null && !request.getUpc().isBlank()) ? request.getUpc() : upcDefault;
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank())
                ? request.getUnidad()
                : String.valueOf(idDistribuidor);

        SenTxRequest senTxRequest = SenTxRequest.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(TIPO_TELEFONO.equals(request.getTipoPago()) ? telefonoReferencia : "")
                .referencia(telefonoReferencia)
                .horaLocal(horaLocal)
                .montoPago(montoLimpio)
                .upc(upc)
                .unidad(unidad)
                .build();

        String senTxCifrado = gestoPagoCryptoUtil.cifrarSenTx(senTxRequest);

        // 6. Obtener token de GestoPago y comparar/verificar el SenTx Cifrado con el Token
        String token = obtenerTokenObligatorio();
        gestoPagoCryptoUtil.verificarSenTxConToken(senTxCifrado, senTxRequest, token);

        // 7. Invocar sendTx.do en GestoPago y procesar la respuesta XML
        String xmlResponse = invocarSendTxGestoPago(token, senTxCifrado);
        ResultadoGestoPago resultado = parsearRespuestaSendTx(xmlResponse);

        if (!CODIGO_EXITO_GESTOPAGO.equals(resultado.codigo())) {
            guardarTransaccion(cuenta, request, telefonoReferencia, montoLimpio, upc, unidad, horaLocal, senTxCifrado, resultado);
            int codigoError = parsearCodigoNumerico(resultado.codigo());
            throw new ValidacionException(codigoError,
                    MensajeError.GESTOPAGO_PAGO_RECHAZADO.formatear(resultado.codigo(), resultado.mensaje()));
        }

        // 8. Código 01: Pago Exitoso -> descontar saldo y registrar transacción
        BigDecimal saldoAnterior = limpiarMonto(cuenta.getSaldo());
        BigDecimal nuevoSaldo = limpiarMonto(cuenta.getSaldo().subtract(montoLimpio));
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);

        TransaccionPago transaccion = guardarTransaccion(
                cuenta, request, telefonoReferencia, montoLimpio, upc, unidad, horaLocal, senTxCifrado, resultado);

        PagoData data = new PagoData();
        data.setIdTransaccionLocal(transaccion.getId());
        data.setNumeroCuenta(cuenta.getNumeroCuenta());
        data.setIdServicio(request.getIdServicio());
        data.setIdProducto(request.getIdProducto());
        data.setTipoPago(request.getTipoPago());
        data.setTelefonoReferencia(telefonoReferencia);
        data.setMonto(montoLimpio);
        data.setSaldoAnterior(saldoAnterior);
        data.setSaldoActual(nuevoSaldo);
        data.setUpc(upc);
        data.setUnidad(unidad);
        data.setHoraLocal(horaLocal);
        data.setCodigoGestoPago(resultado.codigo());
        data.setMensajeGestoPago(resultado.mensaje());
        data.setIdTxGestoPago(resultado.idTx());
        data.setNumAutorizacion(resultado.numAutorizacion());
        data.setFechaGestoPago(resultado.fecha());
        data.setPin(resultado.pin());
        data.setLeyenda(resultado.leyenda());

        PagoResponse response = new PagoResponse();
        response.setCodigo(0);
        response.setMensaje("01: Pago Exitoso");
        response.setPago(data);

        log.info("Fin realizarPago exitoso - numeroCuenta={} idTxGestoPago={}",
                cuenta.getNumeroCuenta(), resultado.idTx());
        return response;
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    private Cuenta obtenerCuentaValida(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(MensajeError.CUENTA_NO_ENCONTRADA));
    }

    private Cliente validarClienteYCuentaActivos(Cuenta cuenta) {
        Cliente cliente = cuenta.getCliente();
        if (cliente == null) {
            throw new ClienteNoEncontradoException(MensajeError.CLIENTE_NO_ENCONTRADO);
        }
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException(MensajeError.CLIENTE_INACTIVO_COMPRA);
        }
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ClienteInactivoException(MensajeError.CUENTA_INACTIVA_COMPRA);
        }
        return cliente;
    }

    private String obtenerTelefonoOReferenciaSegunBandera(PagoRequest request) {
        if (TIPO_TELEFONO.equals(request.getTipoPago())) {
            if (request.getTelefono() == null || request.getTelefono().isBlank()) {
                throw new ValidacionException(MensajeError.TELEFONO_RECARGA_OBLIGATORIO);
            }
            return request.getTelefono().trim();
        }
        if (TIPO_REFERENCIA.equals(request.getTipoPago())) {
            if (request.getReferencia() == null || request.getReferencia().isBlank()) {
                throw new ValidacionException(MensajeError.REFERENCIA_PAGO_OBLIGATORIA);
            }
            return request.getReferencia().trim();
        }
        throw new ValidacionException(MensajeError.VALIDACION_GENERAL);
    }

    private void verificarPin2fa(Integer clienteId, String pinIngresado) {
        Usuario usuario = usuarioRepository.findByCliente_Id(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(MensajeError.CLIENTE_NO_ENCONTRADO));

        if (usuario.getPin2faHash() == null || usuario.getPin2faExpiracion() == null) {
            throw new ValidacionException(MensajeError.PIN_2FA_NO_SOLICITADO);
        }

        if (LocalDateTime.now().isAfter(usuario.getPin2faExpiracion())) {
            usuario.setPin2faHash(null);
            usuario.setPin2faExpiracion(null);
            usuarioRepository.save(usuario);
            throw new ValidacionException(MensajeError.PIN_2FA_EXPIRADO);
        }

        String hashIngresado = hashUtil.sha256(pinIngresado);
        if (!hashIngresado.equals(usuario.getPin2faHash())) {
            throw new ValidacionException(MensajeError.PIN_2FA_INVALIDO);
        }

        // Consumir PIN 2FA tras validacion exitosa (un solo uso)
        usuario.setPin2faHash(null);
        usuario.setPin2faExpiracion(null);
        usuarioRepository.save(usuario);
    }

    private String obtenerTokenObligatorio() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        if (tokenOpt.isEmpty()) {
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        }
        return tokenOpt
                .map(GestoPagoToken::getToken)
                .orElseThrow(() -> new GestoPagoAuthenticationException(
                        "No se pudo obtener un token válido de GestoPago para verificar SenTx."));
    }

    private String invocarSendTxGestoPago(String token, String senTxCifrado) {
        String formBody = "signed=" + URLEncoder.encode(senTxCifrado, StandardCharsets.UTF_8);
        try {
            return gestoPagoServiceClient.sendTx("Bearer " + token, formBody);
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.warn("GestoPago rechazó el token en sendTx.do, renovando token y reintentando...");
            gestoPagoTokenService.renovarToken();
            String nuevoToken = obtenerTokenObligatorio();
            try {
                return gestoPagoServiceClient.sendTx("Bearer " + nuevoToken, formBody);
            } catch (FeignException.Unauthorized | FeignException.Forbidden e2) {
                throw new GestoPagoAuthenticationException(
                        "GestoPago rechazó la autenticación en sendTx.do incluso tras renovar el token", e2);
            }
        } catch (RetryableException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new GestoPagoTimeoutException("Timeout al llamar a sendTx.do en GestoPago", e);
            }
            throw new GestoPagoCommunicationException("Error de comunicación con GestoPago (sendTx.do)", e);
        } catch (FeignException e) {
            throw new GestoPagoResponseException(
                    "GestoPago respondió con un error HTTP en sendTx.do: " + e.status(),
                    e.status(), e);
        }
    }

    private ResultadoGestoPago parsearRespuestaSendTx(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new GestoPagoResponseException("La respuesta de sendTx.do está vacía.", null);
        }

        JSONObject json;
        try {
            json = XML.toJSONObject(xmlResponse);
        } catch (Exception e) {
            throw new GestoPagoResponseException("No se pudo convertir el XML de sendTx.do a JSON.", null, e);
        }

        if (json.has("RESPONSE")) {
            JSONObject resp = json.getJSONObject("RESPONSE");
            JSONObject msgObj = resp.optJSONObject("MENSAJE");
            String codigo = msgObj != null ? normalizarCodigo(msgObj.opt("CODIGO")) : "99";
            String texto = msgObj != null ? msgObj.optString("TEXTO", "Sin mensaje del proveedor") : "Sin mensaje del proveedor";
            String idTx = valorComoTexto(resp.opt("ID_TX"));
            String numAut = valorComoTexto(resp.opt("NUM_AUTORIZACION"));
            String fecha = valorComoTexto(resp.opt("FECHA"));
            String pin = valorComoTexto(resp.opt("PIN"));
            String leyenda = msgObj != null ? valorComoTexto(msgObj.opt("LEYENDA")) : null;
            return new ResultadoGestoPago(codigo, texto, idTx, numAut, fecha, pin, leyenda);
        }

        if (json.has("AUTORIZACION")) {
            JSONObject aut = json.getJSONObject("AUTORIZACION");
            String codigo = normalizarCodigo(aut.opt("CODIGO_MENSAJE"));
            String texto = aut.optString("MENSAJE", "Sin mensaje del proveedor");
            String idTx = valorComoTexto(aut.opt("ID_TX"));
            String numAut = valorComoTexto(aut.opt("NUM_AUTORIZACION"));
            String fecha = valorComoTexto(aut.opt("FECHA"));
            String pin = valorComoTexto(aut.opt("PIN"));
            String leyenda = valorComoTexto(aut.opt("LEYENDA"));
            return new ResultadoGestoPago(codigo, texto, idTx, numAut, fecha, pin, leyenda);
        }

        throw new GestoPagoResponseException("Estructura XML inesperada en sendTx.do", null);
    }

    private TransaccionPago guardarTransaccion(
            Cuenta cuenta,
            PagoRequest request,
            String telefonoReferencia,
            BigDecimal montoLimpio,
            String upc,
            String unidad,
            String horaLocal,
            String senTxCifrado,
            ResultadoGestoPago resultado) {

        TransaccionPago tx = new TransaccionPago();
        tx.setCuenta(cuenta);
        tx.setNumeroCuenta(cuenta.getNumeroCuenta());
        tx.setIdServicio(request.getIdServicio());
        tx.setIdProducto(request.getIdProducto());
        tx.setTipoPago(request.getTipoPago());
        tx.setTelefonoReferencia(telefonoReferencia);
        tx.setMonto(montoLimpio);
        tx.setUpc(upc);
        tx.setUnidad(unidad);
        tx.setHoraLocal(horaLocal);
        tx.setSenTxCifrado(senTxCifrado);
        tx.setIdTxGestoPago(resultado.idTx());
        tx.setNumAutorizacion(resultado.numAutorizacion());
        tx.setCodigoRespuesta(resultado.codigo());
        tx.setMensajeRespuesta(CODIGO_EXITO_GESTOPAGO.equals(resultado.codigo())
                ? "01: Pago Exitoso"
                : resultado.mensaje());
        return transaccionPagoRepository.save(tx);
    }

    private String generarPinSeisDigitos() {
        int numero = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(numero);
    }

    private BigDecimal limpiarMonto(BigDecimal monto) {
        if (monto == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal limpio = monto.stripTrailingZeros();
        if (limpio.scale() < 0) {
            limpio = limpio.setScale(0);
        }
        return limpio;
    }

    private String normalizarCodigo(Object raw) {
        if (raw == null) {
            return "99";
        }
        String texto = String.valueOf(raw).trim();
        if (texto.length() == 1 && Character.isDigit(texto.charAt(0))) {
            return "0" + texto;
        }
        return texto;
    }

    private int parsearCodigoNumerico(String codigo) {
        try {
            return Integer.parseInt(codigo);
        } catch (NumberFormatException e) {
            return MensajeError.GESTOPAGO_PAGO_RECHAZADO.getCodigo();
        }
    }

    private String valorComoTexto(Object val) {
        if (val == null) {
            return null;
        }
        String str = String.valueOf(val).trim();
        return str.isEmpty() ? null : str;
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "***";
        }
        int arroba = correo.indexOf('@');
        String prefijo = correo.substring(0, Math.min(2, arroba));
        return prefijo + "***" + correo.substring(arroba);
    }

    private record ResultadoGestoPago(
            String codigo,
            String mensaje,
            String idTx,
            String numAutorizacion,
            String fecha,
            String pin,
            String leyenda
    ) {
    }
}
