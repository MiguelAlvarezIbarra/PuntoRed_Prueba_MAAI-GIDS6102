# Plataforma de Pagos de Servicios — PuntoRed / GestoPago

Plataforma integral de pagos de servicios basada en la API **Puntored Servicios H2H - V2** (`https://services.preprodcxr.co/puntored/api/v1`), implementada con un backend unificado en **Spring Boot 3.3.6 (Java 17)** y dos interfaces con **paridad funcional completa**:
- **Aplicación Web:** React + TypeScript + Bootstrap 5 ([`frontend/`](frontend/))
- **Aplicación Móvil Android (APK):** Flutter + Material 3 ([`mobile_flutter/`](mobile_flutter/))

Ambas aplicaciones comparten el mismo backend, credenciales de usuario, cuentas bancarias de 18 dígitos, catálogo dinámico, validación obligatoria de referencias y trazabilidad completa de pagos.

---

## Enlaces y Documentación Relacionada

- **Backend en Render:** `https://puntored-prueba-maai-gids6102.onrender.com/swagger-ui/index.html`
- **Documento Técnico Completo:** [`docs/DOCUMENTO_TECNICO.md`](docs/DOCUMENTO_TECNICO.md)
- **Plantilla de Variables de Entorno:** [`.env.example`](.env.example)

---

## Funcionalidades Implementadas

1. **Autenticación Unificada Web y Móvil**
   - Registro público (`POST /clientes`), inicio de sesión (`POST /auth/login`), cierre de sesión (`POST /auth/logout`) y alta administrativa con selección de rol (`POST /admin/clientes`).
   - Sesión protegida por JWT cifrado en BD (`AES-256-GCM`), hash `BCrypt`, expiración por inactividad y cierre de sesión automático ante HTTP `401` / código `9`.
   - Verificación opcional de dispositivo (**2FA del equipo / biometría**) al iniciar sesión y en Dashboard: **WebAuthn (`PublicKeyCredential`)** en navegador (Windows Hello, PIN/contraseña del equipo, Touch ID) y **`local_auth` + `flutter_secure_storage`** en Android.
2. **Catálogo Dinámico de Productos y Servicios (`GET /api/catalogo`, `POST /api/catalogo/buscar`)**
   - Consulta dinámica en **Puntored H2H V2** para módulos **Recargas, Paquetes, Recaudos y Pines Digitales**.
   - Búsqueda en tiempo real sobre todo el catálogo sincronizado, con visualización de logotipos oficiales, nombre, descripción, categoría, precio sugerido y respaldo en Redis/memoria.
3. **Consulta y Validación Obligatoria de Referencias (`POST /pagos/validar-referencia`)**
   - Validación de formato en cliente + verificación en vivo ante el proveedor antes de habilitar el paso de pago.
   - Emisión de `tokenValidacion` firmado con AES-GCM desde backend para impedir pagos sin referencia validada.
4. **Confirmación Explícita, 2FA Dual y Procesamiento de Pagos (`POST /pagos/puntored` + `POST /pagos/enviar-pin`)**
   - Resumen previo con logo, servicio, referencia, cuenta cargo e importe + checkbox de confirmación explícita.
   - **2FA seleccionable** en Web y Móvil:
     - **Equipo / Biometría** (`metodo2fa: "BIOMETRIA_MOVIL"`): Windows Hello / contraseña o PIN del equipo en Web (WebAuthn), o huella/rostro/PIN en Android.
     - **PIN de 6 dígitos por correo** (`POST /pagos/enviar-pin`): enviado con plantilla HTML vía **Resend / Brevo HTTP API** (compatible con Render Free) o SMTP, informando con claridad `correoEnviado`.
   - **Toggle transparente `Real (PuntoRed)` vs `Simulación`** en Web y Móvil (`simularPago`, gobernado en servidor por `PUNTORED_PERMITIR_SIMULACION`): permite ejecutar cobros reales en el sandbox PuntoRed o simular pagos exitosos para demos controladas, mostrando siempre la etiqueta visible **`Simulación`** en comprobante e historial.
   - Diferenciación estricta de estados: **`EXITOSA`** (folio/autorización, tirilla y botón de copiar comprobante), **`RECHAZADA`** y **`PENDIENTE` / `NO_CONFIRMADA`** (con re-consulta en tiempo real).
