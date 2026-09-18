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

### Personas (CRUD)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/personas` | Crea una persona (el id se autogenera) |
| GET | `/personas` | Lista todas las personas |
| GET | `/personas/{codigo}` | Obtiene una persona por id |
| PUT | `/personasActualiza` | Actualiza una persona (solo los campos enviados; los omitidos no se tocan) |
| PUT | `/personasElimina` | Elimina una persona por id |

## Estado actual / pendientes

- [ ] Implementar el flujo de pagos de GestoPago (aún no existe).
- [ ] Bloqueado: faltan credenciales reales de distribuidor GestoPago (`id-distribuidor`, `codigo-dispositivo`, `password`) — actualmente son placeholders.

## Base de datos

Tablas actuales: `flyway_schema_history`, `gestopago_tokens`, `personas`, `productos`.