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

    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final ProductoRepository productoRepository;
    private final ProductoCacheService productoCacheService;

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
        try {
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
        log.info("Iniciando invocación a GestoPago (getProductList.do)");

        String xmlResponse = obtenerListaProductosXml();
        List<ProductoGestoPagoDTO> productosGestoPago = parsearProductos(xmlResponse);
        int total = guardarProductos(productosGestoPago);

        productoCacheService.invalidar();

        log.info("Invocación a GestoPago finalizada. Se procesaron {} productos.", total);
        return total;
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

    private int guardarProductos(List<ProductoGestoPagoDTO> productosGestoPago) {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.systemDefault());
        List<Producto> nuevosProductos = new ArrayList<>();

        for (ProductoGestoPagoDTO dto : productosGestoPago) {
            Producto producto = productoRepository
                    .findByIdProductoAndIdServicio(dto.getIdProducto(), dto.getIdServicio())
                    .orElseGet(Producto::new);

            producto.setIdProducto(dto.getIdProducto());
            producto.setIdServicio(dto.getIdServicio());
            producto.setIdCatTipoServicio(dto.getIdCatTipoServicio());
            producto.setTipoFront(dto.getTipoFront() != null ? dto.getTipoFront() : TIPO_FRONT_DEFAULT);
            producto.setTipoReferencia(dto.getTipoReferencia());
            producto.setPrecio(dto.getPrecio());
            producto.setNombreProducto(dto.getNombreProducto());
            producto.setNombreServicio(dto.getNombreServicio());
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
