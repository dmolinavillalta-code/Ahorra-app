# Ahorra (nombre provisional)

App Android de **seguimiento de objetivos de ahorro**: eliges cuánto quieres ahorrar, para qué y en cuánto tiempo, y la app
calcula tu aportación mensual, registra tu progreso y te motiva con rachas, niveles y logros, sin azar ni presión.

- **Modelo A (v1):** la app **no mueve dinero**. El dinero sigue en tu banco; tú anotas lo que ahorras.
- **Privacidad:** sin cuenta, sin servidor, **sin permiso de Internet**, datos cifrados en el móvil (AES-256-GCM + Android Keystore).
- Español (por defecto) e inglés. Tema claro/oscuro. `minSdk 26`, `targetSdk 36`.

## Estado honesto

| | |
|---|---|
| Núcleo de negocio (`core/`) | ✅ Escrito, compilado y probado: **36 pruebas pasan** |
| Cifrado (`PayloadCipher.kt`) | ✅ Compilado y probado: **7 pruebas pasan** |
| Recursos Android (textos, manifiesto) | ✅ Verificados con `tools/check_resources.py` |
| **App Android (Compose, WorkManager, almacenamiento)** | ⚠️ **Escrita y revisada, pero NO compilada** (el entorno de desarrollo no pudo descargar el Android SDK) |
| Pruebas de interfaz, lint, AAB | ⚠️ No ejecutados: los hará CI |
| Publicación en Google Play | ❌ No realizada: requiere tu cuenta de desarrollador (ver `docs/PLAY_STORE_GUIDE.md`) |

Detalle completo en [`docs/TEST_RESULTS.md`](docs/TEST_RESULTS.md).

## Cómo compilar

**Opción 1: Android Studio (la más simple).** Abre la carpeta; sincroniza Gradle; ejecuta `app`.
**Opción 2: línea de comandos** (JDK 17 + Android SDK con `ANDROID_HOME`):
```bash
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
**Opción 3: GitHub Actions (sin instalar nada).** Sube el repositorio y ejecuta el workflow *CI*; el APK debug queda como artefacto.
**Solo el núcleo, sin Android SDK:** `./tools/run_jvm_tests.sh` (JDK 17+; descarga el compilador de Kotlin desde GitHub).

## Qué no está verificado (y dónde puede fallar la primera compilación)
Las versiones de `gradle/libs.versions.toml` salen de la documentación oficial (AGP 9.5.0, Gradle 9.6.0, Kotlin 2.4.21,
Compose BOM 2026.11.01…) pero **no se han resuelto**. Los puntos más probables de ajuste en el primer CI:
1. `compileSdk { version = release(37) { minorApiLevel = 1 } }` — si AGP rechaza la sintaxis, usar `compileSdk = 37` (o el valor que pida el mensaje).
2. Plugins de Kotlin (`compose`, `serialization`) junto a *Kotlin integrado* de AGP 9: no se aplica `kotlin-android`.
3. Dependencias con versión fijada a mano (`kotlinx-coroutines 1.10.2`, `kotlinx-serialization 1.9.0`, `androidx.test`): si hay una incompatibilidad, subir a la última.
4. Errores de compilación de Kotlin en `app/` (código nunca compilado): se corrigen con el mensaje del compilador.

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
