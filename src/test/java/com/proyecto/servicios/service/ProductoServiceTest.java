package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.dto.response.ApiResponse;
import com.proyecto.servicios.dto.response.ResponseCode;
import com.proyecto.servicios.entity.Producto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.ProductoData;
import com.proyecto.servicios.repositorys.ProductoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private GestoPagoServiceClient gestoPagoServiceClient;
    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProductoCacheService productoCacheService;
    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;
    @Mock
    private GestoPagoTokenRepository tokenRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ProductoService productoService;
    private GestoPagoToken tokenActivo;

    private static final String XML_UN_PRODUCTO =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<RESPONSE><PRODUCTOS><producto " +
                    "idProducto=\"101\" " +
                    "idServicio=\"5\" " +
                    "idCatTipoServicio=\"2\" " +
                    "tipoFront=\"1\" " +
                    "tipoReferencia=\"a\" " +
                    "precio=\"150.00\">" +
                    "Recarga Telcel" +
                    "</producto></PRODUCTOS>" +
                    "<MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>" +
                    "</RESPONSE>";

    private static final String XML_MULTIPLES_PRODUCTOS =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<RESPONSE><PRODUCTOS>" +
            "<producto idProducto=\"101\" idServicio=\"5\" precio=\"10\">P1</producto>" +
            "<producto idProducto=\"102\" idServicio=\"5\" precio=\"20\">P2</producto>" +
            "</PRODUCTOS></RESPONSE>";

    private static final String XML_RESPUESTA_VACIA =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<RESPONSE><MENSAJE><CODIGO>99</CODIGO><TEXTO>Sin productos</TEXTO></MENSAJE></RESPONSE>";

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(gestoPagoServiceClient, gestoPagoTokenService, productoRepository, productoCacheService);
        tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("token-valido");
    }

    @Test
    void sincronizarProductos_guardaProductoCuandoTodoSaleBien() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn(XML_UN_PRODUCTO);
        when(productoRepository.findByIdProductoAndIdServicio(101, 5)).thenReturn(Optional.empty());

        productoService.sincronizarProductos();

        verify(productoRepository).saveAll(argThat((Iterable<Producto> lista) -> {
            Producto p = lista.iterator().next();
            return p.getIdProducto().equals(101);
        }));
        verify(gestoPagoTokenService, never()).renovarToken();
    }

    @Test
    void sincronizarProductos_guardaMultiplesProductos() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn(XML_MULTIPLES_PRODUCTOS);
        when(productoRepository.findByIdProductoAndIdServicio(any(), any())).thenReturn(Optional.empty());

        int total = productoService.sincronizarProductos();
        assertThat(total).isEqualTo(2);
    }

    @Test
    void sincronizarProductos_intentaRenovarTokenCuandoNoHayActivo() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.empty());
        doNothing().when(gestoPagoTokenService).renovarToken();

        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());

        verify(gestoPagoTokenService).renovarToken();
        verify(gestoPagoServiceClient, never()).getProductList(any());
    }

    @Test
    void sincronizarProductos_reintentaCuandoTokenEsRechazadoYErroresRed() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        Request feignReq = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        FeignException.Unauthorized unauthorized = new FeignException.Unauthorized("401", feignReq, null, null);

        when(gestoPagoServiceClient.getProductList("Bearer token-valido"))
                .thenThrow(unauthorized)
                .thenReturn(XML_UN_PRODUCTO);

        int total = productoService.sincronizarProductos();
        assertThat(total).isEqualTo(1);

        // Caso: falla también en el segundo intento
        doThrow(unauthorized).when(gestoPagoServiceClient).getProductList("Bearer token-valido");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());

        // Caso: Timeout
        RetryableException timeoutEx = new RetryableException(504, "timeout", Request.HttpMethod.GET, new SocketTimeoutException(), (Date) null, feignReq);
        doThrow(timeoutEx).when(gestoPagoServiceClient).getProductList("Bearer token-valido");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());

        // Caso: Error de comunicacion
        RetryableException commEx = new RetryableException(503, "comm", Request.HttpMethod.GET, new RuntimeException("net"), (Date) null, feignReq);
        doThrow(commEx).when(gestoPagoServiceClient).getProductList("Bearer token-valido");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());

        // Caso: Otro FeignException
        FeignException badGateway = new FeignException.BadGateway("502", feignReq, null, null);
        doThrow(badGateway).when(gestoPagoServiceClient).getProductList("Bearer token-valido");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());
    }

    @Test
    void sincronizarProductos_noFallaCuandoRespuestaEsVaciaOSinNodoProducto() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn(XML_RESPUESTA_VACIA);

        productoService.sincronizarProductos();
        verify(productoRepository, never()).saveAll(any());

        when(gestoPagoServiceClient.getProductList("Bearer token-valido"))
                .thenReturn("<RESPONSE><PRODUCTOS><otro>1</otro></PRODUCTOS></RESPONSE>");
        assertThat(productoService.sincronizarProductos()).isZero();

        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn("");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());

        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn("<OTRO>1</OTRO>");
        assertThrows(ValidacionException.class, () -> productoService.sincronizarProductos());
    }

    @Test
    void syncOnStartupYSyncDaily_capturanExcepcionesYNoFallan() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.empty());
        productoService.syncOnStartup();
        productoService.syncDaily();
        verify(gestoPagoTokenService, org.mockito.Mockito.atLeastOnce()).renovarToken();
    }

    @Test
    void obtenerTodosLosProductos_retornaListaDesdeRepositorioOCache() {
        Producto p = new Producto();
        p.setIdProducto(1);
        p.setIdServicio(1);
        p.setNombreProducto("Test");
        p.setNombreServicio("Test");

        when(productoCacheService.obtener()).thenReturn(null);
        when(productoRepository.findAllByOrderByTipoFrontAsc()).thenReturn(List.of(p));

        List<ProductoData> dtos = productoService.obtenerProductos();
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getNombreProducto()).isEqualTo("Test");

        when(productoCacheService.obtener()).thenReturn(List.of(p));
        assertThat(productoService.obtenerTodosLosProductos()).hasSize(1);
    }

    @Test
    void productoCacheService_operaConFallbackAnteFallosRedis() {
        ProductoCacheService cacheService = new ProductoCacheService(redisTemplate);
        ReflectionTestUtils.setField(cacheService, "cacheTtlSeconds", 60L);

        Producto p = new Producto();
        p.setNombreProducto("CacheProd");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("productos:todos")).thenReturn(List.of(p));
        assertThat(cacheService.obtener()).hasSize(1);

        cacheService.guardar(List.of(p));
        cacheService.invalidar();

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        assertThat(cacheService.obtener()).isNull();
        cacheService.guardar(List.of(p));

        doThrow(new RuntimeException("err")).when(redisTemplate).delete(anyString());
        cacheService.invalidar();
    }

    @Test
    void gestoPagoTokenService_renuevaYObtenerToken() {
        GestoPagoTokenServiceImpl tokenService = new GestoPagoTokenServiceImpl(gestoPagoAuthClient, tokenRepository);
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "dev1");
        ReflectionTestUtils.setField(tokenService, "password", "pass1");

        when(gestoPagoAuthClient.authenticate(83, "dev1", "pass1"))
                .thenReturn("{\"token\":\"jwt-ok\",\"success\":true}");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "dev1")).thenReturn(Optional.empty());

        tokenService.renovarToken();
        verify(tokenRepository).save(any(GestoPagoToken.class));

        tokenActivo.setActivo(true);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "dev1"))
                .thenReturn(Optional.of(tokenActivo));
        assertThat(tokenService.obtenerTokenActual()).isPresent();

        // Respuesta invalida o rechazada
        when(gestoPagoAuthClient.authenticate(83, "dev1", "pass1"))
                .thenReturn("{\"token\":\"INVALID\",\"success\":false,\"message\":\"bad\"}");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        when(gestoPagoAuthClient.authenticate(83, "dev1", "pass1")).thenReturn("");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        when(gestoPagoAuthClient.authenticate(83, "dev1", "pass1")).thenReturn("no-json");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        // Errores de Feign en authenticate
        Request feignReq = Request.create(Request.HttpMethod.POST, "/auth", Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        doThrow(new RetryableException(504, "t", Request.HttpMethod.POST, new SocketTimeoutException(), (Date) null, feignReq))
                .when(gestoPagoAuthClient).authenticate(83, "dev1", "pass1");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        doThrow(new RetryableException(503, "c", Request.HttpMethod.POST, new RuntimeException(), (Date) null, feignReq))
                .when(gestoPagoAuthClient).authenticate(83, "dev1", "pass1");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        doThrow(new FeignException.Unauthorized("401", feignReq, null, null))
                .when(gestoPagoAuthClient).authenticate(83, "dev1", "pass1");
        assertThrows(ValidacionException.class, tokenService::renovarToken);

        // Credenciales incompletas
        ReflectionTestUtils.setField(tokenService, "password", "");
        assertThrows(IllegalStateException.class, tokenService::renovarToken);

        // ApiResponse y ResponseCode
        ApiResponse<String> okResp = ApiResponse.ok("dato");
        assertThat(okResp.getCodigo()).isZero();
        assertThat(okResp.getData()).isEqualTo("dato");
        ApiResponse<String> errResp = ApiResponse.error(ResponseCode.ERROR_SIN_DATOS);
        assertThat(errResp.getCodigo()).isEqualTo(ResponseCode.ERROR_SIN_DATOS.getCodigo());
        ApiResponse<String> errDet = ApiResponse.error(ResponseCode.ERROR_GENERICO, "detalle");
        assertThat(errDet.getMensaje()).isEqualTo("detalle");
    }
}
