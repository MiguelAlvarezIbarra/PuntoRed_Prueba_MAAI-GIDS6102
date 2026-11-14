package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.PuntoredApiClient;
import com.proyecto.servicios.client.PuntoredHttpResult;
import com.proyecto.servicios.config.PuntoredProperties;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.puntored.CatalogoBusquedaRequest;
import com.proyecto.servicios.model.puntored.CatalogoCategoriaItem;
import com.proyecto.servicios.model.puntored.CatalogoServicioItem;
import com.proyecto.servicios.model.puntored.CatalogoServiciosResponse;
import com.proyecto.servicios.service.PuntoredCatalogoService;
import com.proyecto.servicios.service.PuntoredTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio dinamico del catalogo de PuntoRed H2H V2.
 * Consolida Recargas, Paquetes, Recaudos (V1/V2 por categoria) y Pines Digitales,
 * conservando logotipos oficiales, descripciones, categorias y saldo de comercio.
 */
@Service
@Slf4j
public class PuntoredCatalogoServiceImpl implements PuntoredCatalogoService {

    private static final String REDIS_KEY_SNAPSHOT = "puntored:catalogo:snapshot:v1";
    private static final String KEY_POS = "pointOfSale";
    private static final String KEY_DATA = "data";
    private static final String OP_RECARGA = "RECARGA";
    private static final String OP_PAQUETE = "PAQUETE";
    private static final String OP_RECAUDO = "RECAUDO";
    private static final String OP_RECAUDO_V2 = "RECAUDO_V2";
    private static final String OP_PIN = "PIN";

    private final PuntoredTokenService tokenService;
    private final PuntoredApiClient apiClient;
    private final PuntoredProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final long cacheTtlSeconds;

    private final AtomicReference<CachedSnapshot> memoriaCache = new AtomicReference<>();

    @Autowired
    public PuntoredCatalogoServiceImpl(
            PuntoredTokenService tokenService,
            PuntoredApiClient apiClient,
            PuntoredProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${app.cache.puntored.ttl-seconds:3600}") long cacheTtlSeconds) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplateProvider != null ? redisTemplateProvider.getIfAvailable() : null;
        this.cacheTtlSeconds = cacheTtlSeconds > 0 ? cacheTtlSeconds : 3600L;
    }