5. **Historial de Pagos, Cuentas y Paridad Web ↔ Móvil**
   - **`POST /pagos/historial`** y **`POST /pagos/detalle`**: historial aislado al usuario autenticado, filtros por estado/búsqueda, insignia `Simulación` cuando aplica, tirilla copiable y verificación de operaciones pendientes.
   - **`GET /cuentas/mias`**, **`POST /cuentas/consulta`**, **`POST /cuentas/saldo`**, **`POST /cuentas/abonar`**: consulta de cuentas de 18 dígitos y abono de saldo.
   - **`POST /clientes/buscar`** y **`PUT /clientes`** disponibles en ambos clientes para rol Administrador.

---

## Stack Tecnológico

- **Backend:** Java 17, Spring Boot 3.3.6, Spring Data JPA/Hibernate, PostgreSQL + Flyway `10.20.1` (V1–V11), Redis/Lettuce, JJWT `0.12.6`, BCrypt, `Correo2faService` (Resend / Brevo HTTPS + SMTP HTML), OpenAPI/Swagger UI, JUnit 5 + Mockito + JaCoCo.
- **Web Frontend:** React `18.3.1`, TypeScript `5.6.3`, Vite `6.0.3`, Bootstrap `5.3.3` + Bootstrap Icons, WebAuthn (`PublicKeyCredential` en `deviceAuthService.ts`).
- **Móvil Android:** Flutter (`mobile_flutter/`, Material 3, `http`, `shared_preferences`, `local_auth`, `local_auth_android`, `flutter_secure_storage`).

---

## Configuración Segura (Sin Credenciales en el Código)

1. Copia [`.env.example`](.env.example) a `.env` o crea `application-local.properties` (ambos ignorados por `.gitignore`).
2. Configura como mínimo:
   - Base de datos PostgreSQL (`DATABASE_URL`, `DB_USER`, `DB_PASSWORD`).
   - Credenciales Puntored H2H V2 (`PUNTORED_*`) y GestoPago (`GESTOPAGO_*`, `GESTOPAGO_TX_AES_KEY`, `GESTOPAGO_TX_AES_IV`).
   - Llaves de seguridad Base64 de 32+ bytes (`AES_LLAVE`, `JWT_LLAVE`).
   - Correo 2FA (`RESEND_API_KEY` o `BREVO_API_KEY` + `MAIL_FROM`, y/o SMTP `MAIL_*`) y bandera de pruebas `PUNTORED_PERMITIR_SIMULACION`.

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
> **Despliegue en Render (Static Site gratuito):** crea un **Static Site** apuntando al mismo repositorio con **Root Directory** = `frontend`, **Build Command** = `npm install && npm run build`, **Publish Directory** = `dist`, y variable de entorno `VITE_API_BASE_URL=https://puntored-prueba-maai-gids6102.onrender.com` (asegura tener el origen en `CORS_ALLOWED_ORIGINS` o `*` en el backend). Render sirve HTTPS automático, lo cual es requisito del navegador para habilitar WebAuthn (`Windows Hello` / contraseña del equipo / Touch ID).

### 3. Aplicación Móvil Flutter (`mobile_flutter/`)
```bash
cd mobile_flutter
flutter pub get
flutter analyze --no-fatal-infos
flutter test
flutter build apk --release
```
> El archivo APK compilado se genera en `mobile_flutter/build/app/outputs/flutter-apk/app-release.apk`. En la pantalla de inicio de sesión o en la barra superior es posible elegir entre **Render Cloud**, **Emulador Android (`http://10.0.2.2:8081`)** o **Localhost**.
