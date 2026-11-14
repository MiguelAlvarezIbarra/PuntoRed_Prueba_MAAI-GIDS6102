package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.PuntoredApiClient;
import com.proyecto.servicios.client.PuntoredHttpResult;
import com.proyecto.servicios.config.PuntoredProperties;
import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.puntored.DetallePagoRequest;
import com.proyecto.servicios.model.puntored.DetallePagoResponse;
import com.proyecto.servicios.model.puntored.HistorialPagoItem;
import com.proyecto.servicios.model.puntored.HistorialPagosRequest;
import com.proyecto.servicios.model.puntored.HistorialPagosResponse;
import com.proyecto.servicios.model.puntored.OpcionMontoConvenio;
import com.proyecto.servicios.model.puntored.PagoPuntoredRequest;
import com.proyecto.servicios.model.puntored.PagoPuntoredResponse;
import com.proyecto.servicios.model.puntored.TicketData;
import com.proyecto.servicios.model.puntored.ValidacionReferenciaResponse;
import com.proyecto.servicios.model.puntored.ValidarReferenciaRequest;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.PuntoredPagoService;
import com.proyecto.servicios.service.PuntoredTokenService;
import com.proyecto.servicios.util.CryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import com.proyecto.servicios.util.Roles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Implementacion integral del flujo de pagos PuntoRed H2H V2:
 * 1) Validacion de referencia y reglas de monto con token firmado AES-GCM.
 * 2) Confirmacion explicita + verificacion 2FA + ejecucion contra API real.
 * 3) Diferenciacion explicita de estados EXITOSA, RECHAZADA, PENDIENTE y NO_CONFIRMADA.
 * 4) Historial filtrado por cliente autenticado y reverificacion de transacciones pendientes.
 */
@Service
@Slf4j
public class PuntoredPagoServiceImpl implements PuntoredPagoService {

    private static final String PROVEEDOR_PUNTORED = "PUNTORED";
    private static final String ESTADO_EXITOSA = "EXITOSA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_NO_CONFIRMADA = "NO_CONFIRMADA";
    private static final String OP_RECARGA = "RECARGA";
    private static final String OP_PAQUETE = "PAQUETE";
    private static final String OP_RECAUDO = "RECAUDO";
    private static final String OP_RECAUDO_V2 = "RECAUDO_V2";
    private static final String OP_PIN = "PIN";
    private static final String CAMPO_DATA = "data";
    private static final String CAMPO_POS = "pointOfSale";
    private static final String CAMPO_TERMINAL = "terminal";
    private static final String CAMPO_TRACE = "trace";
    private static final String CAMPO_TX_PASSWORD = "transactionalPassword";
    private static final String CAMPO_AMOUNT = "amount";
    private static final String CAMPO_PRODUCT_CODE = "productCode";
    private static final String CAMPO_AGREEMENT_ID = "agreementId";

    private static final Pattern PATRON_CELULAR = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PATRON_REFERENCIA_GENERAL = Pattern.compile("^[A-Za-z0-9._-]{4,50}$");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long EXPIRACION_TOKEN_VALIDACION_SEGUNDOS = 900L;

    private final PuntoredTokenService tokenService;
    private final PuntoredApiClient apiClient;
    private final PuntoredProperties properties;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final CryptoUtil cryptoUtil;
    private final HashUtil hashUtil;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();
    private final AtomicInteger secuenciaTrace = new AtomicInteger(10);

