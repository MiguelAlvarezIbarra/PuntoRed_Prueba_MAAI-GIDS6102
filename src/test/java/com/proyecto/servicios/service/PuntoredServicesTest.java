package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.PuntoredApiClient;
import com.proyecto.servicios.client.PuntoredHttpResult;
import com.proyecto.servicios.config.PuntoredProperties;
import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.controller.CatalogoController;
import com.proyecto.servicios.controller.CuentaUsuarioController;
import com.proyecto.servicios.controller.PuntoredPagoController;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.TransaccionPago;
import com.proyecto.servicios.entity.sf.Usuario;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.puntored.CatalogoBusquedaRequest;
import com.proyecto.servicios.model.puntored.CatalogoServiciosResponse;
import com.proyecto.servicios.model.puntored.DetallePagoRequest;
import com.proyecto.servicios.model.puntored.DetallePagoResponse;
import com.proyecto.servicios.model.puntored.HistorialPagosRequest;
import com.proyecto.servicios.model.puntored.HistorialPagosResponse;
import com.proyecto.servicios.model.puntored.PagoPuntoredRequest;
import com.proyecto.servicios.model.puntored.PagoPuntoredResponse;
import com.proyecto.servicios.model.puntored.ValidacionReferenciaResponse;
import com.proyecto.servicios.model.puntored.ValidarReferenciaRequest;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.UsuarioRepository;
import com.proyecto.servicios.service.Impl.PuntoredCatalogoServiceImpl;
import com.proyecto.servicios.service.Impl.PuntoredPagoServiceImpl;
import com.proyecto.servicios.service.Impl.PuntoredTokenServiceImpl;
import com.proyecto.servicios.util.CryptoUtil;
import com.proyecto.servicios.util.HashUtil;
import com.proyecto.servicios.util.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuntoredServicesTest {

    private static final String TEST_AES_KEY = "u/EC0kzidACsyNCTbbea/7Wj1t86k5i3Qrhu5HYxCMM=";
    private static final String CUENTA_18 = "123456789012345678";
    private static final String PIN_2FA = "482910";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CryptoUtil cryptoUtil = new CryptoUtil(TEST_AES_KEY);
    private final HashUtil hashUtil = new HashUtil();

    private PuntoredProperties properties;

    @Mock
    private PuntoredApiClient apiClient;

    @Mock
    private PuntoredTokenService tokenService;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PuntoredCatalogoServiceImpl catalogoService;
    private PuntoredPagoServiceImpl pagoService;

    @BeforeEach
    void setUp() {
        properties = new PuntoredProperties();
        properties.setUrl("https://services.preprodcxr.co/puntored/api/v1");
        properties.setCommerce("539998");
        properties.setPointOfSale(271826);
        properties.setTerminal("383149");
        properties.setUsername("PRUEBACONEXRED");
        properties.setPassword("hash-password");
        properties.setTransactionalPassword("123456");

        lenient().when(tokenService.obtenerTokenValido()).thenReturn("puntored-jwt");
        lenient().when(tokenService.renovarToken()).thenReturn("puntored-jwt-refreshed");

        catalogoService = new PuntoredCatalogoServiceImpl(
                tokenService, apiClient, properties, objectMapper, redisTemplate, 3600L);
        pagoService = new PuntoredPagoServiceImpl(
                tokenService,
                apiClient,
                properties,
                cuentaRepository,
                usuarioRepository,
                transaccionPagoRepository,
                cryptoUtil,
                hashUtil,
                objectMapper
        );
    }

    @Test
    void apiClient_manejaExito404YTimeoutCorrectamente() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        PuntoredApiClient clientReal = new PuntoredApiClient(properties, objectMapper, restTemplate);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"state\":true,\"code\":\"00\",\"message\":\"OK\",\"data\":{\"balance\":\"100\"}}"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        "{\"state\":false,\"code\":\"HE04\",\"message\":\"Factura invalida\"}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("timed out")));

        PuntoredHttpResult ok = clientReal.postJson("/query/balance-commerce", java.util.Map.of(), "tok");
        assertThat(ok.esTransaccionExitosa()).isTrue();
        assertThat(ok.obtenerCodigo()).isEqualTo("00");

        PuntoredHttpResult notFound = clientReal.postJson("/payment-collection/check-agreement", java.util.Map.of(), "tok");
        assertThat(notFound.esTransaccionExitosa()).isFalse();
        assertThat(notFound.esEstadoIncierto()).isFalse();
        assertThat(notFound.obtenerCodigo()).isEqualTo("HE04");

        PuntoredHttpResult timeout = clientReal.postJson("/recharge/buy", java.util.Map.of(), "tok");
        assertThat(timeout.esEstadoIncierto()).isTrue();
        assertThat(timeout.timedOut()).isTrue();
        assertThat(timeout.obtenerCodigo()).isEqualTo("TIMEOUT");
    }

    @Test
    void tokenService_cacheaYRenuevaTokenCorrectamente() throws Exception {
        PuntoredTokenServiceImpl impl = new PuntoredTokenServiceImpl(apiClient, properties);
        when(apiClient.postJson(eq("/auth"), any(), eq(null)))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"message\":\"OK\",\"data\":{\"token\":\"tk-1\",\"expiration\":\"2099-01-01T00:00:00Z\"}}"));

        assertThat(impl.obtenerTokenValido()).isEqualTo("tk-1");
        assertThat(impl.obtenerTokenValido()).isEqualTo("tk-1");
        impl.invalidarToken();
        assertThat(impl.renovarToken()).isEqualTo("tk-1");

        PuntoredProperties vacias = new PuntoredProperties();
        PuntoredTokenServiceImpl sinConfig = new PuntoredTokenServiceImpl(apiClient, vacias);
        sinConfig.renovarPeriodicamente();
        assertThatThrownBy(sinConfig::obtenerTokenValido)
                .isInstanceOf(ValidacionException.class)
                .extracting("codigo").isEqualTo(MensajeError.PUNTORED_CONFIG_FALTANTE.getCodigo());
    }

    @Test
    void catalogoService_cargaModulosYFiltraConLogosReales() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        when(apiClient.postJson(eq("/query/balance-commerce"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"balance\":\"3626567.87\"}}"));
        when(apiClient.postJson(eq("/recharge/find-suppliers"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"suppliers\":[{\"supplierCode\":\"737\",\"supplierDescription\":\"CLARO\",\"supplierLogo\":\"https://cdn.example/claro.webp\"}]}}"));
        when(apiClient.postJson(eq("/package/find-data-packages"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"dataPackages\":[{\"supplier\":{\"supplierCode\":\"1\",\"supplierDescription\":\"TIGO\",\"supplierLogo\":\"https://cdn.example/tigo.webp\"},\"categories\":[{\"category\":{\"categoryCode\":\"2\",\"categoryDescription\":\"DATOS\"},\"packages\":[{\"productCode\":\"99\",\"sku\":\"SKU1\",\"amount\":6000,\"description\":\"2GB Navegacion\",\"completeName\":\"Paquete 2GB\",\"validity\":\"7 dias\"}]}]}]}}"));
        when(apiClient.postJson(eq("/payment-collection/v2/find-agreements-for-point-sale"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"agreements\":[{\"agreementId\":\"501\",\"agreementName\":\"BANCOLOMBIA\",\"productCode\":\"482\",\"bankName\":\"Bancolombia\",\"reference1Description\":\"Cuenta\"}]}}"));
        when(apiClient.postJson(eq("/payment-collection/find-categories"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"agreementsCategories\":[{\"categoryId\":6,\"categoryName\":\"Servicios Publicos\",\"categoryLogoName\":\"servicios\"}]}}"));
        when(apiClient.postJson(eq("/payment-collection/find-agreements-by-category"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"agreements\":[{\"agreementId\":100,\"agreementName\":\"EPM ENERGIA\",\"reference1Description\":\"Contrato\",\"reference2Description\":null,\"bankName\":\"EPM\",\"agreementType\":1}]}}"));
        when(apiClient.postJson(eq("/pin/find-categories"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":[{\"categoryId\":2,\"categoryName\":\"STREAMING\",\"categoryImage\":\"https://cdn.example/str.webp\"}]}}"));
        when(apiClient.postJson(eq("/pin/find-types-categories"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":[{\"typeIdCategory\":10,\"typeNameCategory\":\"SPOTIFY\",\"typeImageCategory\":\"https://cdn.example/sp.webp\"}]}}"));
        when(apiClient.postJson(eq("/pin/find-sub-categories"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":[{\"subCategoryId\":15,\"subCategoryDescription\":\"SPOTIFY PREMIUM\"}]}}"));
        when(apiClient.postJson(eq("/pin/consult"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":[{\"pinId\":\"3\",\"sku\":\"SKU-SP\",\"productCode\":\"571\",\"supplierCode\":\"108\",\"name\":\"SPOTIFY 1 MES\",\"summary\":\"Plan individual\",\"amount\":16900}]}}"));

        CatalogoServiciosResponse completo = catalogoService.consultarCatalogo(new CatalogoBusquedaRequest());
        assertThat(completo.getFuenteDatos()).isEqualTo("PUNTORED_H2H_V2");
        assertThat(completo.getSaldoComercio()).isEqualTo("3626567.87");
        assertThat(completo.getServicios()).hasSize(5);
        assertThat(completo.getServicios())
                .anySatisfy(s -> {
                    assertThat(s.getTipoOperacion()).isEqualTo("PIN");
                    assertThat(s.getLogoUrl()).isEqualTo("https://cdn.example/sp.webp");
                });

        CatalogoBusquedaRequest filtro = new CatalogoBusquedaRequest();
        filtro.setTipoOperacion("RECAUDO");
        filtro.setTexto("EPM");
        CatalogoServiciosResponse filtrado = catalogoService.consultarCatalogo(filtro);
        assertThat(filtrado.getFuenteDatos()).isEqualTo("CACHE_MEMORIA");
        assertThat(filtrado.getServicios()).hasSize(1);
        assertThat(filtrado.getServicios().get(0).getNombreServicio()).isEqualTo("EPM ENERGIA");
    }

    @Test
    void validarReferencia_bloqueaFormatoInvalidoYHabilitaConvenioValido() throws Exception {
        InfoToken user = new InfoToken(7, Roles.USUARIO_NORMAL);

        ValidarReferenciaRequest telMal = new ValidarReferenciaRequest();
        telMal.setTipoOperacion("RECARGA");
        telMal.setCodigoProducto("737");
        telMal.setReferencia1("12345");
        ValidacionReferenciaResponse failTel = pagoService.validarReferencia(telMal, user);
        assertThat(failTel.getReferenciaValida()).isFalse();
        assertThat(failTel.getTokenValidacion()).isNull();

        ValidarReferenciaRequest recaudoReq = new ValidarReferenciaRequest();
        recaudoReq.setTipoOperacion("RECAUDO_V2");
        recaudoReq.setIdConvenio("501");
        recaudoReq.setCodigoProducto("482");
        recaudoReq.setReferencia1("REF998877");

        when(apiClient.postJson(eq("/payment-collection/v2/check-agreement"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"message\":\"OK\",\"data\":[{\"codeRecord\":\"REC1\",\"descriptionRecord\":\"Factura Energia\",\"amount\":45000,\"typeAmount\":1,\"minAmount\":45000,\"maxAmount\":45000}]}"));

        ValidacionReferenciaResponse okRec = pagoService.validarReferencia(recaudoReq, user);
        assertThat(okRec.getReferenciaValida()).isTrue();
        assertThat(okRec.getMontoSugerido()).isEqualByComparingTo("45000");
        assertThat(okRec.getMontoEditable()).isFalse();
        assertThat(okRec.getTokenValidacion()).isNotBlank();
    }

    @Test
    void realizarPago_diferenciaExitosaRechazadaYPendienteConSeguridad() throws Exception {
        InfoToken owner = new InfoToken(7, Roles.USUARIO_NORMAL);
        Cuenta cuenta = crearCuentaConSaldo(7, new BigDecimal("100000.00"));
        Usuario usuario = crearUsuarioConPin(PIN_2FA);

        when(cuentaRepository.findByNumeroCuenta(CUENTA_18)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByCliente_Id(7)).thenReturn(Optional.of(usuario));
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            tx.setId(55);
            tx.setFechaRegistro(LocalDateTime.now(ZoneId.systemDefault()));
            return tx;
        });

        ValidarReferenciaRequest valReq = new ValidarReferenciaRequest();
        valReq.setTipoOperacion("RECARGA");
        valReq.setCodigoProducto("737");
        valReq.setReferencia1("3001234567");
        ValidacionReferenciaResponse valRes = pagoService.validarReferencia(valReq, owner);

        // 1. Timeout / NO_CONFIRMADA -> NO descuenta saldo
        PagoPuntoredRequest reqPago = construirPagoRequest(valRes, new BigDecimal("10000.00"));
        when(apiClient.postJson(eq("/recharge/buy"), any(), anyString()))
                .thenReturn(new PuntoredHttpResult(504, true, false, null, null));

        PagoPuntoredResponse noConf = pagoService.realizarPago(reqPago, owner);
        assertThat(noConf.getEstado()).isEqualTo("NO_CONFIRMADA");
        assertThat(noConf.getTransaccionConfirmada()).isFalse();
        assertThat(noConf.getSaldoNuevo()).isEqualByComparingTo("100000.00");

        // Regenerar PIN para segundo intento -> RECHAZADA -> NO descuenta saldo
        usuario = crearUsuarioConPin(PIN_2FA);
        when(usuarioRepository.findByCliente_Id(7)).thenReturn(Optional.of(usuario));
        when(apiClient.postJson(eq("/recharge/buy"), any(), anyString()))
                .thenReturn(new PuntoredHttpResult(400, false, false,
                        objectMapper.readTree("{\"state\":false,\"code\":\"HE11\",\"message\":\"Sin cupo\"}"), ""));

        PagoPuntoredResponse rechazada = pagoService.realizarPago(reqPago, owner);
        assertThat(rechazada.getEstado()).isEqualTo("RECHAZADA");
        assertThat(rechazada.getTransaccionConfirmada()).isFalse();
        assertThat(rechazada.getSaldoNuevo()).isEqualByComparingTo("100000.00");

        // Tercer intento -> EXITOSA -> SI descuenta saldo y entrega tirilla
        usuario = crearUsuarioConPin(PIN_2FA);
        when(usuarioRepository.findByCliente_Id(7)).thenReturn(Optional.of(usuario));
        when(apiClient.postJson(eq("/recharge/buy"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"message\":\"Exitoso\",\"data\":{\"transactionId\":\"998877\",\"authorizationCode\":\"AUT123\",\"trace\":\"" + valRes.getTrace() + "\",\"message\":\"[00] - OK\",\"ticket\":{\"header\":\"PUNTORED\",\"title\":\"RECIBO\",\"body\":{\"Valor\":\"$10.000\"},\"footer\":\"Gracias\",\"logo\":\"https://cdn.example/claro.webp\"}}}"));

        PagoPuntoredResponse exitosa = pagoService.realizarPago(reqPago, owner);
        assertThat(exitosa.getEstado()).isEqualTo("EXITOSA");
        assertThat(exitosa.getTransaccionConfirmada()).isTrue();
        assertThat(exitosa.getIdTransaccionProveedor()).isEqualTo("998877");
        assertThat(exitosa.getSaldoNuevo()).isEqualByComparingTo("90000.00");
        assertThat(exitosa.getTicket().getTitulo()).isEqualTo("RECIBO");

        // Acceso denegado a otro cliente
        InfoToken intruso = new InfoToken(999, Roles.USUARIO_NORMAL);
        assertThatThrownBy(() -> pagoService.realizarPago(reqPago, intruso))
                .isInstanceOf(ValidacionException.class)
                .extracting("codigo").isEqualTo(MensajeError.ACCESO_DENEGADO.getCodigo());
    }

    @Test
    void historialYDetalle_filtraPorUsuarioYReconciliaPendiente() throws Exception {
        InfoToken owner = new InfoToken(7, Roles.USUARIO_NORMAL);
        InfoToken otro = new InfoToken(8, Roles.USUARIO_NORMAL);
        Cuenta cuenta = crearCuentaConSaldo(7, new BigDecimal("50000.00"));

        TransaccionPago pendiente = new TransaccionPago();
        pendiente.setId(10);
        pendiente.setCuenta(cuenta);
        pendiente.setNumeroCuenta(CUENTA_18);
        pendiente.setIdServicio(737);
        pendiente.setIdProducto(737);
        pendiente.setTipoPago("RECARGA");
        pendiente.setTelefonoReferencia("3001234567");
        pendiente.setMonto(new BigDecimal("10000.00"));
        pendiente.setProveedor("PUNTORED");
        pendiente.setNombreServicio("CLARO");
        pendiente.setTrace("1234567890");
        pendiente.setEstado("NO_CONFIRMADA");
        pendiente.setCodigoRespuesta("TIMEOUT");
        pendiente.setMensajeRespuesta("Sin confirmar");
        pendiente.setFechaRegistro(LocalDateTime.now(ZoneId.systemDefault()));

        when(transaccionPagoRepository.findByCuenta_Cliente_IdOrderByFechaRegistroDesc(7))
                .thenReturn(List.of(pendiente));
        HistorialPagosResponse historial = pagoService.consultarHistorial(new HistorialPagosRequest(), owner);
        assertThat(historial.getTotal()).isEqualTo(1);
        verify(transaccionPagoRepository, never()).findAllByOrderByFechaRegistroDesc();

        when(transaccionPagoRepository.findById(10)).thenReturn(Optional.of(pendiente));
        DetallePagoRequest detReq = new DetallePagoRequest();
        detReq.setIdTransaccion(10);
        detReq.setConsultarEstadoProveedor(true);

        assertThatThrownBy(() -> pagoService.consultarDetalleYReverificar(detReq, otro))
                .isInstanceOf(ValidacionException.class)
                .extracting("codigo").isEqualTo(MensajeError.ACCESO_DENEGADO.getCodigo());

        when(apiClient.postJson(eq("/query/transaction-status-trace"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":[{\"transactionId\":776655,\"status\":\"APROBADA\",\"trace\":\"1234567890\"}]}"));
        when(apiClient.postJson(eq("/query/transaction-ticket"), any(), anyString()))
                .thenReturn(okResult("{\"state\":true,\"code\":\"00\",\"data\":{\"authorizationCode\":\"OK77\",\"ticket\":{\"title\":\"TIRILLA\",\"body\":{\"Valor\":\"$10.000\"}}}}"));

        DetallePagoResponse detalle = pagoService.consultarDetalleYReverificar(detReq, owner);
        assertThat(detalle.getEstadoActualizado()).isTrue();
        assertThat(detalle.getTransaccion().getEstado()).isEqualTo("EXITOSA");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("40000.00");
    }

    @Test
    void controladoresNuevos_deleganYProtegenCorrectamente() {
        SeguridadGuard guard = mock(SeguridadGuard.class);
        InfoToken user = new InfoToken(7, Roles.USUARIO_NORMAL);
        when(guard.exigirToken("Bearer ok")).thenReturn(user);

        PuntoredCatalogoService catSvc = mock(PuntoredCatalogoService.class);
        when(catSvc.consultarCatalogo(any())).thenReturn(new CatalogoServiciosResponse());
        CatalogoController catCtrl = new CatalogoController(catSvc);
        assertThat(catCtrl.obtenerCatalogoCompleto().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catCtrl.buscarCatalogo(null).getStatusCode()).isEqualTo(HttpStatus.OK);

        PuntoredPagoService paySvc = mock(PuntoredPagoService.class);
        when(paySvc.validarReferencia(any(), any())).thenReturn(new ValidacionReferenciaResponse());
        when(paySvc.realizarPago(any(), any())).thenReturn(new PagoPuntoredResponse());
        when(paySvc.consultarHistorial(any(), any())).thenReturn(new HistorialPagosResponse());
        when(paySvc.consultarDetalleYReverificar(any(), any())).thenReturn(new DetallePagoResponse());

        PuntoredPagoController payCtrl = new PuntoredPagoController(paySvc, guard);
        assertThat(payCtrl.validarReferencia("Bearer ok", new ValidarReferenciaRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payCtrl.realizarPagoPuntored("Bearer ok", new PagoPuntoredRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payCtrl.consultarHistorial("Bearer ok", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payCtrl.consultarDetalle("Bearer ok", new DetallePagoRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);

        when(cuentaRepository.findByCliente_Id(7)).thenReturn(List.of(crearCuentaConSaldo(7, new BigDecimal("25000"))));
        CuentaUsuarioController ctaCtrl = new CuentaUsuarioController(cuentaRepository, guard);
        assertThat(ctaCtrl.obtenerMisCuentas("Bearer ok").getBody().getCuentas()).hasSize(1);
    }

    private PuntoredHttpResult okResult(String json) throws Exception {
        return new PuntoredHttpResult(200, false, false, objectMapper.readTree(json), json);
    }

    private Cuenta crearCuentaConSaldo(int clienteId, BigDecimal saldo) {
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setNombre("Ana");
        cliente.setApellidoPaterno("Lopez");
        cliente.setApellidoMaterno("Perez");
        cliente.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(clienteId * 10);
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(CUENTA_18);
        cuenta.setSaldo(saldo);
        cuenta.setEstatus("ACTIVA");
        return cuenta;
    }

    private Usuario crearUsuarioConPin(String pin) {
        Usuario u = new Usuario();
        u.setId(1);
        u.setPin2faHash(hashUtil.sha256(pin));
        u.setPin2faExpiracion(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(5));
        return u;
    }

    private PagoPuntoredRequest construirPagoRequest(ValidacionReferenciaResponse valRes, BigDecimal monto) {
        PagoPuntoredRequest req = new PagoPuntoredRequest();
        req.setNumeroCuenta(CUENTA_18);
        req.setTipoOperacion("RECARGA");
        req.setNombreServicio("CLARO");
        req.setNombreProducto("Recarga CLARO");
        req.setCategoria("Recargas");
        req.setCodigoProducto("737");
        req.setCodigoProveedor("737");
        req.setReferencia1("3001234567");
        req.setMonto(monto);
        req.setTraceValidacion(valRes.getTrace());
        req.setTokenValidacion(valRes.getTokenValidacion());
        req.setConfirmacionExplicita(true);
        req.setPin2fa(PIN_2FA);
        return req;
    }
}
