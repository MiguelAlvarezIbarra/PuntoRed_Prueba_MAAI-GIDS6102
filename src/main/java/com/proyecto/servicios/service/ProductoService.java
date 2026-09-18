package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.Producto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.repositorys.ProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductoService {

    private final GestoPagoServiceClient gestoPagoServiceClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final ProductoRepository productoRepository;

    public ProductoService(GestoPagoServiceClient gestoPagoServiceClient,
                           GestoPagoTokenService gestoPagoTokenService,
                           ProductoRepository productoRepository) {
        this.gestoPagoServiceClient = gestoPagoServiceClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.productoRepository = productoRepository;
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

    /**
     * Ya NO se traga los errores: los avienta para que el endpoint /sync
     * y los jobs programados sepan si realmente funcionó.
     * También loguea el XML crudo y el JSON ya parseado para poder ver
     * exactamente qué está regresando GestoPago (bájale a debug cuando
     * ya no lo necesites).
     */
    public int sincronizarProductos() {
        Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActual();
        if (tokenOpt.isEmpty()) {
            log.warn("No se encontró token activo. Intentando renovar token...");
            gestoPagoTokenService.renovarToken();
            tokenOpt = gestoPagoTokenService.obtenerTokenActual();
            if (tokenOpt.isEmpty()) {
                throw new IllegalStateException("No se pudo obtener el token de GestoPago. Sincronización abortada.");
            }
        }

        String token = tokenOpt.get().getToken();
        String xmlResponse = gestoPagoServiceClient.getProductList("Bearer " + token);

        log.info("XML crudo recibido de GestoPago:\n{}", xmlResponse);

        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new IllegalStateException("La respuesta del servicio de productos está vacía.");
        }

        JSONObject jsonResponse = XML.toJSONObject(xmlResponse);
        log.info("JSON parseado desde el XML:\n{}", jsonResponse.toString(2));

        if (!jsonResponse.has("RESPONSE") || !jsonResponse.getJSONObject("RESPONSE").has("PRODUCTOS")) {
            throw new IllegalStateException("Estructura JSON inesperada al parsear el XML: " + jsonResponse);
        }

        JSONObject productosObj = jsonResponse.getJSONObject("RESPONSE").getJSONObject("PRODUCTOS");

        Object productoNode = productosObj.opt("producto");
        if (productoNode == null) {
            log.warn("No se encontró la clave 'producto' dentro de PRODUCTOS. Claves disponibles: {}",
                    productosObj.keySet());
            throw new IllegalStateException(
                    "No se encontró el nodo 'producto' esperado. Revisa el log anterior para ver el nombre real de la clave.");
        }

        List<JSONObject> productList = new ArrayList<>();
        if (productoNode instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) productoNode;
            for (int i = 0; i < jsonArray.length(); i++) {
                productList.add(jsonArray.getJSONObject(i));
            }
        } else if (productoNode instanceof JSONObject) {
            productList.add((JSONObject) productoNode);
        }

        List<Producto> nuevosProductos = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (JSONObject prodJson : productList) {
            Integer idProducto = prodJson.optInt("idProducto");
            Integer idServicio = prodJson.optInt("idServicio");

            Optional<Producto> existente = productoRepository.findByIdProductoAndIdServicio(idProducto, idServicio);
            Producto prod = existente.orElseGet(Producto::new);

            prod.setIdProducto(idProducto);
            prod.setIdServicio(idServicio);
            prod.setIdCatTipoServicio(prodJson.optInt("idCatTipoServicio"));
            prod.setTipoFront(prodJson.optInt("tipoFront"));
            prod.setTipoReferencia(prodJson.optString("tipoReferencia", null));
            prod.setPrecio(prodJson.optString("precio", null));

            String nombreProducto = prodJson.optString("content", "Desconocido");
            prod.setNombreProducto(nombreProducto);
            prod.setNombreServicio(prodJson.optString("servicio", nombreProducto));
            prod.setFechaActualizacion(ahora);

            nuevosProductos.add(prod);
        }

        productoRepository.saveAll(nuevosProductos);
        log.info("Sincronización finalizada. Se procesaron {} productos.", nuevosProductos.size());
        return nuevosProductos.size();
    }

    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll();
    }
}