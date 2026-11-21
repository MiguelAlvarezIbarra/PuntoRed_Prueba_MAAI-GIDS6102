package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.dto.gestopago.ProductoGestoPagoDTO;
import com.proyecto.servicios.entity.Producto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.ProductoData;
import com.proyecto.servicios.repositorys.ProductoRepository;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductoService {

    private static final Integer TIPO_FRONT_DEFAULT = 0;
    public static final String FUENTE_GESTOPAGO_OFICIAL = "GESTOPAGO_MX_OFICIAL";
    public static final String FUENTE_GESTOPAGO_SIMULADO = "GESTOPAGO_MX_SIMULADO";

    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final ProductoRepository productoRepository;
    private final ProductoCacheService productoCacheService;
    private final Object syncLock = new Object();

    private volatile String saldoComercioCache;
    private volatile long saldoComercioCacheExpiraMs = 0L;

    @Value("${app.cache.productos.db-ttl-seconds:86400}")
    private long dbCacheTtlSeconds = 86400L;

    @Value("${gestopago.productos.sync-on-startup:false}")
    private boolean syncOnStartupEnabled = true;

    @Value("${gestopago.simulacion.respaldo-automatico:true}")
    private boolean respaldoSimuladoActivo = false;

    public ProductoService(GestoPagoServiceClient gestoPagoServiceClient,
                           GestoPagoTokenService gestoPagoTokenService,
                           ProductoRepository productoRepository,
                           ProductoCacheService productoCacheService) {
        this.gestoPagoServiceClient = gestoPagoServiceClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.productoRepository = productoRepository;
        this.productoCacheService = productoCacheService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("Iniciando sincronización de productos al arrancar la aplicación...");
        if (tieneCatalogoVigenteEnBd()) {
            log.info("Catálogo de productos vigente en base de datos; se omite consulta externa en el arranque.");
            try {
                obtenerTodosLosProductos();
            } catch (Exception e) {
                log.warn("No se pudo precargar caché de productos desde BD: {}", e.getMessage());
            }
            return;
        }
        if (!syncOnStartupEnabled) {
            log.info("Sincronización automática de GestoPago al arranque deshabilitada (gestopago.productos.sync-on-startup=false) para respetar el límite diario de 3 consultas/IP.");
            return;
        }
        try {
            if (gestoPagoTokenService.obtenerTokenActual().isEmpty()) {
                gestoPagoTokenService.renovarToken();
            }
            sincronizarProductos();
        } catch (Exception e) {
            log.error("Sincronización inicial de productos falló: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void syncDaily() {
        log.info("Iniciando sincronización diaria programada de productos...");
        try {
            sincronizarProductos();
        } catch (Exception e) {
            log.error("Sincronización diaria de productos falló: {}", e.getMessage(), e);
        }
    }

    public int sincronizarProductos() {
        synchronized (syncLock) {
            log.info("Iniciando invocación a GestoPago (getProductList.do)");

            List<ProductoGestoPagoDTO> productosGestoPago;
            try {
                String xmlResponse = obtenerListaProductosXml();
                productosGestoPago = parsearProductos(xmlResponse);
            } catch (RuntimeException ex) {
                if (!respaldoSimuladoActivo) {
                    throw ex;
                }
                log.warn("GestoPago oficial no respondió o bloqueó la solicitud ({}); activando catálogo de respaldo simulado México.",
                        ex.getMessage());
                productosGestoPago = new ArrayList<>();
            }

            if (!productosGestoPago.isEmpty()) {
                int total = guardarProductos(productosGestoPago, FUENTE_GESTOPAGO_OFICIAL);
                productoCacheService.invalidar();
                log.info("Invocación a GestoPago oficial finalizada. Se procesaron {} productos.", total);
                return total;
            }

            if (respaldoSimuladoActivo) {
                log.warn("GestoPago oficial no devolvió nodo PRODUCTOS (posible bloqueo de IP código 05); guardando catálogo de respaldo simulado GestoPago México en PostgreSQL.");
                List<ProductoGestoPagoDTO> fallback = GestoPagoMxCatalogoFallback.construirCatalogoMexico();
                int totalFallback = guardarProductos(fallback, FUENTE_GESTOPAGO_SIMULADO);
                if (totalFallback > 0) {
                    productoCacheService.invalidar();
                }
                log.info("Catálogo de respaldo simulado GestoPago México guardado en BD ({} productos).", totalFallback);
                return totalFallback;
            }

            log.info("Invocación a GestoPago finalizada. Se procesaron 0 productos.");
            return 0;
        }
    }

    public List<Producto> obtenerOCargarCatalogoMx(boolean forzarRecarga) {
        if (!forzarRecarga) {
            List<Producto> existentes = obtenerTodosLosProductos();
            if (!existentes.isEmpty()) {
                return existentes;
            }
        }
        synchronized (syncLock) {
            if (!forzarRecarga) {
                List<Producto> existentes = obtenerTodosLosProductos();
                if (!existentes.isEmpty()) {
                    return existentes;
                }
            }
            sincronizarProductos();
            return obtenerTodosLosProductos();
        }
    }

    public String consultarSaldoComercioMx() {
        return consultarSaldoComercioMx(false);
    }

    public String consultarSaldoComercioMx(boolean forzarConsulta) {
        long ahoraMs = System.currentTimeMillis();
        if (!forzarConsulta && saldoComercioCache != null && ahoraMs < saldoComercioCacheExpiraMs) {
            return saldoComercioCache;
        }
        String resultado = null;
        try {
            Optional<GestoPagoToken> tokOpt = gestoPagoTokenService.obtenerTokenActual();
            if (tokOpt.isPresent() && tokOpt.get().getToken() != null && !tokOpt.get().getToken().isBlank()) {
                String xml = gestoPagoServiceClient.getBalance("Bearer " + tokOpt.get().getToken());
                if (xml != null && !xml.isBlank()) {
                    JSONObject json = XML.toJSONObject(xml);
                    JSONObject resp = json.optJSONObject("RESPONSE");
                    if (resp != null) {
                        JSONObject msg = resp.optJSONObject("MENSAJE");
                        String cod = msg != null ? String.valueOf(msg.opt("CODIGO")) : "";
                        if ("10".equals(cod) || "01".equals(cod) || "1".equals(cod)) {
                            Object saldo = resp.opt("SALDO");
                            if (saldo != null && !String.valueOf(saldo).isBlank()) {
                                resultado = String.valueOf(saldo);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("No se pudo consultar saldo oficial GestoPago (getBalance.do): {}", ex.getMessage());
        }
        if (resultado == null && respaldoSimuladoActivo) {
            resultado = "150000.00";
        }
        if (resultado != null) {
            saldoComercioCache = resultado;
            saldoComercioCacheExpiraMs = ahoraMs + 900_000L; // 15 min para no repetir llamadas externas
        }
        return resultado;
    }

    private boolean tieneCatalogoVigenteEnBd() {
        Optional<Producto> ultimo = productoRepository.findTopByOrderByFechaActualizacionDesc();
        if (ultimo.isEmpty() || ultimo.get().getFechaActualizacion() == null) {
            return false;
        }
        LocalDateTime limite = LocalDateTime.now(ZoneId.systemDefault()).minusSeconds(Math.max(60L, dbCacheTtlSeconds));
        return ultimo.get().getFechaActualizacion().isAfter(limite);
    }

    private String obtenerListaProductosXml() {
        String token = obtenerTokenObligatorio();

        try {
            return gestoPagoServiceClient.getProductList("Bearer " + token);
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.warn("GestoPago rechazó el token actual, intentando renovar y reintentar una vez...");
            gestoPagoTokenService.renovarToken();
            String nuevoToken = obtenerTokenObligatorio();

            try {
                return gestoPagoServiceClient.getProductList("Bearer " + nuevoToken);
            } catch (FeignException.Unauthorized | FeignException.Forbidden e2) {
                throw new ValidacionException(MensajeError.GESTOPAGO_AUTENTICACION,
                        "GestoPago rechazó la autenticación incluso después de renovar el token", e2);
            }
        } catch (RetryableException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new ValidacionException(MensajeError.GESTOPAGO_TIMEOUT,
                        "Timeout al llamar a getProductList.do en GestoPago", e);
            }
            throw new ValidacionException(MensajeError.GESTOPAGO_COMUNICACION,
                    "Error de comunicación con GestoPago (getProductList.do)", e);
        } catch (FeignException e) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "GestoPago respondió con un error al pedir la lista de productos: " + e.getMessage(), e);
        }
    }

    private String obtenerTokenObligatorio() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        if (tokenOpt.isEmpty()) {
            log.warn("No se encontró token activo. Intentando renovar token...");
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        }
        return tokenOpt
                .map(GestoPagoToken::getToken)
                .orElseThrow(() -> new ValidacionException(MensajeError.GESTOPAGO_AUTENTICACION,
                        "No se pudo obtener un token válido de GestoPago. Sincronización abortada."));
    }

    private List<ProductoGestoPagoDTO> parsearProductos(String xmlResponse) {
        log.debug("XML crudo recibido de GestoPago:\n{}", xmlResponse);

        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "La respuesta del servicio de productos está vacía.");
        }

        JSONObject jsonResponse;
        try {
            jsonResponse = XML.toJSONObject(xmlResponse);
        } catch (Exception e) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "No se pudo convertir el XML de GestoPago a JSON.", e);
        }

        if (!jsonResponse.has("RESPONSE")) {
            throw new ValidacionException(MensajeError.GESTOPAGO_RESPUESTA,
                    "Estructura JSON inesperada al parsear el XML de GestoPago: " + jsonResponse);
        }

        JSONObject responseObj = jsonResponse.getJSONObject("RESPONSE");
        if (!responseObj.has("PRODUCTOS")) {
            log.warn("Respuesta sin nodo PRODUCTOS. Posible respuesta vacía o sin productos: {}", jsonResponse);
            return new ArrayList<>();
        }

        JSONObject productosObj = responseObj.getJSONObject("PRODUCTOS");

        Object productoNode = productosObj.opt("producto");
        if (productoNode == null) {
            log.warn("No se encontró la clave 'producto' dentro de PRODUCTOS. Claves disponibles: {}",
                    productosObj.keySet());
            return new ArrayList<>();
        }

        List<ProductoGestoPagoDTO> productos = new ArrayList<>();
        if (productoNode instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.length(); i++) {
                productos.add(ProductoGestoPagoDTO.fromJson(jsonArray.getJSONObject(i)));
            }
        } else if (productoNode instanceof JSONObject singleProducto) {
            productos.add(ProductoGestoPagoDTO.fromJson(singleProducto));
        }

        return productos;
    }

    private int guardarProductos(List<ProductoGestoPagoDTO> productosGestoPago, String fuenteProveedor) {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.systemDefault());
        List<Producto> nuevosProductos = new ArrayList<>();

        for (ProductoGestoPagoDTO dto : productosGestoPago) {
            Producto producto = productoRepository
                    .findFirstByIdProductoAndIdServicioOrderByFechaActualizacionDesc(dto.getIdProducto(), dto.getIdServicio())
                    .orElseGet(Producto::new);

            producto.setIdProducto(dto.getIdProducto());
            producto.setIdServicio(dto.getIdServicio());
            producto.setIdCatTipoServicio(dto.getIdCatTipoServicio());
            producto.setTipoFront(dto.getTipoFront() != null ? dto.getTipoFront() : TIPO_FRONT_DEFAULT);
            producto.setTipoReferencia(dto.getTipoReferencia());
            producto.setPrecio(dto.getPrecio());
            producto.setNombreProducto(dto.getNombreProducto());
            producto.setNombreServicio(dto.getNombreServicio());
            producto.setDescripcion(dto.getDescripcion());
            producto.setLogoUrl(dto.getLogoUrl());
            producto.setCategoria(dto.getCategoria());
            producto.setCodigoProveedor(fuenteProveedor);
            producto.setTipoOperacion(dto.getTipoOperacion());
            producto.setFechaActualizacion(ahora);

            nuevosProductos.add(producto);
        }

        if (!nuevosProductos.isEmpty()) {
            productoRepository.saveAll(nuevosProductos);
        }
        return nuevosProductos.size();
    }

    public List<Producto> obtenerTodosLosProductos() {
        List<Producto> cacheados = productoCacheService.obtener();
        if (cacheados != null && !cacheados.isEmpty()) {
            log.debug("Productos obtenidos desde cache Redis.");
            return cacheados;
        }

        log.debug("Cache Redis no disponible o vacío. Consultando Postgres...");
        List<Producto> productos = productoRepository.findAllByOrderByTipoFrontAsc();
        productoCacheService.guardar(productos);

        return productos;
    }

    public List<ProductoData> obtenerProductos() {
        return obtenerTodosLosProductos().stream()
                .map(this::toData)
                .toList();
    }

    private ProductoData toData(Producto producto) {
        ProductoData data = new ProductoData();
        data.setIdProducto(producto.getIdProducto());
        data.setIdServicio(producto.getIdServicio());
        data.setIdCatTipoServicio(producto.getIdCatTipoServicio());
        data.setNombreProducto(producto.getNombreProducto());
        data.setNombreServicio(producto.getNombreServicio());
        data.setTipoFront(producto.getTipoFront());
        data.setTipoReferencia(producto.getTipoReferencia());
        data.setPrecio(producto.getPrecio());
        data.setFechaActualizacion(producto.getFechaActualizacion());
        return data;
    }
}
