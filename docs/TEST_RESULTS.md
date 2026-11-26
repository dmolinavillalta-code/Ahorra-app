# Resultados reales de pruebas

Fecha de ejecución: 2026-11-22. Entorno: contenedor Linux, OpenJDK 21, Kotlin 2.4.21 (compilador descargado de GitHub Releases).
Comando reproducible: `./tools/run_jvm_tests.sh`.

## Ejecutado (fase 2): 72 pruebas, 0 fallos
Fecha: 2026-11-26. Las 43 de la fase 1 más 29 nuevas: `SummaryAndSeriesTest` (totales por moneda sin mezclar divisas, serie real/plan), `ChallengesAndAchievementsTest` (retos, nuevos logros), `SecurityCryptoTest` (cifrado con contraseña, PIN, bloqueo progresivo) y `LedgerPhase2Test` (iconos, nombre, retos, importación). La app Android de la fase 2 se compila, pasa lint y pruebas unitarias en CI (GitHub Actions); la interfaz **no** se ha probado a mano en un dispositivo real.

Detalle de la fase 1 (43 pruebas):

| Suite | Pruebas | Qué cubre |
|---|---|---|
| `MoneyAndPlanTest` | 14 | Reparto exacto sin perder céntimos (0–600 € × 1–24 cuotas), parseo "1.500,25"/"1,500.50", rechazo de basura y exceso de decimales, formato por idioma/moneda, fechas (día 31 en meses cortos, bisiestos, cambio de año), 1.200 € en 12 meses = 100 € y en 8 = 150 €, 10 € en 3 meses = 3,34 + 3,33 + 3,33, próxima aportación con parciales/extras/atrasos, progreso en enteros |
| `LedgerTest` | 13 | Validación de campos, idempotencia (objetivo y aportación duplicados), no aceptar fechas futuras ni importes ≤ 0, completar y reabrir objetivo, cambio de plan, pausa y reanudación, borrado, ajustes, **importación de archivos no fiables** (huérfanos, duplicados, importes negativos, fechas futuras, versión) |
| `GamificationTest` | 9 | Racha (mes abierto, rotura, pausas que no rompen), niveles por nº de aportaciones, logros, cosméticos |
| `AesGcmPayloadCipherTest` | 7 | Ida y vuelta, el texto cifrado no contiene el original, IV nuevo en cada cifrado (200/200 distintos), **alterar cualquier byte se detecta**, truncado/vacío rechazado, otra clave no descifra |

Nota de método: las pruebas usan `kotlin.test`. Para ejecutarlas sin Gradle uso un *mini-ejecutor* propio (`tools/jvm-harness/`)
con una implementación mínima de las aserciones. Es un sustituto, **no JUnit**: la ejecución oficial es `./gradlew test` en CI.

## Comprobación estática ejecutada
`python3 tools/check_resources.py` → OK: 113 textos en español e inglés, todos los usados en el código existen en ambos idiomas,
argumentos de formato coherentes, XML bien formado, el manifiesto no declara `INTERNET` y solo declara `POST_NOTIFICATIONS`.

## NO ejecutado (y por qué)
| Qué | Motivo | Cómo se ejecutará |
|---|---|---|
| Compilación de la app Android (Compose, AGP) | El entorno no puede descargar el Android SDK ni las librerías (Google Maven bloqueado) | CI: `./gradlew :app:assembleDebug` |
| Versiones del catálogo (`libs.versions.toml`) | Comprobadas contra la documentación oficial, pero **no resueltas** | CI |
| Lint de Android | Requiere el SDK | CI: `:app:lintDebug` |
| Pruebas de interfaz (`HomeScreenTest`) | Requieren emulador | CI: job `ui-tests` |
| AAB firmado | Requiere SDK y tus claves | Workflow *Release* |
| `StorageFormat`, `EncryptedStateStore`, `AhorraRepository`, `ReminderWorker` | Sin pruebas escritas todavía (dependen de kotlinx.serialization/coroutines/Android) | Pendiente (ver `LAUNCH_CHECKLIST.md`) |
| Pruebas manuales en dispositivos, accesibilidad, rendimiento | Requieren dispositivos | Pendiente |
| Pruebas de penetración | Requieren la app compilada y un especialista | Pendiente |
