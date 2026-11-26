# Ahorra (nombre provisional)

App Android de **seguimiento de objetivos de ahorro**: eliges cuánto quieres ahorrar, para qué y en cuánto tiempo, y la app
calcula tu aportación mensual, registra tu progreso y te motiva con rachas, niveles y logros, sin azar ni presión.

- **Modelo A (v1):** la app **no mueve dinero**. El dinero sigue en tu banco; tú anotas lo que ahorras.
- **Privacidad:** sin cuenta, sin servidor, **sin permiso de Internet**, datos cifrados en el móvil (AES-256-GCM + Android Keystore).
- Español (por defecto) e inglés. Tema claro/oscuro. `minSdk 26`, `targetSdk 36`.

## Estado honesto

| | |
|---|---|
| Núcleo de negocio (`core/`) | ✅ Compilado y probado: **72 pruebas pasan** (también en CI) |
| App Android (Compose) | ✅ **Compila, pasa lint y pruebas unitarias en GitHub Actions** |
| Interfaz en dispositivo real | ⚠️ Pruebas automáticas mínimas en emulador; **sin revisión manual en móviles distintos, accesibilidad ni rendimiento** |
| Capa Android de bloqueo (`LockStore`, `LockController`) | ⚠️ Sin pruebas automáticas (la lógica de PIN/bloqueo sí está probada en `core`) |
| APK de prueba | ✅ Firmado con una clave de **depuración** pública del repositorio: solo para uso personal |
| AAB de publicación y Google Play | ❌ No realizados: requieren tu clave de firma y tu cuenta de desarrollador (ver `docs/PLAY_STORE_GUIDE.md`) |

Fase 2: bienvenida y bloqueo (PIN de 6 dígitos, huella opcional, bloqueo automático, ocultar contenido), copias cifradas con
contraseña, rediseño con animaciones, gráfica de evolución, iconos por objetivo, niveles, logros y retos opcionales.

Detalle completo en [`docs/TEST_RESULTS.md`](docs/TEST_RESULTS.md).

## Cómo compilar

**Opción 1: Android Studio (la más simple).** Abre la carpeta; sincroniza Gradle; ejecuta `app`.
**Opción 2: línea de comandos** (JDK 17 + Android SDK con `ANDROID_HOME`):
```bash
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
**Opción 3: GitHub Actions (sin instalar nada).** Sube el repositorio y ejecuta el workflow *CI*; el APK debug queda como artefacto.
**Solo el núcleo, sin Android SDK:** `./tools/run_jvm_tests.sh` (JDK 17+; descarga el compilador de Kotlin desde GitHub).

## Estructura
```
core/            Reglas de negocio (Kotlin puro)           -> docs/ARCHITECTURE.md
app/             App Android (Compose)
docs/            Arquitectura, privacidad (borrador), inventario de datos, amenazas, guía de Play, pendientes, resultados
tools/           Verificador de recursos y ejecutor de pruebas sin Gradle
.github/         CI y release (AAB firmado con secretos)
```

## Documentación
[Arquitectura y modelo financiero](docs/ARCHITECTURE.md) · [Política de privacidad (borrador)](docs/PRIVACY_POLICY_DRAFT.md) ·
[Datos y permisos](docs/DATA_INVENTORY_AND_PERMISSIONS.md) · [Modelo de amenazas](docs/THREAT_MODEL.md) ·
[Guía de Google Play](docs/PLAY_STORE_GUIDE.md) · [Pendientes y costes](docs/LAUNCH_CHECKLIST.md) · [Resultados de pruebas](docs/TEST_RESULTS.md)

> Esto no es asesoramiento legal ni financiero. La política de privacidad y la viabilidad regulatoria requieren revisión de un profesional.
