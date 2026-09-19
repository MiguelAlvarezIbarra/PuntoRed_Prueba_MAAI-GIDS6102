# DWP_Alvarez_Ibarra_GIDS6102_PagoServicio

Servicio backend en Spring Boot que integra con **GestoPago** para la sincronización de productos, y que además expone un módulo de gestión de **Personas** (CRUD). Proyecto académico (GIDS6102) de Miguel Álvarez Ibarra.

## Tecnologías

- Java 21
- Spring Boot 3
- Spring Data JPA / Hibernate
- PostgreSQL + HikariCP (pool `PagoServ-Pool`)
- Redis (Spring Data Redis + Lettuce) — cache de productos con fallback automático a PostgreSQL
- Flyway (migraciones)
- Lombok
- org.json (conversión XML → JSON de la respuesta de GestoPago)
- springdoc-openapi / Swagger UI

## Requisitos previos

- JDK 17+
- Gradle (wrapper incluido)
- PostgreSQL corriendo localmente (o accesible por red)
- Redis corriendo localmente (o accesible por red) — ver sección [Redis](#redis-cache-de-productos) para cómo levantarlo con Docker

## Configuración

El proyecto usa `src/main/resources/application.properties`. **No subas contraseñas reales a este archivo** — usa variables de entorno o un `application-local.properties` (ya ignorado en `.gitignore`). Ejemplo:

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5436/tu_base
spring.datasource.username=tu_usuario
spring.datasource.password=${DB_PASSWORD}

# GestoPago
gestopago.auth.url=${GESTOPAGO_URL}
gestopago.auth.id-distribuidor=${GESTOPAGO_ID_DISTRIBUIDOR}
gestopago.auth.codigo-dispositivo=${GESTOPAGO_CODIGO_DISPOSITIVO}
gestopago.auth.password=${GESTOPAGO_PASSWORD}
gestopago.auth.refresh-rate-ms=3600000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=1500ms
spring.data.redis.connect-timeout=1500ms
app.cache.productos.ttl-seconds=3600
```

## Cómo correr el proyecto

```bash
./gradlew clean build
./gradlew bootRun
```

La documentación interactiva de la API queda disponible en Swagger UI (por defecto en `http://localhost:8081/swagger-ui/index.html`).

## Módulos

### Productos (GestoPago)

- Sincroniza el catálogo de productos consumiendo `getProductList.do` de GestoPago (XML → JSON).
- Se sincroniza automáticamente al arrancar la app (`ApplicationReadyEvent`) y todos los días a las 3:00 am (`@Scheduled`).
- Los productos sincronizados se guardan en PostgreSQL, se cachean en Redis, y se exponen vía `/api/productos`.
- Los productos se devuelven siempre **ordenados por `tipoFront` ascendente** (0, 1, 2, 3...). Si `tipoFront` viene nulo desde GestoPago o ya existe así en la base, se trata como `0` tanto al guardar como al ordenar.

#### Cómo se convierte el XML a JSON

GestoPago responde en XML, no en JSON, así que hay que transformarlo antes de poder trabajarlo cómodamente en Java:

1. **Se pide el XML crudo.** La llamada a `getProductList.do` regresa un `String` con el XML completo (se guarda en `xmlResponse`).
2. **Se convierte todo de un jalón con la librería `org.json`.** En vez de parsear el XML tag por tag a mano, se usa `XML.toJSONObject(xmlResponse)`. Esa función recorre el árbol del XML y convierte automáticamente cada tag en una llave de JSON — por ejemplo, `<RESPONSE><PRODUCTOS>...</PRODUCTOS></RESPONSE>` se vuelve `{"RESPONSE": {"PRODUCTOS": {...}}}`. Una sola línea reemplaza lo que sería escribir un parser de XML propio.
3. **Se navega el JSON resultante como un `JSONObject` normal** hasta llegar a la llave `producto` (`.getJSONObject("RESPONSE").getJSONObject("PRODUCTOS")`).
4. **Se normaliza el caso de uno vs. varios productos.** XML no tiene el concepto nativo de "lista": si solo viene un `<producto>`, `org.json` lo convierte en un `JSONObject`; si vienen varios repetidos, lo convierte en un `JSONArray`. El código revisa con `instanceof` cuál de los dos casos llegó y arma siempre una `List<JSONObject>`, para que el resto de la lógica no le importe si GestoPago mandó uno o cien productos.
5. **Se leen los campos de cada producto con `opt...` (`optInt`, `optString`, etc.)** en vez de `get...`, porque no truenan si una llave no existe — regresan un valor por default.
6. **Cada JSON se mapea a la entidad `Producto`**, buscando primero si ya existe (por `idProducto` + `idServicio`) para actualizarlo en vez de duplicarlo, `tipoFront` nulo se normaliza a `0`, y se guarda todo junto con `saveAll()`.

#### Redis (cache de productos)

`GET /api/productos` sigue este flujo:

1. Intenta leer del cache de Redis (`productos:todos`).
2. Si hay datos en cache, los devuelve directamente (ya vienen ordenados, porque se guardan tal cual salieron de Postgres).
3. Si Redis no responde — sin conexión, sin internet, timeout — el error se atrapa y **cae automáticamente a PostgreSQL** sin que el cliente vea ningún error.
4. Si consultó Postgres, intenta repoblar el cache para la próxima consulta (si eso también falla, no rompe la respuesta).

Cada vez que corre `sincronizarProductos()` (al arrancar, en el cron diario, o vía `POST /api/productos/sync`), el cache se invalida (`DEL productos:todos`) para que la siguiente consulta traiga datos frescos de Postgres.

El manejo de Redis vive aislado en `ProductoCacheService`, separado de `ProductoService`, para no mezclar la lógica de negocio de productos con el manejo de cache.

**Levantar Redis en desarrollo (Docker):**

```bash
docker run -d --name redis-local -p 6379:6379 redis:7-alpine
```

Verificar que responde:

```bash
docker exec -it redis-local redis-cli ping
# PONG
```

### Respuestas estandarizadas

Los endpoints devuelven un formato consistente con código y mensaje (`ApiResponse<T>` + `ResponseCode`):

```json
{
  "codigo": 0,
  "mensaje": "Datos consultados correctamente",
  "data": [ /* ... */ ]
}
```

| Código | Significado |
|---|---|
| 0 | Datos consultados correctamente |
| 1 | Datos consultados de forma errónea |
| 2 | No se pudo autenticar con GestoPago |
| 3 | Tiempo de espera agotado al comunicarse con GestoPago |
| 4 | Error de comunicación con GestoPago |
| 5 | GestoPago respondió con un error |
| 6 | No se encontraron productos |

Las excepciones del módulo de GestoPago (autenticación, timeout, comunicación, respuesta inesperada) se traducen automáticamente a este formato mediante un `@RestControllerAdvice` (`GlobalExceptionHandler`), centralizando el manejo de errores en un solo lugar.

### Personas (CRUD)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/personas` | Crea una persona (el id se autogenera) |
| GET | `/personas` | Lista todas las personas |
| GET | `/personas/{codigo}` | Obtiene una persona por id |
| PUT | `/personasActualiza` | Actualiza una persona (solo los campos enviados; los omitidos no se tocan) |
| PUT | `/personasElimina` | Elimina una persona por id |

## Estado actual / pendientes

- [x] Bug de sincronización de productos (reportaba éxito sin guardar) — resuelto.
- [x] Orden y normalización de `tipoFront` (default `0`) al guardar y consultar productos.
- [x] Respuestas estandarizadas (código/mensaje) y manejo centralizado de excepciones.
- [x] Cache de productos en Redis con fallback automático a PostgreSQL.
- [x] Credenciales reales de distribuidor GestoPago agregadas (`id-distribuidor`, `codigo-dispositivo`, `password`) — actualmente son placeholders.
- [ ] Implementar el flujo de pagos de GestoPago (aún no existe).

## Base de datos

Tablas actuales: `flyway_schema_history`, `gestopago_tokens`, `personas`, `productos`.