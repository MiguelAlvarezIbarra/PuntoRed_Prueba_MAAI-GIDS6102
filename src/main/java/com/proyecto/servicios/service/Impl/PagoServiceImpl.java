package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
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
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CODIGO_EXITO_GESTOPAGO = "01";
    private static final String SIN_MENSAJE_PROVEEDOR = "Sin mensaje del proveedor";

    private final SecureRandom secureRandom = new SecureRandom();

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoCryptoUtil gestoPagoCryptoUtil;
    private final Correo2faService correo2faService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.tx.upc-default:0}")
    private String upcDefault;

    @Value("${seguridad.2fa.expiracion-minutos:5}")
    private long expiracionPinMinutos;

    @Value("${seguridad.2fa.mostrar-pin-respuesta:true}")
    private boolean mostrarPinEnRespuesta;

    public PagoServiceImpl(
            CuentaRepository cuentaRepository,
            UsuarioRepository usuarioRepository,
            TransaccionPagoRepository transaccionPagoRepository,
            GestoPagoTokenService gestoPagoTokenService,
            GestoPagoServiceClient gestoPagoServiceClient,
            GestoPagoCryptoUtil gestoPagoCryptoUtil,
            Correo2faService correo2faService) {
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoServiceClient = gestoPagoServiceClient;
        this.gestoPagoCryptoUtil = gestoPagoCryptoUtil;
        this.correo2faService = correo2faService;
    }

    @Override
    public Pin2faResponse enviarPin2fa(SolicitudPin2faRequest request) {
        log.info("Inicio enviarPin2fa - numeroCuenta={}", request.getNumeroCuenta());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta()).orElseThrow();
        Cliente cliente = cuenta.getCliente();
        Usuario usuario = usuarioRepository.findByCliente_Id(cliente.getId()).orElseThrow();

        String pin = generarPinSeisDigitos();
        usuario.setPin2faHash(correo2faService.hashearPin(pin));
        usuario.setPin2faExpiracion(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(expiracionPinMinutos));
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
    public PagoResponse realizarPago(PagoRequest request) {
        log.info("Inicio realizarPago - numeroCuenta={} idServicio={} idProducto={} tipoPago={}",
                request.getNumeroCuenta(), request.getIdServicio(), request.getIdProducto(), request.getTipoPago());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta()).orElseThrow();
        Cliente cliente = cuenta.getCliente();
        consumirPin2fa(cliente.getId());

        SenTxRequest senTxRequest = construirSenTxRequest(request);
        String senTxCifrado = gestoPagoCryptoUtil.cifrarSenTx(senTxRequest);

        String token = obtenerTokenActivo();
        gestoPagoCryptoUtil.verificarSenTxConToken(senTxCifrado, senTxRequest, token);

        String formBody = "signed=" + URLEncoder.encode(senTxCifrado, StandardCharsets.UTF_8);
        String xmlResponse = gestoPagoServiceClient.sendTx("Bearer " + token, formBody);
        ResultadoGestoPago resultado = parsearRespuestaSendTx(xmlResponse);

        BigDecimal montoLimpio = senTxRequest.getMontoPago();
        BigDecimal saldoAnterior = limpiarMonto(cuenta.getSaldo());
        BigDecimal nuevoSaldo = saldoAnterior;

        boolean pagoExitoso = CODIGO_EXITO_GESTOPAGO.equals(resultado.codigo());
        if (pagoExitoso) {
            nuevoSaldo = limpiarMonto(saldoAnterior.subtract(montoLimpio));
            cuenta.setSaldo(nuevoSaldo);
            cuentaRepository.save(cuenta);
        }

        TransaccionPago transaccion = guardarTransaccion(cuenta, request, senTxRequest, senTxCifrado, resultado);
        PagoData data = construirPagoData(cuenta, request, senTxRequest, saldoAnterior, nuevoSaldo, transaccion, resultado);

        PagoResponse response = new PagoResponse();
        response.setCodigo(pagoExitoso ? 0 : parsearCodigoNumerico(resultado.codigo()));
        response.setMensaje(pagoExitoso ? "01: Pago Exitoso" : resultado.codigo() + ": " + resultado.mensaje());
        response.setPago(data);

        log.info("Fin realizarPago - numeroCuenta={} codigoGestoPago={}",
                cuenta.getNumeroCuenta(), resultado.codigo());
        return response;
    }

    private void consumirPin2fa(Integer clienteId) {
        usuarioRepository.findByCliente_Id(clienteId).ifPresent(usuario -> {
            usuario.setPin2faHash(null);
            usuario.setPin2faExpiracion(null);
            usuarioRepository.save(usuario);
        });
    }

    private SenTxRequest construirSenTxRequest(PagoRequest request) {
        SenTxRequest senTxRequest = new SenTxRequest();
        senTxRequest.setIdProducto(request.getIdProducto());
        senTxRequest.setIdServicio(request.getIdServicio());
        senTxRequest.setTelefono(request.obtenerTelefonoSenTx());
        senTxRequest.setReferencia(request.obtenerDestinoPago());
        senTxRequest.setHoraLocal(LocalDateTime.now(ZoneId.systemDefault()).format(FORMATO_HORA_LOCAL));
        senTxRequest.setMontoPago(request.obtenerMontoLimpio());
        senTxRequest.setUpc(request.obtenerUpcEfectivo(upcDefault));
        senTxRequest.setUnidad(request.obtenerUnidadEfectiva(idDistribuidor));
        return senTxRequest;
    }

    private String obtenerTokenActivo() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        if (tokenOpt.isEmpty()) {
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        }
        return tokenOpt.map(GestoPagoToken::getToken).orElse("");
    }

    private ResultadoGestoPago parsearRespuestaSendTx(String xmlResponse) {
        JSONObject json = XML.toJSONObject(xmlResponse != null ? xmlResponse : "");

        JSONObject resp = json.optJSONObject("RESPONSE");
        if (resp != null) {
            JSONObject msgObj = resp.optJSONObject("MENSAJE");
            String codigo = msgObj != null ? normalizarCodigo(msgObj.opt("CODIGO")) : "99";
            String texto = msgObj != null ? msgObj.optString("TEXTO", SIN_MENSAJE_PROVEEDOR) : SIN_MENSAJE_PROVEEDOR;
            String idTx = valorComoTexto(resp.opt("ID_TX"));
            String numAut = valorComoTexto(resp.opt("NUM_AUTORIZACION"));
            String fecha = valorComoTexto(resp.opt("FECHA"));
            String pin = valorComoTexto(resp.opt("PIN"));
            String leyenda = msgObj != null ? valorComoTexto(msgObj.opt("LEYENDA")) : null;
            return new ResultadoGestoPago(codigo, texto, idTx, numAut, fecha, pin, leyenda);
        }

        JSONObject aut = json.optJSONObject("AUTORIZACION");
        if (aut != null) {
            String codigo = normalizarCodigo(aut.opt("CODIGO_MENSAJE"));
            String texto = aut.optString("MENSAJE", SIN_MENSAJE_PROVEEDOR);
            String idTx = valorComoTexto(aut.opt("ID_TX"));
            String numAut = valorComoTexto(aut.opt("NUM_AUTORIZACION"));
            String fecha = valorComoTexto(aut.opt("FECHA"));
            String pin = valorComoTexto(aut.opt("PIN"));
            String leyenda = valorComoTexto(aut.opt("LEYENDA"));
            return new ResultadoGestoPago(codigo, texto, idTx, numAut, fecha, pin, leyenda);
        }

        return new ResultadoGestoPago("99", SIN_MENSAJE_PROVEEDOR, null, null, null, null, null);
    }

    private TransaccionPago guardarTransaccion(
            Cuenta cuenta,
            PagoRequest request,
            SenTxRequest senTxRequest,
            String senTxCifrado,
            ResultadoGestoPago resultado) {

        TransaccionPago tx = new TransaccionPago();
        tx.setCuenta(cuenta);
        tx.setNumeroCuenta(cuenta.getNumeroCuenta());
        tx.setIdServicio(request.getIdServicio());
        tx.setIdProducto(request.getIdProducto());
        tx.setTipoPago(request.getTipoPago());
        tx.setTelefonoReferencia(senTxRequest.getReferencia());
        tx.setMonto(senTxRequest.getMontoPago());
        tx.setUpc(senTxRequest.getUpc());
        tx.setUnidad(senTxRequest.getUnidad());
        tx.setHoraLocal(senTxRequest.getHoraLocal());
        tx.setSenTxCifrado(senTxCifrado);
        tx.setIdTxGestoPago(resultado.idTx());
        tx.setNumAutorizacion(resultado.numAutorizacion());
        tx.setCodigoRespuesta(resultado.codigo());
        tx.setMensajeRespuesta(CODIGO_EXITO_GESTOPAGO.equals(resultado.codigo())
                ? "01: Pago Exitoso"
                : resultado.mensaje());
        return transaccionPagoRepository.save(tx);
    }

    private PagoData construirPagoData(
            Cuenta cuenta,
            PagoRequest request,
            SenTxRequest senTxRequest,
            BigDecimal saldoAnterior,
            BigDecimal nuevoSaldo,
            TransaccionPago transaccion,
            ResultadoGestoPago resultado) {

        PagoData data = new PagoData();
        data.setIdTransaccionLocal(transaccion.getId());
        data.setNumeroCuenta(cuenta.getNumeroCuenta());
        data.setIdServicio(request.getIdServicio());
        data.setIdProducto(request.getIdProducto());
        data.setTipoPago(request.getTipoPago());
        data.setTelefonoReferencia(senTxRequest.getReferencia());
        data.setMonto(senTxRequest.getMontoPago());
        data.setSaldoAnterior(saldoAnterior);
        data.setSaldoActual(nuevoSaldo);
        data.setUpc(senTxRequest.getUpc());
        data.setUnidad(senTxRequest.getUnidad());
        data.setHoraLocal(senTxRequest.getHoraLocal());
        data.setCodigoGestoPago(resultado.codigo());
        data.setMensajeGestoPago(resultado.mensaje());
        data.setIdTxGestoPago(resultado.idTx());
        data.setNumAutorizacion(resultado.numAutorizacion());
        data.setFechaGestoPago(resultado.fecha());
        data.setPin(resultado.pin());
        data.setLeyenda(resultado.leyenda());
        return data;
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
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
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
        if (codigo != null && codigo.matches("^\\d+$")) {
            return Integer.parseInt(codigo);
        }
        return 24;
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
