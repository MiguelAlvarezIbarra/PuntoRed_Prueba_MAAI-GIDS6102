# Plataforma de Pagos de Servicios — PuntoRed / GestoPago

Plataforma integral de pagos de servicios basada en la API **Puntored Servicios H2H - V2** (`https://services.preprodcxr.co/puntored/api/v1`), implementada con un backend unificado en **Spring Boot 3.3.6 (Java 17)** y dos interfaces con **paridad funcional completa**:
- **Aplicación Web:** React + TypeScript + Bootstrap 5 ([`frontend/`](file:///c:/Hasani/PuntoRed_Prueba_MAAI-GIDS6102/frontend))
- **Aplicación Móvil Android (APK):** Flutter + Material 3 ([`mobile_flutter/`](file:///c:/Hasani/PuntoRed_Prueba_MAAI-GIDS6102/mobile_flutter))

Ambas aplicaciones comparten el mismo backend, credenciales de usuario, cuentas bancarias de 18 dígitos, catálogo dinámico, validación obligatoria de referencias y trazabilidad completa de pagos.

---

## Enlaces y Documentación Relacionada

- **Backend en Render:** `https://puntored-prueba-maai-gids6102.onrender.com/swagger-ui/index.html`
- **Documento Técnico Completo:** [`docs/DOCUMENTO_TECNICO.md`](file:///c:/Hasani/PuntoRed_Prueba_MAAI-GIDS6102/docs/DOCUMENTO_TECNICO.md)
- **Plantilla de Variables de Entorno:** [`.env.example`](file:///c:/Hasani/PuntoRed_Prueba_MAAI-GIDS6102/.env.example)

---

## Funcionalidades Implementadas

1. **Autenticación Unificada Web y Móvil**
   - Registro público (`POST /clientes`), inicio de sesión (`POST /auth/login`), cierre de sesión (`POST /auth/logout`) y alta administrativa con selección de rol (`POST /admin/clientes`).
   - Sesión protegida por JWT cifrado en BD (`AES-256-GCM`), hash `BCrypt`, expiración por inactividad y cierre de sesión automático ante HTTP `401` / código `9`.
2. **Catálogo Dinámico de Productos y Servicios (`GET /api/catalogo`, `POST /api/catalogo/buscar`)**
   - Consulta dinámica en **Puntored H2H V2** para módulos **Recargas, Paquetes, Recaudos y Pines Digitales**.
   - Visualización de logotipos oficiales, nombre, descripción, categoría, precio sugerido y respaldo en Redis/memoria.
3. **Consulta y Validación Obligatoria de Referencias (`POST /pagos/validar-referencia`)**
   - Validación de formato en cliente + verificación en vivo ante el proveedor antes de habilitar el paso de pago.
   - Emisión de `tokenValidacion` firmado con AES-GCM desde backend para impedir pagos sin referencia validada.
4. **Confirmación Explícita y Procesamiento de Pagos (`POST /pagos/puntored` + `POST /pagos/enviar-pin`)**
   - Resumen previo con logo, servicio, referencia, cuenta cargo e importe + checkbox de confirmación explícita + PIN 2FA de 6 dígitos enviado por correo.
   - Diferenciación estricta de estados: **`EXITOSA`** (folio/autorización y tirilla), **`RECHAZADA`** y **`PENDIENTE` / `NO_CONFIRMADA`** (con opción de re-consulta en tiempo real).
5. **Historial de Pagos y Cuentas Propias**
   - **`POST /pagos/historial`** y **`POST /pagos/detalle`**: historial aislado al usuario autenticado, filtros por estado/búsqueda, comprobante/ticket digital y verificación de operaciones pendientes.
   - **`GET /cuentas/mias`**, **`POST /cuentas/consulta`**, **`POST /cuentas/saldo`**, **`POST /cuentas/abonar`**: consulta de cuentas de 18 dígitos y abono de saldo.

---

## Stack Tecnológico

- **Backend:** Java 17, Spring Boot 3.3.6, Spring Data JPA/Hibernate, PostgreSQL + Flyway `10.20.1` (V1–V11), Redis/Lettuce, JJWT `0.12.6`, BCrypt, OpenAPI/Swagger UI, JUnit 5 + Mockito + JaCoCo.
- **Web Frontend:** React `18.3.1`, TypeScript `5.6.3`, Vite `6.0.3`, Bootstrap `5.3.3` + Bootstrap Icons.
- **Móvil Android:** Flutter (`mobile_flutter/`, Material 3, `http`, `shared_preferences`, `android/app/src/main/AndroidManifest.xml`).

---

## Configuración Segura (Sin Credenciales en el Código)

1. Copia [`.env.example`](file:///c:/Hasani/PuntoRed_Prueba_MAAI-GIDS6102/.env.example) a `.env` o crea `application-local.properties` (ambos ignorados por `.gitignore`).
2. Configura como mínimo:
   - Base de datos PostgreSQL (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
   - Credenciales Puntored H2H V2 (`PUNTORED_*`) y GestoPago (`GESTOPAGO_*`, `GESTOPAGO_AES_KEY`).
   - Llaves de seguridad Base64 de 32+ bytes (`AES_LLAVE`, `JWT_LLAVE`).

---

## Comandos de Ejecución, Pruebas y Compilación

### 1. Backend Spring Boot
```bash
# Ejecutar pruebas unitarias y reporte de cobertura JaCoCo
./gradlew test jacocoTestReport

# Levantar el servidor backend (por defecto puerto 8081)
./gradlew bootRun
```

### 2. Frontend Web (`frontend/`)
```bash
cd frontend
npm install
npm run dev      # Servidor de desarrollo en http://localhost:5173
npm run build    # Compilación TypeScript + bundle Vite en frontend/dist/
```

### 3. Aplicación Móvil Flutter (`mobile_flutter/`)
```bash
cd mobile_flutter
flutter pub get
flutter analyze --no-fatal-infos
flutter test
flutter build apk --release
```
> El archivo APK compilado se genera en `mobile_flutter/build/app/outputs/flutter-apk/app-release.apk`. En la pantalla de inicio de sesión o en la barra superior es posible elegir entre **Render Cloud**, **Emulador Android (`http://10.0.2.2:8081`)** o **Localhost**.