    public PuntoredPagoServiceImpl(
            PuntoredTokenService tokenService,
            PuntoredApiClient apiClient,
            PuntoredProperties properties,
            CuentaRepository cuentaRepository,
            UsuarioRepository usuarioRepository,
            TransaccionPagoRepository transaccionPagoRepository,
            CryptoUtil cryptoUtil,
            HashUtil hashUtil,
            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
        this.properties = properties;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.cryptoUtil = cryptoUtil;
        this.hashUtil = hashUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidacionReferenciaResponse validarReferencia(ValidarReferenciaRequest request, InfoToken infoToken) {
        String tipoOp = normalizarOperacion(request.getTipoOperacion());
        String ref1 = request.getReferencia1().trim();
        String ref2 = request.getReferencia2() != null ? request.getReferencia2().trim() : "";
        String trace = generarTraceNumerico();

        if (OP_RECAUDO.equals(tipoOp)) {
            return validarRecaudoV1(request, infoToken, ref1, ref2, trace);
        }
        if (OP_RECAUDO_V2.equals(tipoOp)) {
            return validarRecaudoV2(request, infoToken, ref1, trace);
        }
        return validarReferenciaLineaOPin(request, infoToken, tipoOp, ref1, ref2, trace);
    }

    @Override
    public PagoPuntoredResponse realizarPago(PagoPuntoredRequest request, InfoToken infoToken) {
        if (!Boolean.TRUE.equals(request.getConfirmacionExplicita())) {
            throw new ValidacionException(MensajeError.CONFIRMACION_PAGO_REQUERIDA);
        }

        String tipoOp = normalizarOperacion(request.getTipoOperacion());
        TokenValidacionPayload validacion = verificarTokenValidacion(request, infoToken, tipoOp);
        Cuenta cuenta = validarCuentaYPermisos(request.getNumeroCuenta(), infoToken, request.getMonto());
        validarYConsumirPin2fa(cuenta.getCliente().getId(), request.getPin2fa());

        String trace = tieneTexto(request.getTraceValidacion())
                ? request.getTraceValidacion().trim()
                : (tieneTexto(validacion.trace) ? validacion.trace : generarTraceNumerico());

        PuntoredHttpResult resultadoProveedor = ejecutarPagoEnProveedor(request, tipoOp, trace);
        return procesarYPersistirResultadoPago(request, cuenta, tipoOp, trace, resultadoProveedor);
    }

    @Override
    public HistorialPagosResponse consultarHistorial(HistorialPagosRequest request, InfoToken infoToken) {
        boolean esAdmin = esAdmin(infoToken);
        List<TransaccionPago> transacciones = esAdmin
                ? transaccionPagoRepository.findAllByOrderByFechaRegistroDesc()
                : transaccionPagoRepository.findByCuenta_Cliente_IdOrderByFechaRegistroDesc(infoToken.getClienteId());

        String filtroCuenta = request != null && tieneTexto(request.getNumeroCuenta())
                ? request.getNumeroCuenta().trim() : null;
        String filtroEstado = request != null && tieneTexto(request.getEstado())
                ? request.getEstado().trim().toUpperCase(Locale.ROOT) : null;
        String filtroTipo = request != null && tieneTexto(request.getTipoOperacion())
                ? request.getTipoOperacion().trim().toUpperCase(Locale.ROOT) : null;
        int limite = request != null && request.getLimite() != null && request.getLimite() > 0
                ? Math.min(request.getLimite(), 200) : 100;

        List<HistorialPagoItem> items = transacciones.stream()
                .filter(tx -> filtroCuenta == null || filtroCuenta.equals(tx.getNumeroCuenta()))
                .filter(tx -> filtroEstado == null || filtroEstado.equalsIgnoreCase(tx.getEstado()))
                .filter(tx -> filtroTipo == null || filtroTipo.equalsIgnoreCase(tx.getTipoPago()))
                .limit(limite)
                .map(this::mapearHistorialItem)
                .toList();

        HistorialPagosResponse response = new HistorialPagosResponse();
        response.setCodigo(0);
        response.setMensaje("Historial de pagos consultado exitosamente");
        response.setTotal(items.size());
        response.setTransacciones(items);
        return response;
    }

    @Override
    public DetallePagoResponse consultarDetalleYReverificar(DetallePagoRequest request, InfoToken infoToken) {
        TransaccionPago tx = transaccionPagoRepository.findById(request.getIdTransaccion())
                .orElseThrow(() -> new ValidacionException(MensajeError.PUNTORED_TRANSACCION_NO_ENCONTRADA));

        Integer ownerId = tx.getCuenta() != null && tx.getCuenta().getCliente() != null
                ? tx.getCuenta().getCliente().getId() : null;
        if (!esAdmin(infoToken) && !infoToken.getClienteId().equals(ownerId)) {
            throw new ValidacionException(MensajeError.ACCESO_DENEGADO);
        }

        boolean actualizado = false;
        if (Boolean.TRUE.equals(request.getConsultarEstadoProveedor()) && PROVEEDOR_PUNTORED.equalsIgnoreCase(tx.getProveedor())) {
            actualizado = reverificarEstadoEnPuntored(tx);
        }

        DetallePagoResponse response = new DetallePagoResponse();
        response.setCodigo(0);
        response.setMensaje(actualizado
                ? "Detalle consultado y estado actualizado ante PuntoRed"
                : "Detalle de transaccion consultado exitosamente");
        response.setTransaccion(mapearHistorialItem(tx));
        response.setEstadoActualizado(actualizado);
        return response;
    }

    private ValidacionReferenciaResponse validarReferenciaLineaOPin(
            ValidarReferenciaRequest request,
            InfoToken infoToken,
            String tipoOp,
            String ref1,
            String ref2,
            String trace) {
        boolean esBetPlay = "87".equals(request.getCodigoProducto()) || "87".equals(request.getCodigoProveedor());
        boolean formatoValido = esBetPlay
                ? PATRON_REFERENCIA_GENERAL.matcher(ref1).matches()
                : PATRON_CELULAR.matcher(ref1).matches();

        if (!formatoValido) {
            return construirValidacionFallida(
                    tipoOp,
                    request.getIdConvenio(),
                    request.getCodigoProducto(),
                    ref1,
                    ref2,
                    "FORMAT_ERROR",
                    esBetPlay
                            ? "La referencia debe tener entre 4 y 50 caracteres alfanumericos sin espacios"
                            : "El numero de celular debe tener exactamente 10 digitos numericos",
                    trace
            );
        }

        BigDecimal sugerido = request.getMontoReferencia();
        boolean editable = OP_RECARGA.equals(tipoOp) && (sugerido == null || sugerido.compareTo(BigDecimal.ZERO) <= 0);
        BigDecimal min = editable ? new BigDecimal("1000.00") : sugerido;
        BigDecimal max = editable ? new BigDecimal("500000.00") : sugerido;

        ValidacionReferenciaResponse res = new ValidacionReferenciaResponse();
        res.setCodigo(0);
        res.setMensaje("Referencia validada correctamente");
        res.setReferenciaValida(true);
        res.setTipoOperacion(tipoOp);
        res.setCodigoProducto(request.getCodigoProducto());
        res.setReferencia1(ref1);
        res.setReferencia2(ref2);
        res.setCodigoRespuestaConvenio("00");
        res.setMensajeConvenio(esBetPlay
                ? "Documento / cuenta BetPlay con formato valido"
                : "Linea celular validada (10 digitos)");
        res.setMontoSugerido(sugerido);
        res.setMontoMinimo(min);
        res.setMontoMaximo(max);
        res.setMontoEditable(editable);
        res.setTrace(trace);
        res.setTokenValidacion(generarTokenValidacion(
                infoToken, tipoOp, request.getCodigoProducto(), ref1, trace, sugerido, min, max, editable));
        return res;
    }

    private ValidacionReferenciaResponse validarRecaudoV1(
            ValidarReferenciaRequest request,
            InfoToken infoToken,
            String ref1,
            String ref2,
            String trace) {
        if (!tieneTexto(request.getIdConvenio())) {
            throw new ValidacionException(MensajeError.PUNTORED_REFERENCIA_INVALIDA);
        }
        if (!PATRON_REFERENCIA_GENERAL.matcher(ref1).matches()) {
            return construirValidacionFallida(OP_RECAUDO, request.getIdConvenio(), request.getCodigoProducto(),
                    ref1, ref2, "FORMAT_ERROR", "La referencia del convenio debe contener entre 4 y 50 caracteres validos", trace);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(CAMPO_POS, properties.getPointOfSale());
        payload.put(CAMPO_TERMINAL, properties.getTerminal());
        payload.put("date", LocalDate.now(ZoneId.systemDefault()).toString());
        payload.put(CAMPO_TRACE, trace);
        payload.put(CAMPO_AGREEMENT_ID, parseEnteroSeguro(request.getIdConvenio()));
        payload.put("reference1", ref1);
        payload.put("reference2", ref2);
        payload.put("iac", parseEnteroSeguro(request.getCodigoIac()));
        payload.put(CAMPO_AMOUNT, request.getMontoReferencia() != null ? request.getMontoReferencia().intValue() : 0);

        PuntoredHttpResult result = postAutenticado("/payment-collection/check-agreement", payload);
        if (result.esEstadoIncierto()) {
            throw new ValidacionException(MensajeError.PUNTORED_TIMEOUT);
        }

        JsonNode data = result.body() != null ? result.body().path(CAMPO_DATA) : null;
        String agMsg = data != null ? textoOpcional(data.path("agreementMessage")) : null;
        String agCode = data != null ? textoOpcional(data.path("agreementCode")) : null;

        if (!result.esTransaccionExitosa()) {
            String detalle = tieneTexto(agMsg) ? agMsg : result.obtenerMensaje();
            return construirValidacionFallida(OP_RECAUDO, request.getIdConvenio(), request.getCodigoProducto(),
                    ref1, ref2, tieneTexto(agCode) ? agCode : result.obtenerCodigo(), detalle, trace);
        }

        BigDecimal amount = parseDecimal(data.path(CAMPO_AMOUNT));
        String requiresAmount = textoOpcional(data.path("requiresAmount"));
        boolean editable = "1".equals(requiresAmount) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;

        Map<String, Object> aval = new LinkedHashMap<>();
        aval.put("aditionalValue54Eco", textoOpcional(data.path("aditionalValue54Eco")));
        aval.put("aditionalValue62Eco", textoOpcional(data.path("aditionalValue62Eco")));
        aval.put("accountDataAcredited", textoOpcional(data.path("accountDataAcredited")));
        aval.put("additionalCompanyCode", textoOpcional(data.path("additionalCompanyCode")));

        ValidacionReferenciaResponse res = new ValidacionReferenciaResponse();
        res.setCodigo(0);
        res.setMensaje("Referencia de convenio validada ante PuntoRed");
        res.setReferenciaValida(true);
        res.setTipoOperacion(OP_RECAUDO);
        res.setIdConvenio(request.getIdConvenio());
        res.setCodigoProducto(request.getCodigoProducto());
        res.setReferencia1(ref1);
        res.setReferencia2(ref2);
        res.setCodigoRespuestaConvenio(tieneTexto(agCode) ? agCode : "00");
        res.setMensajeConvenio(tieneTexto(agMsg) ? agMsg : "Referencia habilitada para pago");
        res.setMontoSugerido(amount);
        res.setMontoEditable(editable);
        res.setTrace(trace);
        res.setCamposAdicionalesAval(aval);
        res.setTokenValidacion(generarTokenValidacion(
                infoToken, OP_RECAUDO, request.getIdConvenio(), ref1, trace, amount, null, null, editable));
        return res;
    }

    private ValidacionReferenciaResponse validarRecaudoV2(
            ValidarReferenciaRequest request,
            InfoToken infoToken,
            String ref1,
            String trace) {
        if (!tieneTexto(request.getIdConvenio())) {
            throw new ValidacionException(MensajeError.PUNTORED_REFERENCIA_INVALIDA);
        }
        if (!PATRON_REFERENCIA_GENERAL.matcher(ref1).matches()) {
            return construirValidacionFallida(OP_RECAUDO_V2, request.getIdConvenio(), request.getCodigoProducto(),
                    ref1, null, "FORMAT_ERROR", "La referencia debe contener entre 4 y 50 caracteres validos", trace);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(CAMPO_POS, properties.getPointOfSale());
        payload.put(CAMPO_AGREEMENT_ID, parseEnteroSeguro(request.getIdConvenio()));
        payload.put("reference", ref1);
        if (tieneTexto(request.getCodigoProducto())) {
            payload.put(CAMPO_PRODUCT_CODE, request.getCodigoProducto());
        }
        payload.put(CAMPO_TRACE, trace);
        payload.put(CAMPO_TERMINAL, properties.getTerminal());
        if (tieneTexto(request.getTipoCuenta())) {
            payload.put("accountType", request.getTipoCuenta());
        }
        if (request.getTipoReferencia() != null) {
            payload.put("typeReference", request.getTipoReferencia());
        }

        PuntoredHttpResult result = postAutenticado("/payment-collection/v2/check-agreement", payload);
        if (result.esEstadoIncierto()) {
            throw new ValidacionException(MensajeError.PUNTORED_TIMEOUT);
        }
        if (!result.esTransaccionExitosa() || result.body() == null) {
            return construirValidacionFallida(OP_RECAUDO_V2, request.getIdConvenio(), request.getCodigoProducto(),
                    ref1, null, result.obtenerCodigo(), result.obtenerMensaje(), trace);
        }

        JsonNode data = result.body().path(CAMPO_DATA);
        List<OpcionMontoConvenio> opciones = extraerOpcionesV2(data);
        OpcionMontoConvenio primera = opciones.isEmpty() ? null : opciones.get(0);
        BigDecimal sug = primera != null ? primera.getMonto() : null;
        BigDecimal min = primera != null ? primera.getMontoMinimo() : null;
        BigDecimal max = primera != null ? primera.getMontoMaximo() : null;
        boolean editable = primera == null || !Integer.valueOf(1).equals(primera.getTipoMonto());

        ValidacionReferenciaResponse res = new ValidacionReferenciaResponse();
        res.setCodigo(0);
        res.setMensaje("Convenio V2 validado exitosamente ante PuntoRed");
        res.setReferenciaValida(true);
        res.setTipoOperacion(OP_RECAUDO_V2);
        res.setIdConvenio(request.getIdConvenio());
        res.setCodigoProducto(request.getCodigoProducto());
        res.setReferencia1(ref1);
        res.setCodigoRespuestaConvenio("00");
        res.setMensajeConvenio(textoOpcional(data.path("supplierMessage")) != null
                ? textoOpcional(data.path("supplierMessage"))
                : "Referencia habilitada para recaudo");
        res.setMontoSugerido(sug);
        res.setMontoMinimo(min);
        res.setMontoMaximo(max);
        res.setMontoEditable(editable);
        res.setTrace(trace);
        res.setOpciones(opciones);
        res.setTokenValidacion(generarTokenValidacion(
                infoToken, OP_RECAUDO_V2, request.getIdConvenio(), ref1, trace, sug, min, max, editable));
        return res;
    }

    private List<OpcionMontoConvenio> extraerOpcionesV2(JsonNode data) {
        JsonNode lista = data.isArray() ? data : data.path("agreements");
        if (!lista.isArray()) {
            return Collections.emptyList();
        }
        List<OpcionMontoConvenio> opciones = new ArrayList<>();
        for (JsonNode item : lista) {
            OpcionMontoConvenio op = new OpcionMontoConvenio();
            op.setCodigoRegistro(textoOpcional(item.path("codeRecord")));
            op.setDescripcionRegistro(textoOpcional(item.path("descriptionRecord")));
            op.setMonto(parseDecimal(item.path(CAMPO_AMOUNT)));
            op.setTipoMonto(item.hasNonNull("typeAmount") ? item.get("typeAmount").asInt() : 0);
            op.setMontoMinimo(parseDecimal(item.path("minAmount")));
            op.setMontoMaximo(parseDecimal(item.path("maxAmount")));
            op.setRequiereMonto(!Integer.valueOf(1).equals(op.getTipoMonto()));
            opciones.add(op);
        }
        return opciones;
    }

    private PuntoredHttpResult ejecutarPagoEnProveedor(
            PagoPuntoredRequest request,
            String tipoOp,
            String trace) {
        return switch (tipoOp) {
            case OP_RECARGA -> postAutenticado("/recharge/buy", payloadRecarga(request, trace));
            case OP_PAQUETE -> postAutenticado("/package/buy", payloadPaquete(request, trace));
            case OP_RECAUDO -> postAutenticado("/payment-collection/manual-payment-collection", payloadRecaudoV1(request, trace));
            case OP_RECAUDO_V2 -> postAutenticado("/payment-collection/v2/manual-payment-collection", payloadRecaudoV2(request, trace));
            case OP_PIN -> postAutenticado("/pin/buy", payloadPin(request, trace));
            default -> throw new ValidacionException(MensajeError.DATOS_NO_VALIDOS);
        };
    }

    private Map<String, Object> payloadRecarga(PagoPuntoredRequest req, String trace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CAMPO_POS, properties.getPointOfSale());
        body.put(CAMPO_TERMINAL, properties.getTerminal());
        body.put(CAMPO_TX_PASSWORD, properties.getTransactionalPassword());
        body.put("number", req.getReferencia1().trim());
        body.put(CAMPO_AMOUNT, req.getMonto().intValue());
        body.put(CAMPO_TRACE, trace);
        body.put(CAMPO_PRODUCT_CODE, tieneTexto(req.getCodigoProducto()) ? req.getCodigoProducto() : req.getCodigoProveedor());
        return body;
    }

    private Map<String, Object> payloadPaquete(PagoPuntoredRequest req, String trace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CAMPO_POS, properties.getPointOfSale());
        body.put(CAMPO_TERMINAL, properties.getTerminal());
        body.put(CAMPO_TX_PASSWORD, properties.getTransactionalPassword());
        body.put(CAMPO_TRACE, trace);
        body.put("supplierCode", req.getCodigoProveedor());
        body.put("categoryCode", req.getCodigoCategoria());
        body.put(CAMPO_PRODUCT_CODE, req.getCodigoProducto());
        body.put("sku", req.getSku());
        body.put(CAMPO_AMOUNT, req.getMonto().intValue());
        body.put("number", req.getReferencia1().trim());
        return body;
    }

    private Map<String, Object> payloadRecaudoV1(PagoPuntoredRequest req, String trace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CAMPO_POS, properties.getPointOfSale());
        body.put("h2hPointOfSale", String.valueOf(properties.getPointOfSale()));
        body.put(CAMPO_TERMINAL, properties.getTerminal());
        body.put(CAMPO_TX_PASSWORD, properties.getTransactionalPassword());
        body.put("date", LocalDate.now(ZoneId.systemDefault()).toString());
        body.put(CAMPO_TRACE, trace);
        body.put(CAMPO_AGREEMENT_ID, parseEnteroSeguro(req.getIdConvenio()));
        body.put("reference1", req.getReferencia1().trim());
        body.put("reference2", req.getReferencia2() != null ? req.getReferencia2().trim() : "");
        body.put(CAMPO_AMOUNT, req.getMonto().intValue());
        body.put("amountQuery", req.getMontoQuery() != null ? req.getMontoQuery().intValue() : req.getMonto().intValue());
        body.put("iac", parseEnteroSeguro(req.getCodigoIac()));
        body.put("ip", "127.0.0.1");
        agregarOpcional(body, "aditionalValue54Eco", req.getAditionalValue54Eco());
        agregarOpcional(body, "aditionalValue62Eco", req.getAditionalValue62Eco());
        agregarOpcional(body, "accountDataAcredited", req.getAccountDataAcredited());
        agregarOpcional(body, "additionalCompanyCode", req.getAdditionalCompanyCode());
        return body;
    }

