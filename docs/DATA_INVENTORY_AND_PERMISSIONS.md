# Inventario de datos y lista de permisos

## Datos tratados (v1)

| Dato | Para qué | Dónde | Cifrado | Sale del móvil | Se puede borrar |
|---|---|---|---|---|---|
| Nombre del objetivo (texto libre) | Mostrarlo | `files/ahorra.dat` | AES-256-GCM | No | Sí |
| Cantidad objetivo, moneda, plazo, día de pago | Calcular el plan | idem | idem | No | Sí |
| Aportaciones (importe, fecha, nota, tipo) | Progreso e historial | idem | idem | No | Sí |
| Ajustes (recordatorios, hora, acento, nombre opcional para saludar, bienvenida vista) | Preferencias | idem | idem | No | Sí |
| Icono del objetivo y retos aceptados | Personalización y motivación | idem | idem | No | Sí |
| PIN (solo sal aleatoria + hash PBKDF2-HMAC-SHA256, 100.000 iteraciones), ajustes de bloqueo y contador de intentos | Bloqueo de la app | `files/lock.dat` | AES-256-GCM (misma clave del Keystore) | No | Sí (quitar el PIN o borrar datos) |
| Clave AES | Cifrar el archivo | Android Keystore | Protegida por hardware si existe | No (no exportable) | Sí (al borrar datos) |
| Tarea periódica de recordatorio | Avisos | WorkManager (base interna del sistema) | — (sin datos personales) | No | Sí (al borrar o desactivar) |

**No se recoge:** nombre real, correo, teléfono, contactos, ubicación, fotos, micrófono, cámara, identificadores del
dispositivo o publicitarios, datos bancarios, números de tarjeta, analítica, informes de errores a terceros.

**Logs:** el código no escribe importes ni nombres de objetivos en el log (comprobable con
`grep -rn "Log\." app/src` → sin resultados). Las notificaciones usan versión pública sin importes en pantalla bloqueada.

**Datos en el archivo exportado:** la exportación recomendada va **cifrada con una contraseña** que elige la persona (PBKDF2 210.000 iteraciones + AES-256-GCM; la contraseña no se guarda ni se puede recuperar). También existe la opción de exportar en JSON **sin cifrar**, con aviso previo.

**Pantalla:** con "Ocultar contenido" activado se usa `FLAG_SECURE` (sin capturas ni vista previa en apps recientes).

## Permisos

| Permiso | Motivo | Cuándo se pide |
|---|---|---|
| `POST_NOTIFICATIONS` | Mostrar recordatorios | Solo al activar los recordatorios (Android 13+) |
| `USE_BIOMETRIC` | Desbloquear la app con huella o rostro (diálogo del sistema; la app nunca recibe datos biométricos) | Solo al activar la huella en Ajustes |

No se declaran: `INTERNET`, `ACCESS_NETWORK_STATE`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`
(WorkManager reprograma por sí mismo), ubicación, almacenamiento, contactos, cámara, micrófono.
El selector de archivos del sistema (exportar/importar) no necesita permisos.

## Formulario "Seguridad de los datos" de Google Play (propuesta, a verificar en la consola)
- ¿Recopila o comparte datos? → **No** (los datos nunca salen del dispositivo y no se transmiten).
- ¿Datos cifrados en tránsito? → no aplica (no hay tránsito).
- ¿Puede el usuario solicitar la eliminación? → la elimina él mismo en la app.
Revisar con el asistente de Play Console cualquier cambio de redacción antes de enviar. **No declarar nada que no se haya comprobado en el APK final** (`aapt dump permissions`, inspección de dependencias).
