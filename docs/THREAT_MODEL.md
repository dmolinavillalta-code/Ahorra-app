# Modelo de amenazas (STRIDE simplificado)

**Activos:** (1) datos de ahorro del usuario, (2) integridad de las cifras mostradas, (3) la clave de cifrado,
(4) la cadena de suministro y la firma de la app.
**Superficie:** archivo local cifrado, selector de archivos (importar/exportar), notificaciones, intents de la actividad.
**Fuera de la superficie (v1):** red, cuentas, pagos, backend.

| # | Amenaza | Mitigación en v1 | Riesgo residual |
|---|---|---|---|
| 1 | Otra app lee el archivo de datos | Almacenamiento privado de la app + AES-256-GCM con clave del Keystore | Dispositivo con root o malware con privilegios elevados puede leer memoria/UI mientras la app está abierta |
| 2 | Alteración del archivo (cambiar importes) | GCM autentica: cualquier byte modificado ⇒ error ⇒ `Unreadable` y no se sobrescribe (probado: `anyFlippedByteIsDetected`) | Quien tenga la clave (root) podría rehacerlo |
| 3 | Archivo importado malicioso o corrupto | Límite de tamaño (10 MB), decodificación estricta, validación de versión/duplicados/huérfanos/importes/fechas, recálculo de estados; si falla no cambia nada (probado en `LedgerTest`) | JSON válido pero absurdo dentro de los límites (p. ej. importes enormes pero ≤ máx.) |
| 4 | Pérdida de datos por fallo en mitad de la escritura | Archivo temporal + `fsync` + renombrado atómico; la UI solo muestra lo ya guardado | Fallo de almacenamiento: se informa y no se aplica el cambio |
| 5 | Pérdida de datos al cambiar de móvil o restaurar copia | `allowBackup=false`; exportar/importar manual; si no se puede descifrar, no se borra automáticamente | El usuario que no exporte pierde los datos al perder el móvil |
| 6 | Duplicados por doble toque o reintento | IDs idempotentes generados al abrir el formulario (idempotencia probada en el núcleo); cerrojo en el repositorio (**sin prueba automatizada todavía**) | — |
| 7 | Error de redondeo / pérdida de céntimos | Aritmética en enteros con reparto exacto (probado hasta 600 céntimos × 24 cuotas) | — |
| 8 | Fuga por capturas de pantalla / pantalla bloqueada | Notificaciones con versión pública sin importes | La app no bloquea capturas ni pide biometría (pendiente evaluar `FLAG_SECURE` y bloqueo opcional) |
| 9 | Fuga por logs o informes de fallo | No se registran datos financieros; no hay SDK de crash reporting | — |
| 10 | Exportación insegura | Aviso explícito: el archivo no está cifrado | El usuario puede guardarlo en un lugar inseguro (pendiente: exportación cifrada con contraseña) |
| 11 | Dependencia comprometida | Pocas dependencias (AndroidX, kotlinx); versiones fijadas en el catálogo | Falta activar Dependabot/escaneo de vulnerabilidades en el repositorio |
| 12 | APK manipulado y redistribuido | Firma de Google Play (App Signing); la clave de subida solo en secretos de CI | Distribución fuera de Play no verificable por el usuario |
| 13 | Intents maliciosos | Una sola actividad exportada (launcher) que no procesa extras ni datos externos | — |
| 14 | Ingeniería social con una "app de ahorro" que imita un banco | La UI no imita interfaces bancarias; el texto aclara que la app no mueve dinero | — |

## Fase 2: bloqueo, copias cifradas y retos
| Amenaza | Mitigación | Riesgo residual |
|---|---|---|
| Alguien coge el móvil desbloqueado | PIN de 6 dígitos (rechaza PIN triviales), huella/rostro opcional (BIOMETRIC_STRONG), bloqueo al salir (inmediato/30 s/5 min), arranque siempre bloqueado | El PIN es una barrera de interfaz: no es lo que cifra el archivo (lo hace la clave del Keystore) |
| Adivinar el PIN a base de intentos | PBKDF2 con sal; bloqueo progresivo (cada 5 fallos: 30 s, doblando hasta 15 min) guardado cifrado | Se basa en el reloj del sistema: adelantarlo lo acorta. Con un móvil con root se puede saltar la interfaz |
| Capturas y vista previa en apps recientes | Opción "Ocultar contenido" (`FLAG_SECURE`) | Es opcional; una cámara externa sigue pudiendo fotografiar la pantalla |
| Copia exportada robada | Exportación cifrada con contraseña (mínimo 8 caracteres, PBKDF2 210.000 + AES-GCM, cabecera autenticada) | Una contraseña débil se puede adivinar sin límite fuera del móvil; la contraseña olvidada no se recupera |
| Copia sin cifrar | Aviso explícito antes de guardarla | Decisión de la persona |
| Archivo de importación manipulado | Validación completa (`sanitizeImport`), tamaño máximo 10 MB, rechazo si algo no cuadra | — |
| Tarjeta de amigo falsificada o malformada | Se valida como entrada no fiable (especies conocidas, longitudes, sin caracteres de control, tamaño máximo); no ejecuta nada | No está firmada: puede mentir sobre el progreso. Solo sirve entre amigos, sin recompensas |
| Fuga de datos al compartir | La tarjeta no lleva importes, nombres de objetivos ni fechas; la persona elige destino y puede verla antes | Quien la recibe conoce tu nombre y tus mascotas |
| Presión psicológica de la gamificación | Sin dinero, sin azar, sin castigos; retos opcionales y sin consecuencia; romper una racha no quita nada | — |

## Detección y respuesta a incidentes
Con arquitectura sin servidor no hay un sistema que monitorizar. Procedimiento si se detecta una vulnerabilidad:
1. Recepción por el correo de contacto de la ficha de Play. 2. Triaje en 72 h. 3. Corrección y publicación de actualización
(versionCode ↑; Play permite despliegues escalonados y retirada). 4. Aviso en la ficha y, si hubiera datos de usuarios
afectados fuera del dispositivo (no aplica en v1), notificación a la AEPD en 72 h y a los afectados según RGPD art. 33-34.

## Lo que **no** se ha hecho
- Pruebas automáticas de `LockStore`/`LockController` (la lógica de PIN y bloqueo sí está probada en `core`; la capa Android no).
- Pruebas de penetración (requieren la app compilada y un tester humano; obligatorias antes del lanzamiento).
- Análisis estático/dinámico de la app (lint se ejecutará en CI; ver `LAUNCH_CHECKLIST.md`).
- Revisión de seguridad independiente del código.

No existe seguridad absoluta: este documento describe riesgos conocidos y su mitigación, no una garantía.

## Si se añaden pagos (modelo B/C)
Reabrir completamente: autenticación fuerte (SCA/PSD2), idempotencia y conciliación en servidor, webhooks firmados, tokens de
corta duración, *certificate pinning* con rotación, rate limiting, auditoría, separación dev/test/prod, gestión de secretos,
cumplimiento PCI (delegado en el proveedor: nunca almacenar PAN) y plan de respuesta a incidentes con SLA.