    private Map<String, Object> payloadRecaudoV2(PagoPuntoredRequest req, String trace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CAMPO_POS, properties.getPointOfSale());
        body.put("h2hPointOfSale", String.valueOf(properties.getPointOfSale()));
        body.put(CAMPO_TERMINAL, properties.getTerminal());
        body.put(CAMPO_TX_PASSWORD, properties.getTransactionalPassword());
        body.put(CAMPO_AGREEMENT_ID, parseEnteroSeguro(req.getIdConvenio()));
        body.put("reference", req.getReferencia1().trim());
        body.put("codeRecord", tieneTexto(req.getCodigoRegistro()) ? req.getCodigoRegistro() : req.getReferencia1().trim());
        body.put(CAMPO_PRODUCT_CODE, req.getCodigoProducto());
        body.put(CAMPO_AMOUNT, req.getMonto().intValue());
        body.put(CAMPO_TRACE, trace);
        agregarOpcional(body, "accountType", req.getTipoCuenta());
        if (req.getTipoReferencia() != null) {
            body.put("typeReference", String.valueOf(req.getTipoReferencia()));
        }
        return body;
    }

    private Map<String, Object> payloadPin(PagoPuntoredRequest req, String trace) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CAMPO_POS, properties.getPointOfSale());
        body.put(CAMPO_TERMINAL, properties.getTerminal());
        body.put(CAMPO_TX_PASSWORD, properties.getTransactionalPassword());
        body.put("pinId", req.getIdPin());
        body.put(CAMPO_PRODUCT_CODE, req.getCodigoProducto());
        body.put("subCategoryId", req.getIdSubcategoriaPin());
        body.put("supplierCode", req.getCodigoProveedor());
        body.put(CAMPO_TRACE, trace);
        body.put(CAMPO_AMOUNT, req.getMonto().intValue());
        body.put("sku", req.getSku());
        body.put("number", req.getReferencia1().trim());
        return body;
    }

    private PagoPuntoredResponse procesarYPersistirResultadoPago(
            PagoPuntoredRequest request,
            Cuenta cuenta,
            String tipoOp,
            String trace,
            PuntoredHttpResult result) {
        BigDecimal monto = limpiarMonto(request.getMonto());
        BigDecimal saldoAnterior = limpiarMonto(cuenta.getSaldo());
        BigDecimal saldoNuevo = saldoAnterior;

        JsonNode data = result.body() != null ? result.body().path(CAMPO_DATA) : null;
        String idTxProveedor = data != null ? textoOpcional(data.path("transactionId")) : null;
        String authCode = data != null ? textoOpcional(data.path("authorizationCode")) : null;
        String pinContenido = data != null ? textoOpcional(data.path("pin")) : null;
        String codigoProveedor = result.obtenerCodigo();
        String mensajeProveedor = data != null && tieneTexto(textoOpcional(data.path("message")))
                ? textoOpcional(data.path("message"))
                : result.obtenerMensaje();

        String estado;
        boolean confirmada = false;
        if (result.esTransaccionExitosa() && tieneTexto(idTxProveedor)) {
            estado = ESTADO_EXITOSA;
            confirmada = true;
            saldoNuevo = limpiarMonto(saldoAnterior.subtract(monto));
            cuenta.setSaldo(saldoNuevo);
            cuentaRepository.save(cuenta);
        } else if (result.esEstadoIncierto() || result.esTransaccionExitosa()) {
            estado = result.timedOut() ? ESTADO_NO_CONFIRMADA : ESTADO_PENDIENTE;
        } else {
            estado = ESTADO_RECHAZADA;
        }

        TicketData ticket = extraerTicket(data, request.getLogoUrl());
        TransaccionPago tx = construirEntidadTransaccion(
                request, cuenta, tipoOp, trace, monto, idTxProveedor, authCode,
                codigoProveedor, mensajeProveedor, estado, ticket
        );
        TransaccionPago guardada = transaccionPagoRepository.save(tx);

        PagoPuntoredResponse response = new PagoPuntoredResponse();
        response.setCodigo(confirmada ? 0 : (ESTADO_RECHAZADA.equals(estado) ? 29 : 26));
        response.setMensaje(confirmada
                ? "Pago confirmado y aplicado exitosamente"
                : (ESTADO_RECHAZADA.equals(estado)
                        ? "Pago rechazado por el proveedor: " + mensajeProveedor
                        : "Operacion pendiente o no confirmada por intermitencia de red; verifica su estado en el historial"));
        response.setEstado(estado);
        response.setTransaccionConfirmada(confirmada);
        response.setIdTransaccionLocal(guardada.getId());
        response.setIdTransaccionProveedor(idTxProveedor);
        response.setCodigoAutorizacion(authCode);
        response.setTrace(trace);
        response.setPinContenido(pinContenido);
        response.setCodigoRespuestaProveedor(codigoProveedor);
        response.setMensajeProveedor(mensajeProveedor);
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        response.setNombreServicio(request.getNombreServicio());
        response.setNombreProducto(request.getNombreProducto());
        response.setCategoria(request.getCategoria());
        response.setLogoUrl(request.getLogoUrl());
        response.setReferencia1(request.getReferencia1().trim());
        response.setReferencia2(request.getReferencia2());
        response.setMonto(monto);
        response.setSaldoAnterior(saldoAnterior);
        response.setSaldoNuevo(saldoNuevo);
        response.setTicket(ticket);
        response.setFechaRegistro(guardada.getFechaRegistro());
        return response;
    }

    private boolean reverificarEstadoEnPuntored(TransaccionPago tx) {
        PuntoredHttpResult statusRes;
        if (tieneTexto(tx.getIdTxGestoPago())) {
            statusRes = postAutenticado("/query/transaction-status", Map.of(
                    CAMPO_POS, properties.getPointOfSale(),
                    "transactionId", parseEnteroSeguro(tx.getIdTxGestoPago())
            ));
        } else if (tieneTexto(tx.getTrace())) {
            statusRes = postAutenticado("/query/transaction-status-trace", Map.of(
                    CAMPO_POS, properties.getPointOfSale(),
                    CAMPO_TRACE, parseEnteroSeguro(tx.getTrace())
            ));
        } else {
            return false;
        }

        if (!statusRes.esTransaccionExitosa() || statusRes.body() == null) {
            return false;
        }

        JsonNode dataNode = statusRes.body().path(CAMPO_DATA);
        JsonNode item = dataNode.isArray() && !dataNode.isEmpty() ? dataNode.get(0) : dataNode;
        String statusRemoto = textoOpcional(item.path("status"));
        if (!tieneTexto(statusRemoto)) {
            return false;
        }

        String nuevoEstado = interpretarEstadoRemoto(statusRemoto);
        if (nuevoEstado.equalsIgnoreCase(tx.getEstado())) {
            return false;
        }

        String idTxRemoto = textoOpcional(item.path("transactionId"));
        if (tieneTexto(idTxRemoto)) {
            tx.setIdTxGestoPago(idTxRemoto);
        }
        if (ESTADO_EXITOSA.equals(nuevoEstado) && !ESTADO_EXITOSA.equals(tx.getEstado())) {
            Cuenta cuenta = tx.getCuenta();
            BigDecimal saldo = limpiarMonto(cuenta.getSaldo());
            if (saldo.compareTo(tx.getMonto()) < 0) {
                throw new ValidacionException(MensajeError.SALDO_INSUFICIENTE);
            }
            cuenta.setSaldo(limpiarMonto(saldo.subtract(tx.getMonto())));
            cuentaRepository.save(cuenta);
            cargarTicketRemoto(tx);
        }
        tx.setEstado(nuevoEstado);
        tx.setMensajeRespuesta("Estado actualizado desde PuntoRed: " + statusRemoto);
        transaccionPagoRepository.save(tx);
        return true;
    }

    private void cargarTicketRemoto(TransaccionPago tx) {
        if (!tieneTexto(tx.getIdTxGestoPago())) {
            return;
        }
        PuntoredHttpResult ticketRes = postAutenticado("/query/transaction-ticket", Map.of(
                CAMPO_POS, properties.getPointOfSale(),
                "transactionId", parseEnteroSeguro(tx.getIdTxGestoPago())
        ));
        if (ticketRes.esTransaccionExitosa() && ticketRes.body() != null) {
            JsonNode data = ticketRes.body().path(CAMPO_DATA);
            TicketData ticket = extraerTicket(data, tx.getLogoUrl());
            if (ticket != null) {
                tx.setTicketJson(serializarTicket(ticket));
            }
            String auth = textoOpcional(data.path("authorizationCode"));
            if (tieneTexto(auth)) {
                tx.setNumAutorizacion(auth);
            }
        }
    }

    private String interpretarEstadoRemoto(String statusRemoto) {
        String norm = statusRemoto.trim().toUpperCase(Locale.ROOT);
        if (norm.contains("APROB") || norm.contains("EXIT") || "OK".equals(norm)) {
            return ESTADO_EXITOSA;
        }
        if (norm.contains("RECHAZ") || norm.contains("DECLIN") || norm.contains("FALL") || norm.contains("ERROR")) {
            return ESTADO_RECHAZADA;
        }
        return ESTADO_PENDIENTE;
    }

    private TransaccionPago construirEntidadTransaccion(
            PagoPuntoredRequest req,
            Cuenta cuenta,
            String tipoOp,
            String trace,
            BigDecimal monto,
            String idTxProveedor,
            String authCode,
            String codigoResp,
            String mensajeResp,
            String estado,
            TicketData ticket) {
        TransaccionPago tx = new TransaccionPago();
        tx.setCuenta(cuenta);
        tx.setNumeroCuenta(cuenta.getNumeroCuenta());
        tx.setIdServicio(parseEnteroSeguro(tieneTexto(req.getIdConvenio()) ? req.getIdConvenio() : req.getCodigoProveedor()));
        tx.setIdProducto(parseEnteroSeguro(tieneTexto(req.getCodigoProducto()) ? req.getCodigoProducto() : req.getIdPin()));
        tx.setTipoPago(tipoOp);
        tx.setTelefonoReferencia(req.getReferencia1().trim());
        tx.setReferenciaSecundaria(req.getReferencia2());
        tx.setMonto(monto);
        tx.setUpc(tieneTexto(req.getSku()) ? req.getSku() : "0");
        tx.setUnidad(tieneTexto(properties.getTerminal()) ? properties.getTerminal() : "H2H");
        tx.setHoraLocal(LocalDateTime.now(ZoneId.systemDefault()).format(FORMATO_HORA));
        tx.setSenTxCifrado("PUNTORED_H2H_V2:" + trace);
        tx.setIdTxGestoPago(idTxProveedor);
        tx.setNumAutorizacion(authCode);
        tx.setCodigoRespuesta(tieneTexto(codigoResp) ? codigoResp : "N/A");
        tx.setMensajeRespuesta(tieneTexto(mensajeResp) ? mensajeResp : estado);
        tx.setNombreServicio(req.getNombreServicio());
        tx.setNombreProducto(req.getNombreProducto());
        tx.setCategoria(req.getCategoria());
        tx.setLogoUrl(req.getLogoUrl());
        tx.setTrace(trace);
        tx.setEstado(estado);
        tx.setProveedor(PROVEEDOR_PUNTORED);
        tx.setCodigoProductoExterno(req.getCodigoProducto());
        tx.setCodigoConvenioExterno(req.getIdConvenio());
        tx.setTicketJson(serializarTicket(ticket));
        return tx;
    }

    private TokenValidacionPayload verificarTokenValidacion(
            PagoPuntoredRequest request,
            InfoToken infoToken,
            String tipoOp) {
        try {
            String plano = cryptoUtil.descifrar(request.getTokenValidacion());
            TokenValidacionPayload payload = objectMapper.readValue(plano, TokenValidacionPayload.class);
            if (payload.expiraEpoch <= Instant.now().getEpochSecond()) {
                throw new ValidacionException(MensajeError.PUNTORED_REFERENCIA_NO_VALIDADA);
            }
            if (!esAdmin(infoToken) && !infoToken.getClienteId().equals(payload.clienteId)) {
                throw new ValidacionException(MensajeError.ACCESO_DENEGADO);
            }
            if (!tipoOp.equalsIgnoreCase(payload.tipoOperacion)
                    || !request.getReferencia1().trim().equals(payload.referencia1)) {
                throw new ValidacionException(MensajeError.PUNTORED_REFERENCIA_NO_VALIDADA);
            }
            validarReglasMonto(request.getMonto(), payload);
            return payload;
        } catch (ValidacionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidacionException(MensajeError.PUNTORED_REFERENCIA_NO_VALIDADA);
        }
    }

    private void validarReglasMonto(BigDecimal monto, TokenValidacionPayload payload) {
        if (Boolean.FALSE.equals(payload.montoEditable)
                && payload.montoSugerido != null
                && payload.montoSugerido.compareTo(BigDecimal.ZERO) > 0
                && monto.compareTo(payload.montoSugerido) != 0) {
            throw new ValidacionException(
                    MensajeError.PUNTORED_MONTO_INVALIDO,
                    "Debe corresponder exactamente a $" + payload.montoSugerido
            );
        }
        if (payload.montoMinimo != null
                && payload.montoMinimo.compareTo(BigDecimal.ZERO) > 0
                && monto.compareTo(payload.montoMinimo) < 0) {
            throw new ValidacionException(
                    MensajeError.PUNTORED_MONTO_INVALIDO,
                    "Monto minimo permitido: $" + payload.montoMinimo
            );
        }
        if (payload.montoMaximo != null
                && payload.montoMaximo.compareTo(BigDecimal.ZERO) > 0
                && monto.compareTo(payload.montoMaximo) > 0) {
            throw new ValidacionException(
                    MensajeError.PUNTORED_MONTO_INVALIDO,
                    "Monto maximo permitido: $" + payload.montoMaximo
            );
        }
    }

    private Cuenta validarCuentaYPermisos(String numeroCuenta, InfoToken infoToken, BigDecimal monto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ValidacionException(MensajeError.CUENTA_NO_ENCONTRADA));
        Cliente cliente = cuenta.getCliente();
        if (cliente == null) {
            throw new ValidacionException(MensajeError.CLIENTE_NO_ENCONTRADO);
        }
        if (!esAdmin(infoToken) && !cliente.getId().equals(infoToken.getClienteId())) {
            throw new ValidacionException(MensajeError.ACCESO_DENEGADO);
        }
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ValidacionException(MensajeError.CLIENTE_INACTIVO_COMPRA);
        }
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ValidacionException(MensajeError.CUENTA_INACTIVA_COMPRA);
        }
        if (limpiarMonto(cuenta.getSaldo()).compareTo(limpiarMonto(monto)) < 0) {
            throw new ValidacionException(MensajeError.SALDO_INSUFICIENTE);
        }
        return cuenta;
    }

    private void validarYConsumirPin2fa(Integer clienteId, String pinIngresado) {
        Usuario usuario = usuarioRepository.findByCliente_Id(clienteId)
                .orElseThrow(() -> new ValidacionException(MensajeError.CLIENTE_NO_ENCONTRADO));

        if (usuario.getPin2faHash() == null || usuario.getPin2faExpiracion() == null) {
            throw new ValidacionException(MensajeError.PIN_2FA_NO_SOLICITADO);
        }
        if (LocalDateTime.now(ZoneId.systemDefault()).isAfter(usuario.getPin2faExpiracion())) {
            usuario.setPin2faHash(null);
            usuario.setPin2faExpiracion(null);
            usuarioRepository.save(usuario);
            throw new ValidacionException(MensajeError.PIN_2FA_EXPIRADO);
        }
        if (!hashUtil.sha256(pinIngresado).equals(usuario.getPin2faHash())) {
            throw new ValidacionException(MensajeError.PIN_2FA_INVALIDO);
        }
        usuario.setPin2faHash(null);
        usuario.setPin2faExpiracion(null);
        usuarioRepository.save(usuario);
    }

    private String generarTokenValidacion(
            InfoToken infoToken,
            String tipoOp,
            String objetivoId,
            String ref1,
            String trace,
            BigDecimal sugerido,
            BigDecimal min,
            BigDecimal max,
            boolean editable) {
        try {
            TokenValidacionPayload payload = new TokenValidacionPayload();
            payload.clienteId = infoToken.getClienteId();
            payload.tipoOperacion = tipoOp;
            payload.objetivoId = objetivoId;
            payload.referencia1 = ref1;
            payload.trace = trace;
            payload.montoSugerido = sugerido;
            payload.montoMinimo = min;
            payload.montoMaximo = max;
            payload.montoEditable = editable;
            payload.expiraEpoch = Instant.now().getEpochSecond() + EXPIRACION_TOKEN_VALIDACION_SEGUNDOS;
            return cryptoUtil.cifrar(objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar token de validacion de referencia", ex);
        }
    }

    private ValidacionReferenciaResponse construirValidacionFallida(
            String tipoOp,
            String idConvenio,
            String codigoProducto,
            String ref1,
            String ref2,
            String codigoConvenio,
            String mensajeDetalle,
            String trace) {
        ValidacionReferenciaResponse res = new ValidacionReferenciaResponse();
        res.setCodigo(28);
        res.setMensaje(tieneTexto(mensajeDetalle) ? mensajeDetalle : "Referencia no valida para pago");
        res.setReferenciaValida(false);
        res.setTokenValidacion(null);
        res.setTipoOperacion(tipoOp);
        res.setIdConvenio(idConvenio);
        res.setCodigoProducto(codigoProducto);
        res.setReferencia1(ref1);
        res.setReferencia2(ref2);
        res.setCodigoRespuestaConvenio(codigoConvenio);
        res.setMensajeConvenio(res.getMensaje());
        res.setMontoEditable(false);
        res.setTrace(trace);
        return res;
    }

    private HistorialPagoItem mapearHistorialItem(TransaccionPago tx) {
        HistorialPagoItem item = new HistorialPagoItem();
        item.setId(tx.getId());
        item.setNumeroCuenta(tx.getNumeroCuenta());
        item.setProveedor(tieneTexto(tx.getProveedor()) ? tx.getProveedor() : "GESTOPAGO");
        item.setTipoOperacion(tx.getTipoPago());
        item.setNombreServicio(tieneTexto(tx.getNombreServicio()) ? tx.getNombreServicio() : "Servicio #" + tx.getIdServicio());
        item.setNombreProducto(tieneTexto(tx.getNombreProducto()) ? tx.getNombreProducto() : "Producto #" + tx.getIdProducto());
        item.setCategoria(tx.getCategoria());
        item.setLogoUrl(tx.getLogoUrl());
        item.setReferencia1(tx.getTelefonoReferencia());
        item.setReferencia2(tx.getReferenciaSecundaria());
        item.setMonto(tx.getMonto());
        item.setIdTransaccionProveedor(tx.getIdTxGestoPago());
        item.setCodigoAutorizacion(tx.getNumAutorizacion());
        item.setTrace(tx.getTrace());
        item.setCodigoRespuesta(tx.getCodigoRespuesta());
        item.setMensajeRespuesta(tx.getMensajeRespuesta());
        item.setEstado(tieneTexto(tx.getEstado()) ? tx.getEstado() : ESTADO_EXITOSA);
        item.setTicket(deserializarTicket(tx.getTicketJson()));
        item.setFechaRegistro(tx.getFechaRegistro());
        return item;
    }

    private TicketData extraerTicket(JsonNode data, String fallbackLogo) {
        if (data == null || data.isMissingNode() || !data.has("ticket") || data.path("ticket").isNull()) {
            return null;
        }
        JsonNode t = data.path("ticket");
        TicketData ticket = new TicketData();
        ticket.setEncabezado(textoOpcional(t.path("header")));
        ticket.setTitulo(textoOpcional(t.path("title")));
        ticket.setPiePagina(textoOpcional(t.path("footer")));
        String logoTicket = textoOpcional(t.path("logo"));
        ticket.setLogoUrl(tieneTexto(logoTicket) ? logoTicket : fallbackLogo);
        if (t.has("body") && t.get("body").isObject()) {
            ticket.setCuerpo(objectMapper.convertValue(
                    t.get("body"),
                    new TypeReference<Map<String, Object>>() {}
            ));
        }
        return ticket;
    }

    private String serializarTicket(TicketData ticket) {
        if (ticket == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ticket);
        } catch (Exception ex) {
            return null;
        }
    }

    private TicketData deserializarTicket(String json) {
        if (!tieneTexto(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TicketData.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private PuntoredHttpResult postAutenticado(String path, Object payload) {
        String token = tokenService.obtenerTokenValido();
        PuntoredHttpResult result = apiClient.postJson(path, payload, token);
        if (result.httpStatus() == 401) {
            tokenService.invalidarToken();
            String nuevo = tokenService.renovarToken();
            result = apiClient.postJson(path, payload, nuevo);
        }
        return result;
    }

    private String generarTraceNumerico() {
        long base = Instant.now().toEpochMilli() % 1_000_000_000L;
        int seq = secuenciaTrace.updateAndGet(v -> v >= 99 ? 10 : v + 1);
        int rand = random.nextInt(9);
        return String.format("%09d%02d%d", base, seq, rand);
    }

    private boolean esAdmin(InfoToken infoToken) {
        return infoToken != null && infoToken.getRol() != null && infoToken.getRol().equals(Roles.ADMIN);
    }

    private BigDecimal limpiarMonto(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            String raw = node.asText("").trim();
            return raw.isEmpty() ? null : new BigDecimal(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseEnteroSeguro(String raw) {
        if (!tieneTexto(raw)) {
            return 0;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private void agregarOpcional(Map<String, Object> target, String key, String value) {
        if (tieneTexto(value)) {
            target.put(key, value.trim());
        }
    }

    private String normalizarOperacion(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String textoOpcional(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return (text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())) ? null : text.trim();
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }

    public static class TokenValidacionPayload {
        public Integer clienteId;
        public String tipoOperacion;
        public String objetivoId;
        public String referencia1;
        public String trace;
        public BigDecimal montoSugerido;
        public BigDecimal montoMinimo;
        public BigDecimal montoMaximo;
        public Boolean montoEditable;
        public long expiraEpoch;
    }
}