    public PuntoredCatalogoServiceImpl(
            PuntoredTokenService tokenService,
            PuntoredApiClient apiClient,
            PuntoredProperties properties,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            long cacheTtlSeconds) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.cacheTtlSeconds = cacheTtlSeconds > 0 ? cacheTtlSeconds : 3600L;
    }

    @Override
    public CatalogoServiciosResponse consultarCatalogo(CatalogoBusquedaRequest request) {
        CatalogoBusquedaRequest filtro = request != null ? request : new CatalogoBusquedaRequest();
        if (Boolean.TRUE.equals(filtro.getForzarRecarga())) {
            invalidarCache();
        }

        if (tieneTexto(filtro.getIdSubcategoriaPin())) {
            return consultarPinesPorSubcategoria(filtro.getIdSubcategoriaPin(), filtro.getTexto());
        }
        if (tieneTexto(filtro.getIdTipoPin())) {
            return consultarPinesPorTipo(filtro.getIdTipoPin(), filtro.getTexto());
        }

        CatalogoSnapshot snapshot = obtenerSnapshotBase();
        List<CatalogoServicioItem> servicios = new ArrayList<>(snapshot.servicios);

        if (tieneTexto(filtro.getIdCategoriaRecaudo())) {
            servicios.addAll(cargarConveniosRecaudoCategoria(
                    filtro.getIdCategoriaRecaudo(),
                    buscarNombreCategoriaRecaudo(snapshot.categorias, filtro.getIdCategoriaRecaudo()),
                    snapshot.productCodesV2
            ));
        }

        List<CatalogoServicioItem> filtrados = filtrarServicios(servicios, filtro);
        CatalogoServiciosResponse response = new CatalogoServiciosResponse();
        response.setCodigo(0);
        response.setMensaje("Catalogo PuntoRed consultado exitosamente");
        response.setFuenteDatos(snapshot.fuenteDatos);
        response.setSaldoComercio(snapshot.saldoComercio);
        response.setCategorias(filtrarCategorias(snapshot.categorias, filtro));
        response.setServicios(filtrados);
        return response;
    }

    private CatalogoSnapshot obtenerSnapshotBase() {
        CachedSnapshot enMemoria = memoriaCache.get();
        if (enMemoria != null && Instant.now().isBefore(enMemoria.expiraEn)) {
            return copiaConFuente(enMemoria.snapshot, "CACHE_MEMORIA");
        }

        CatalogoSnapshot desdeRedis = leerDesdeRedis();
        if (desdeRedis != null) {
            memoriaCache.set(new CachedSnapshot(desdeRedis, Instant.now().plusSeconds(cacheTtlSeconds)));
            return copiaConFuente(desdeRedis, "CACHE_REDIS");
        }

        CatalogoSnapshot fresco = construirSnapshotVivo();
        memoriaCache.set(new CachedSnapshot(fresco, Instant.now().plusSeconds(cacheTtlSeconds)));
        guardarEnRedis(fresco);
        return fresco;
    }

    private CatalogoSnapshot construirSnapshotVivo() {
        List<CatalogoCategoriaItem> categorias = new ArrayList<>();
        List<CatalogoServicioItem> servicios = new ArrayList<>();
        String saldoComercio = consultarSaldoComercio();

        int modulosExitosos = 0;
        modulosExitosos += cargarRecargas(categorias, servicios) ? 1 : 0;
        modulosExitosos += cargarPaquetes(categorias, servicios) ? 1 : 0;

        Map<String, String> v2ProductCodes = cargarRecaudos(categorias, servicios);
        if (!v2ProductCodes.isEmpty() || categorias.stream().anyMatch(c -> OP_RECAUDO.equals(c.getTipoOperacion()))) {
            modulosExitosos++;
        }

        modulosExitosos += cargarPinesCatalogoInicial(categorias, servicios) ? 1 : 0;

        if (modulosExitosos == 0) {
            throw new ValidacionException(MensajeError.PUNTORED_COMUNICACION);
        }

        return new CatalogoSnapshot("PUNTORED_H2H_V2", saldoComercio, categorias, servicios, v2ProductCodes);
    }

    private String consultarSaldoComercio() {
        Map<String, Object> body = Map.of("commerce", properties.getCommerce());
        PuntoredHttpResult res = postAutenticado("/query/balance-commerce", body);
        if (!res.esTransaccionExitosa() || res.body() == null) {
            return null;
        }
        return textoOpcional(res.body().path(KEY_DATA).path("balance"));
    }

    private boolean cargarRecargas(List<CatalogoCategoriaItem> categorias, List<CatalogoServicioItem> servicios) {
        PuntoredHttpResult res = postAutenticado("/recharge/find-suppliers", Map.of(KEY_POS, pos()));
        if (!res.esTransaccionExitosa() || res.body() == null) {
            return false;
        }

        JsonNode suppliers = res.body().path(KEY_DATA).path("suppliers");
        if (!suppliers.isArray()) {
            return false;
        }

        int count = 0;
        for (JsonNode s : suppliers) {
            String code = textoOpcional(s.path("supplierCode"));
            String name = textoOpcional(s.path("supplierDescription"));
            String logo = normalizarUrlLogo(textoOpcional(s.path("supplierLogo")));
            if (!tieneTexto(code) || !tieneTexto(name)) {
                continue;
            }
            CatalogoServicioItem item = new CatalogoServicioItem();
            item.setClaveUnica("RECARGA-" + code);
            item.setTipoOperacion(OP_RECARGA);
            item.setCategoria("Recargas");
            item.setNombreServicio(name);
            item.setNombreProducto("Recarga " + name);
            item.setDescripcion("Recarga directa en linea para " + name);
            item.setProveedorCodigo(code);
            item.setProveedorNombre(name);
            item.setCodigoProducto(code);
            item.setLogoUrl(logo);
            item.setEtiquetaReferencia1("BetPlay".equalsIgnoreCase(name)
                    ? "Documento o numero de linea"
                    : "Numero de celular (10 digitos)");
            item.setRequiereValidacionReferencia(true);
            servicios.add(item);
            count++;
        }

        categorias.add(new CatalogoCategoriaItem("RECARGAS", "Recargas", OP_RECARGA, null, null, count));
        return true;
    }

    private boolean cargarPaquetes(List<CatalogoCategoriaItem> categorias, List<CatalogoServicioItem> servicios) {
        PuntoredHttpResult res = postAutenticado("/package/find-data-packages", Map.of(KEY_POS, pos()));
        if (!res.esTransaccionExitosa() || res.body() == null) {
            return false;
        }

        JsonNode dataPackages = res.body().path(KEY_DATA).path("dataPackages");
        if (!dataPackages.isArray()) {
            return false;
        }

        Map<String, CatalogoCategoriaItem> catMap = new LinkedHashMap<>();
        for (JsonNode wrapper : dataPackages) {
            JsonNode supplier = wrapper.path("supplier");
            String supplierCode = textoOpcional(supplier.path("supplierCode"));
            String supplierName = textoOpcional(supplier.path("supplierDescription"));
            String supplierLogo = normalizarUrlLogo(textoOpcional(supplier.path("supplierLogo")));

            JsonNode cats = wrapper.path("categories");
            if (!cats.isArray()) {
                continue;
            }
            for (JsonNode catEntry : cats) {
                procesarCategoriaPaquetes(catEntry, supplierCode, supplierName, supplierLogo, catMap, servicios);
            }
        }
        categorias.addAll(catMap.values());
        return true;
    }

    private void procesarCategoriaPaquetes(
            JsonNode catEntry,
            String supplierCode,
            String supplierName,
            String supplierLogo,
            Map<String, CatalogoCategoriaItem> catMap,
            List<CatalogoServicioItem> servicios) {
        JsonNode cat = catEntry.path("category");
        String catCode = textoOpcional(cat.path("categoryCode"));
        String catDesc = textoOpcional(cat.path("categoryDescription"));
        String categoriaNombre = tieneTexto(catDesc) ? "Paquetes - " + catDesc : "Paquetes";

        CatalogoCategoriaItem catItem = catMap.computeIfAbsent(
                "PAQ-" + supplierCode + "-" + catCode,
                k -> new CatalogoCategoriaItem(
                        catCode,
                        tieneTexto(supplierName) ? supplierName + " / " + catDesc : categoriaNombre,
                        OP_PAQUETE,
                        supplierLogo,
                        "Paquetes",
                        0
                )
        );

        JsonNode packages = catEntry.path("packages");
        if (!packages.isArray()) {
            return;
        }

        for (JsonNode pkg : packages) {
            String productCode = textoOpcional(pkg.path("productCode"));
            String sku = textoOpcional(pkg.path("sku"));
            String completeName = textoOpcional(pkg.path("completeName"));
            String desc = textoOpcional(pkg.path("description"));
            String validity = textoOpcional(pkg.path("validity"));
            BigDecimal amount = parseDecimal(pkg.path("amount"));

            CatalogoServicioItem item = new CatalogoServicioItem();
            item.setClaveUnica("PAQ-" + supplierCode + "-" + productCode + "-" + sku);
            item.setTipoOperacion(OP_PAQUETE);
            item.setCategoria(categoriaNombre);
            item.setNombreServicio(supplierName);
            item.setNombreProducto(tieneTexto(completeName) ? completeName : desc);
            item.setDescripcion(construirDescripcionPaquete(desc, validity));
            item.setProveedorCodigo(supplierCode);
            item.setProveedorNombre(supplierName);
            item.setLogoUrl(supplierLogo);
            item.setCodigoProducto(productCode);
            item.setCodigoCategoria(catCode);
            item.setSku(sku);
            item.setPrecioSugerido(amount);
            item.setValidez(validity);
            item.setEtiquetaReferencia1("Numero de celular (10 digitos)");
            item.setRequiereValidacionReferencia(true);
            servicios.add(item);
            catItem.setTotalElementos(catItem.getTotalElementos() + 1);
        }
    }

    private Map<String, String> cargarRecaudos(List<CatalogoCategoriaItem> categorias, List<CatalogoServicioItem> servicios) {
        Map<String, String> productCodesV2 = new LinkedHashMap<>();
        PuntoredHttpResult v2Res = postAutenticado("/payment-collection/v2/find-agreements-for-point-sale", Map.of(KEY_POS, pos()));
        if (v2Res.esTransaccionExitosa() && v2Res.body() != null) {
            JsonNode v2Agreements = v2Res.body().path(KEY_DATA).path("agreements");
            if (v2Agreements.isArray()) {
                for (JsonNode ag : v2Agreements) {
                    String agreementId = textoOpcional(ag.path("agreementId"));
                    String productCode = textoOpcional(ag.path("productCode"));
                    if (tieneTexto(agreementId) && tieneTexto(productCode)) {
                        productCodesV2.put(agreementId, productCode);
                    }
                    servicios.add(mapearConvenio(ag, "Recaudos V2", productCode, null));
                }
            }
        }

        PuntoredHttpResult catRes = postAutenticado("/payment-collection/find-categories", Collections.emptyMap());
        if (catRes.esTransaccionExitosa() && catRes.body() != null) {
            JsonNode cats = catRes.body().path(KEY_DATA).path("agreementsCategories");
            if (cats.isArray()) {
                for (JsonNode c : cats) {
                    String catId = textoOpcional(c.path("categoryId"));
                    String catName = textoOpcional(c.path("categoryName"));
                    String catLogo = normalizarUrlLogo(textoOpcional(c.path("categoryLogoName")));
                    if (!tieneTexto(catId) || !tieneTexto(catName)) {
                        continue;
                    }
                    List<CatalogoServicioItem> ags = cargarConveniosRecaudoCategoria(catId, catName, productCodesV2);
                    categorias.add(new CatalogoCategoriaItem(catId, catName, OP_RECAUDO, catLogo, "Recaudos", ags.size()));
                    for (CatalogoServicioItem item : ags) {
                        agregarSiNoExiste(servicios, item);
                    }
                }
            }
        }
        return productCodesV2;
    }

    private List<CatalogoServicioItem> cargarConveniosRecaudoCategoria(
            String categoryId,
            String categoryName,
            Map<String, String> productCodesV2) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put(KEY_POS, pos());
        req.put("categoryId", parseEnteroSeguro(categoryId));

        PuntoredHttpResult res = postAutenticado("/payment-collection/find-agreements-by-category", req);
        if (!res.esTransaccionExitosa() || res.body() == null) {
            return Collections.emptyList();
        }
        JsonNode agreements = res.body().path(KEY_DATA).path("agreements");
        if (!agreements.isArray()) {
            return Collections.emptyList();
        }
        List<CatalogoServicioItem> items = new ArrayList<>();
        String catLabel = tieneTexto(categoryName) ? "Recaudos - " + categoryName : "Recaudos";
        for (JsonNode ag : agreements) {
            String agId = textoOpcional(ag.path("agreementId"));
            String v2Code = agId != null ? productCodesV2.get(agId) : null;
            items.add(mapearConvenio(ag, catLabel, v2Code, categoryId));
        }
        return items;
    }

    private CatalogoServicioItem mapearConvenio(
            JsonNode ag,
            String categoriaNombre,
            String v2ProductCode,
            String categoryId) {
        String agId = textoOpcional(ag.path("agreementId"));
        String agName = textoOpcional(ag.path("agreementName"));
        String ref1 = textoOpcional(ag.path("reference1Description"));
        String ref2 = textoOpcional(ag.path("reference2Description"));
        String bank = textoOpcional(ag.path("bankName"));
        String prodCode = tieneTexto(v2ProductCode) ? v2ProductCode : textoOpcional(ag.path("productCode"));
        Integer agreementType = ag.hasNonNull("agreementType") ? ag.get("agreementType").asInt() : null;

        CatalogoServicioItem item = new CatalogoServicioItem();
        item.setClaveUnica("REC-" + agId + (tieneTexto(prodCode) ? "-" + prodCode : ""));
        item.setTipoOperacion(tieneTexto(prodCode) ? OP_RECAUDO_V2 : OP_RECAUDO);
        item.setCategoria(categoriaNombre);
        item.setNombreServicio(agName);
        item.setNombreProducto(agName);
        item.setDescripcion(construirDescripcionConvenio(bank, ref1, ref2));
        item.setProveedorNombre(tieneTexto(bank) ? bank : "PuntoRed Recaudos");
        item.setIdConvenio(agId);
        item.setCodigoProducto(prodCode);
        item.setCodigoCategoria(categoryId);
        item.setEtiquetaReferencia1(tieneTexto(ref1) ? ref1 : "Referencia principal");
        item.setEtiquetaReferencia2(tieneTexto(ref2) && !"null".equalsIgnoreCase(ref2) ? ref2 : null);
        item.setTipoRecaudo(agreementType);
        item.setNombreBanco(bank);
        item.setRequiereValidacionReferencia(true);
        return item;
    }

    private boolean cargarPinesCatalogoInicial(List<CatalogoCategoriaItem> categorias, List<CatalogoServicioItem> servicios) {
        PuntoredHttpResult catRes = postAutenticado("/pin/find-categories", Collections.emptyMap());
        if (!catRes.esTransaccionExitosa() || catRes.body() == null || !catRes.body().path(KEY_DATA).isArray()) {
            return false;
        }

        for (JsonNode cat : catRes.body().path(KEY_DATA)) {
            String catId = textoOpcional(cat.path("categoryId"));
            String catName = textoOpcional(cat.path("categoryName"));
            String catImg = normalizarUrlLogo(textoOpcional(cat.path("categoryImage")));
            cargarTiposDeCategoriaPin(catId, catName, catImg, categorias, servicios);
        }
        return true;
    }

    private void cargarTiposDeCategoriaPin(
            String catId,
            String catName,
            String catImg,
            List<CatalogoCategoriaItem> categorias,
            List<CatalogoServicioItem> servicios) {
        if (!tieneTexto(catId)) {
            return;
        }
        PuntoredHttpResult typeRes = postAutenticado(
                "/pin/find-types-categories",
                Map.of("categoryId", parseEnteroSeguro(catId))
        );
        if (!typeRes.esTransaccionExitosa() || typeRes.body() == null || !typeRes.body().path(KEY_DATA).isArray()) {
            categorias.add(new CatalogoCategoriaItem(catId, catName, OP_PIN, catImg, "Pines", 0));
            return;
        }

        for (JsonNode t : typeRes.body().path(KEY_DATA)) {
            String typeId = textoOpcional(t.path("typeIdCategory"));
            String typeName = textoOpcional(t.path("typeNameCategory"));
            String typeImg = normalizarUrlLogo(textoOpcional(t.path("typeImageCategory")));
            String logo = tieneTexto(typeImg) ? typeImg : catImg;

            List<CatalogoServicioItem> pines = consultarPinesDeTipo(typeId, typeName, catName, logo);
            categorias.add(new CatalogoCategoriaItem(
                    typeId,
                    typeName,
                    OP_PIN,
                    logo,
                    "Pines - " + catName,
                    pines.size()
            ));
            servicios.addAll(pines);
        }
    }

    private List<CatalogoServicioItem> consultarPinesDeTipo(
            String typeId,
            String typeName,
            String catName,
            String logo) {
        if (!tieneTexto(typeId)) {
            return Collections.emptyList();
        }
        PuntoredHttpResult subRes = postAutenticado(
                "/pin/find-sub-categories",
                Map.of("typeIdCategory", parseEnteroSeguro(typeId))
        );
        if (!subRes.esTransaccionExitosa() || subRes.body() == null || !subRes.body().path(KEY_DATA).isArray()) {
            return Collections.emptyList();
        }

        List<CatalogoServicioItem> resultado = new ArrayList<>();
        String categoriaFull = "Pines - " + (tieneTexto(catName) ? catName : typeName);
        for (JsonNode sub : subRes.body().path(KEY_DATA)) {
            String subId = textoOpcional(sub.path("subCategoryId"));
            String subDesc = textoOpcional(sub.path("subCategoryDescription"));
            resultado.addAll(consultarPinesSubcategoria(subId, subDesc, typeName, categoriaFull, logo));
        }
        return resultado;
    }

    private List<CatalogoServicioItem> consultarPinesSubcategoria(
            String subId,
            String subDesc,
            String proveedorNombre,
            String categoriaFull,
            String logo) {
        if (!tieneTexto(subId)) {
            return Collections.emptyList();
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("subCategoryId", parseEnteroSeguro(subId));
        req.put(KEY_POS, pos());

        PuntoredHttpResult pinRes = postAutenticado("/pin/consult", req);
        if (!pinRes.esTransaccionExitosa() || pinRes.body() == null || !pinRes.body().path(KEY_DATA).isArray()) {
            return Collections.emptyList();
        }

        List<CatalogoServicioItem> pines = new ArrayList<>();
        for (JsonNode pin : pinRes.body().path(KEY_DATA)) {
            String pinId = textoOpcional(pin.path("pinId"));
            String productCode = textoOpcional(pin.path("productCode"));
            String sku = textoOpcional(pin.path("sku"));
            String supplierCode = textoOpcional(pin.path("supplierCode"));
            String name = textoOpcional(pin.path("name"));
            String summary = textoOpcional(pin.path("summary"));
            String url = textoOpcional(pin.path("url"));
            BigDecimal amount = parseDecimal(pin.path("amount"));

            CatalogoServicioItem item = new CatalogoServicioItem();
            item.setClaveUnica("PIN-" + subId + "-" + pinId + "-" + sku);
            item.setTipoOperacion(OP_PIN);
            item.setCategoria(tieneTexto(categoriaFull) ? categoriaFull : "Pines Digitales");
            item.setNombreServicio(tieneTexto(proveedorNombre) ? proveedorNombre : subDesc);
            item.setNombreProducto(tieneTexto(name) ? name : subDesc);
            item.setDescripcion(tieneTexto(summary) ? summary : "Pin digital " + name);
            item.setProveedorCodigo(supplierCode);
            item.setProveedorNombre(tieneTexto(subDesc) ? subDesc : proveedorNombre);
            item.setLogoUrl(logo);
            item.setIdPin(pinId);
            item.setIdSubcategoriaPin(subId);
            item.setCodigoProducto(productCode);
            item.setSku(sku);
            item.setPrecioSugerido(amount);
            item.setEtiquetaReferencia1("Numero celular para envio del PIN (10 digitos)");
            item.setRequiereValidacionReferencia(true);
            item.setTerminosUrl(url);
            pines.add(item);
        }
        return pines;
    }

    private CatalogoServiciosResponse consultarPinesPorTipo(String idTipoPin, String texto) {
        List<CatalogoServicioItem> items = consultarPinesDeTipo(idTipoPin, "Pines Digitales", "Seleccion", null);
        CatalogoBusquedaRequest filtro = new CatalogoBusquedaRequest();
        filtro.setTexto(texto);
        return respuestaSimple(filtrarServicios(items, filtro), Collections.emptyList());
    }

    private CatalogoServiciosResponse consultarPinesPorSubcategoria(String idSubcategoriaPin, String texto) {
        List<CatalogoServicioItem> items = consultarPinesSubcategoria(idSubcategoriaPin, null, null, "Pines Digitales", null);
        CatalogoBusquedaRequest filtro = new CatalogoBusquedaRequest();
        filtro.setTexto(texto);
        return respuestaSimple(filtrarServicios(items, filtro), Collections.emptyList());
    }

    private CatalogoServiciosResponse respuestaSimple(
            List<CatalogoServicioItem> items,
            List<CatalogoCategoriaItem> cats) {
        CatalogoServiciosResponse res = new CatalogoServiciosResponse();
        res.setCodigo(0);
        res.setMensaje("Catalogo PuntoRed consultado exitosamente");
        res.setFuenteDatos("PUNTORED_H2H_V2");
        res.setSaldoComercio(consultarSaldoComercio());
        res.setCategorias(cats);
        res.setServicios(items);
        return res;
    }

    private PuntoredHttpResult postAutenticado(String path, Object payload) {
        String token = tokenService.obtenerTokenValido();
        PuntoredHttpResult result = apiClient.postJson(path, payload, token);
        if (result.httpStatus() == 401) {
            tokenService.invalidarToken();
            String nuevoToken = tokenService.renovarToken();
            result = apiClient.postJson(path, payload, nuevoToken);
        }
        return result;
    }

    private List<CatalogoServicioItem> filtrarServicios(
            List<CatalogoServicioItem> origen,
            CatalogoBusquedaRequest filtro) {
        String tipo = normalizarTexto(filtro.getTipoOperacion());
        String cat = normalizarTexto(filtro.getCategoria());
        String texto = normalizarTexto(filtro.getTexto());

        List<CatalogoServicioItem> salida = new ArrayList<>();
        for (CatalogoServicioItem item : origen) {
            if (coincideFiltro(item, tipo, cat, texto)) {
                salida.add(item);
            }
        }
        return salida;
    }

    private boolean coincideFiltro(CatalogoServicioItem item, String tipo, String cat, String texto) {
        if (tieneTexto(tipo) && !"TODOS".equals(tipo)) {
            boolean esRecaudo = OP_RECAUDO.equals(tipo)
                    && (OP_RECAUDO.equals(item.getTipoOperacion()) || OP_RECAUDO_V2.equals(item.getTipoOperacion()));
            if (!esRecaudo && !tipo.equalsIgnoreCase(item.getTipoOperacion())) {
                return false;
            }
        }
        if (tieneTexto(cat) && !"TODAS".equals(cat)) {
            String catItem = normalizarTexto(item.getCategoria());
            if (!catItem.contains(cat)) {
                return false;
            }
        }
        if (tieneTexto(texto)) {
            return contiene(item.getNombreServicio(), texto)
                    || contiene(item.getNombreProducto(), texto)
                    || contiene(item.getDescripcion(), texto)
                    || contiene(item.getCategoria(), texto)
                    || contiene(item.getCodigoProducto(), texto)
                    || contiene(item.getIdConvenio(), texto)
                    || contiene(item.getNombreBanco(), texto);
        }
        return true;
    }

    private List<CatalogoCategoriaItem> filtrarCategorias(
            List<CatalogoCategoriaItem> origen,
            CatalogoBusquedaRequest filtro) {
        String tipo = normalizarTexto(filtro.getTipoOperacion());
        if (!tieneTexto(tipo) || "TODOS".equals(tipo)) {
            return origen;
        }
        List<CatalogoCategoriaItem> salida = new ArrayList<>();
        for (CatalogoCategoriaItem cat : origen) {
            if (tipo.equalsIgnoreCase(cat.getTipoOperacion())) {
                salida.add(cat);
            }
        }
        return salida;
    }

    private void agregarSiNoExiste(List<CatalogoServicioItem> servicios, CatalogoServicioItem nuevo) {
        boolean existe = servicios.stream()
                .anyMatch(s -> s.getClaveUnica() != null && s.getClaveUnica().equals(nuevo.getClaveUnica()));
        if (!existe) {
            servicios.add(nuevo);
        }
    }

    private String buscarNombreCategoriaRecaudo(List<CatalogoCategoriaItem> categorias, String catId) {
        for (CatalogoCategoriaItem c : categorias) {
            if (catId.equals(c.getIdCategoria()) && OP_RECAUDO.equals(c.getTipoOperacion())) {
                return c.getNombreCategoria();
            }
        }
        return null;
    }

    private CatalogoSnapshot leerDesdeRedis() {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(REDIS_KEY_SNAPSHOT);
            if (!tieneTexto(json)) {
                return null;
            }
            return objectMapper.readValue(json, CatalogoSnapshot.class);
        } catch (Exception ex) {
            log.debug("Redis no disponible al leer catalogo PuntoRed: {}", ex.getMessage());
            return null;
        }
    }

    private void guardarEnRedis(CatalogoSnapshot snapshot) {
        if (redisTemplate == null || snapshot == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(REDIS_KEY_SNAPSHOT, json, Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception ex) {
            log.debug("Redis no disponible al guardar catalogo PuntoRed: {}", ex.getMessage());
        }
    }

    private void invalidarCache() {
        memoriaCache.set(null);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_KEY_SNAPSHOT);
            } catch (Exception ex) {
                log.debug("No se pudo limpiar Redis para catalogo PuntoRed: {}", ex.getMessage());
            }
        }
    }

    private Integer pos() {
        return properties.getPointOfSale();
    }

    private Integer parseEnteroSeguro(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            String txt = node.asText("").trim();
            return txt.isEmpty() ? null : new BigDecimal(txt);
        } catch (Exception ex) {
            return null;
        }
    }

    private String construirDescripcionPaquete(String desc, String validity) {
        StringBuilder sb = new StringBuilder();
        if (tieneTexto(desc)) {
            sb.append(desc.trim());
        }
        if (tieneTexto(validity)) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("Vigencia: ").append(validity.trim());
        }
        return sb.toString();
    }

    private String construirDescripcionConvenio(String bank, String ref1, String ref2) {
        StringBuilder sb = new StringBuilder();
        if (tieneTexto(bank)) {
            sb.append("Entidad: ").append(bank.trim());
        }
        if (tieneTexto(ref1)) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("Ref: ").append(ref1.trim());
        }
        if (tieneTexto(ref2) && !"null".equalsIgnoreCase(ref2.trim())) {
            sb.append(" / ").append(ref2.trim());
        }
        return sb.toString();
    }

    private String normalizarUrlLogo(String raw) {
        if (!tieneTexto(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) ? trimmed : null;
    }

    private String textoOpcional(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) ? null : value.trim();
    }

    private String normalizarTexto(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean contiene(String campo, String busquedaUpper) {
        return campo != null && campo.toUpperCase(Locale.ROOT).contains(busquedaUpper);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private CatalogoSnapshot copiaConFuente(CatalogoSnapshot base, String fuente) {
        return new CatalogoSnapshot(fuente, base.saldoComercio, base.categorias, base.servicios, base.productCodesV2);
    }

    private static class CachedSnapshot {
        private final CatalogoSnapshot snapshot;
        private final Instant expiraEn;

        private CachedSnapshot(CatalogoSnapshot snapshot, Instant expiraEn) {
            this.snapshot = snapshot;
            this.expiraEn = expiraEn;
        }
    }

    public static class CatalogoSnapshot {
        public String fuenteDatos;
        public String saldoComercio;
        public List<CatalogoCategoriaItem> categorias = new ArrayList<>();
        public List<CatalogoServicioItem> servicios = new ArrayList<>();
        public Map<String, String> productCodesV2 = new LinkedHashMap<>();

        public CatalogoSnapshot() {
        }

        public CatalogoSnapshot(
                String fuenteDatos,
                String saldoComercio,
                List<CatalogoCategoriaItem> categorias,
                List<CatalogoServicioItem> servicios,
                Map<String, String> productCodesV2) {
            this.fuenteDatos = fuenteDatos;
            this.saldoComercio = saldoComercio;
            this.categorias = categorias;
            this.servicios = servicios;
            this.productCodesV2 = productCodesV2;
        }
    }
}
