package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Encapsula el acceso al cache de productos en Redis. Cualquier falla de
 * conexión (por ejemplo, sin internet) se absorbe aquí: el llamador nunca
 * ve una excepción de Redis, solo Optional.empty()/null como señal de
 * "no disponible, usa tu fuente de respaldo".
 */
@Component
@Slf4j
public class ProductoCacheService {

    private static final String REDIS_KEY_PRODUCTOS = "productos:todos";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.productos.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    public ProductoCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<Producto> obtener() {
        return ejecutarConFallback(
                () -> (List<Producto>) redisTemplate.opsForValue().get(REDIS_KEY_PRODUCTOS),
                "leer",
                null);
    }

    public void guardar(List<Producto> productos) {
        ejecutarConFallback(() -> {
            redisTemplate.opsForValue().set(REDIS_KEY_PRODUCTOS, productos, Duration.ofSeconds(cacheTtlSeconds));
            return null;
        }, "escribir en", null);
    }

    public void invalidar() {
        ejecutarConFallback(() -> {
            redisTemplate.delete(REDIS_KEY_PRODUCTOS);
            return null;
        }, "invalidar", null);
    }

    /**
     * Ejecuta una operación de Redis y, si falla por cualquier motivo
     * (sin conexión, timeout, error de deserialización, etc.), lo registra
     * como warning y devuelve el valor de respaldo en lugar de propagar
     * la excepción. Centraliza el manejo de errores de Redis en un único
     * lugar para no repetirlo en cada método.
     */
    private <T> T ejecutarConFallback(Supplier<T> operacion, String accion, T valorSiFalla) {
        try {
            return operacion.get();
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            log.warn("No se pudo {} en Redis (posible falla de conexión). Motivo: {}", accion, e.getMessage());
            return valorSiFalla;
        } catch (Exception e) {
            log.warn("Error inesperado al {} en Redis. Motivo: {}", accion, e.getMessage());
            return valorSiFalla;
        }
    }
}