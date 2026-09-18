# DWP_Alvarez_Ibarra_GIDS6102_PagoServicio

Servicio backend en Spring Boot que integra con **GestoPago** para la sincronización de productos, y que además expone un módulo de gestión de **Personas** (CRUD). Proyecto académico (GIDS6102) de Miguel Álvarez Ibarra.

## Tecnologías

- Java 21
- Spring Boot 3
- Spring Data JPA / Hibernate
- PostgreSQL + HikariCP (pool `PagoServ-Pool`)
- Flyway (migraciones)
- Lombok
- org.json (conversión XML → JSON de la respuesta de GestoPago)
- springdoc-openapi / Swagger UI

## Requisitos previos

- JDK 17+
- Maven 3.8+
- PostgreSQL corriendo localmente (o accesible por red)

## Configuración

El proyecto usa `src/main/resources/application.properties`. **No subas contraseñas reales a este archivo** — usa variables de entorno o un `application-local.properties` (ya ignorado en `.gitignore`). Ejemplo:

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5436/tu_base
spring.datasource.username=tu_usuario
spring.datasource.password=${DB_PASSWORD}

# GestoPago
gestopago.server=${GESTOPAGO_SERVER}
gestopago.context=${GESTOPAGO_CONTEXT}
gestopago.id-distribuidor=${GESTOPAGO_ID_DISTRIBUIDOR}
gestopago.codigo-dispositivo=${GESTOPAGO_CODIGO_DISPOSITIVO}
gestopago.password=${GESTOPAGO_PASSWORD}
```

## Cómo correr el proyecto

```bash
mvn clean install
mvn spring-boot:run
```

La documentación interactiva de la API queda disponible en Swagger UI (por defecto en `http://localhost:8080/swagger-ui/index.html`).

## Módulos

### Productos (GestoPago)

- Sincroniza el catálogo de productos consumiendo `getProductList.do` de GestoPago (XML → JSON).
- Se sincroniza automáticamente al arrancar la app (`ApplicationReadyEvent`) y todos los días a las 3:00 am (`@Scheduled`).
- Los productos sincronizados se guardan en PostgreSQL y se exponen vía `/api/productos`.

#### Cómo se convierte el XML a JSON

GestoPago responde en XML, no en JSON, así que hay que transformarlo antes de poder trabajarlo cómodamente en Java:

1. **Se pide el XML crudo.** La llamada a `getProductList.do` regresa un `String` con el XML completo (se guarda en `xmlResponse`).
2. **Se convierte todo de un jalón con la librería `org.json`.** En vez de parsear el XML tag por tag a mano, se usa `XML.toJSONObject(xmlResponse)`. Esa función recorre el árbol del XML y convierte automáticamente cada tag en una llave de JSON — por ejemplo, `<RESPONSE><PRODUCTOS>...</PRODUCTOS></RESPONSE>` se vuelve `{"RESPONSE": {"PRODUCTOS": {...}}}`. Una sola línea reemplaza lo que sería escribir un parser de XML propio.
3. **Se navega el JSON resultante como un `JSONObject` normal** hasta llegar a la llave `producto` (`.getJSONObject("RESPONSE").getJSONObject("PRODUCTOS")`).
4. **Se normaliza el caso de uno vs. varios productos.** XML no tiene el concepto nativo de "lista": si solo viene un `<producto>`, `org.json` lo convierte en un `JSONObject`; si vienen varios repetidos, lo convierte en un `JSONArray`. El código revisa con `instanceof` cuál de los dos casos llegó y arma siempre una `List<JSONObject>`, para que el resto de la lógica no le importe si GestoPago mandó uno o cien productos.
5. **Se leen los campos de cada producto con `opt...` (`optInt`, `optString`, etc.)** en vez de `get...`, porque no truenan si una llave no existe — regresan un valor por default.
6. **Cada JSON se mapea a la entidad `Producto`**, buscando primero si ya existe (por `idProducto` + `idServicio`) para actualizarlo en vez de duplicarlo, y se guarda todo junto con `saveAll()`.

### Personas (CRUD)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/personas` | Crea una persona (el id se autogenera) |
| GET | `/personas` | Lista todas las personas |
| GET | `/personas/{codigo}` | Obtiene una persona por id |
| PUT | `/personasActualiza` | Actualiza una persona (solo los campos enviados; los omitidos no se tocan) |
| PUT | `/personasElimina` | Elimina una persona por id |

## Estado actual / pendientes

- [ ] Investigar bug: la sincronización de productos reporta éxito pero no se ven productos guardados.
- [ ] Implementar el flujo de pagos de GestoPago (aún no existe).
- [ ] Bloqueado: faltan credenciales reales de distribuidor GestoPago (`id-distribuidor`, `codigo-dispositivo`, `password`) — actualmente son placeholders.

## Base de datos

Tablas actuales: `flyway_schema_history`, `gestopago_tokens`, `personas`, `productos`.