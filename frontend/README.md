# PuntoRed Frontend (Web + Android APK)

Interfaz gráfica corporativa construida con **React + TypeScript + Vite + Bootstrap 5 + Capacitor Android**, conectada a todos los endpoints del backend Spring Boot de PuntoRed.

## Requisitos
- Node.js 18+ (probado con v22)
- Para compilar APK: Java 17/21 y Android SDK (`ANDROID_HOME` o `%LOCALAPPDATA%\Android\Sdk`)

## Ejecución en modo Web (PC)
```bash
cd frontend
npm install
npm run dev
```

Compilación de producción Web (`dist/`):
```bash
npm run build
```

## Compilación de APK Android
```bash
cd frontend
npm run android:apk
```
El archivo instalable quedará generado en:
`frontend/android/app/build/outputs/apk/debug/app-debug.apk`

## Configuración de Entorno (Render / Local)
Por defecto apunta a producción en Render (`https://puntored-prueba-maai-gids6102.onrender.com`), ideal para usar el APK desde cualquier celular. Desde el botón **API / Servidor** en la pantalla de Login o barra superior puedes cambiar al instante entre Render, `http://localhost:8081` o cualquier URL personalizada.
