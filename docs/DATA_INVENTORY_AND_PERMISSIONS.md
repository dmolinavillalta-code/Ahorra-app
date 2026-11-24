# Inventario de datos y lista de permisos

## Datos tratados (v1)

| Dato | Para qué | Dónde | Cifrado | Sale del móvil | Se puede borrar |
|---|---|---|---|---|---|
| Nombre del objetivo (texto libre) | Mostrarlo | `files/ahorra.dat` | AES-256-GCM | No | Sí |
| Cantidad objetivo, moneda, plazo, día de pago | Calcular el plan | idem | idem | No | Sí |
| Aportaciones (importe, fecha, nota, tipo) | Progreso e historial | idem | idem | No | Sí |
| Ajustes (recordatorios, hora, acento) | Preferencias | idem | idem | No | Sí |
| Clave AES | Cifrar el archivo | Android Keystore | Protegida por hardware si existe | No (no exportable) | Sí (al borrar datos) |
| Tarea periódica de recordatorio | Avisos | WorkManager (base interna del sistema) | — (sin datos personales) | No | Sí (al borrar o desactivar) |

**No se recoge:** nombre real, correo, teléfono, contactos, ubicación, fotos, micrófono, cámara, identificadores del
dispositivo o publicitarios, datos bancarios, números de tarjeta, analítica, informes de errores a terceros.

**Logs:** el código no escribe importes ni nombres de objetivos en el log (comprobable con
`grep -rn "Log\." app/src` → sin resultados). Las notificaciones usan versión pública sin importes en pantalla bloqueada.

**Datos en el archivo exportado:** el JSON de "Exportar mis datos" **no está cifrado**.

## Permisos

| Permiso | Motivo | Cuándo se pide |
|---|---|---|
| `POST_NOTIFICATIONS` | Mostrar recordatorios | Solo al activar los recordatorios (Android 13+) |

No se declaran: `INTERNET`, `ACCESS_NETWORK_STATE`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`
(WorkManager reprograma por sí mismo), ubicación, almacenamiento, contactos, cámara, micrófono.
El selector de archivos del sistema (exportar/importar) no necesita permisos.

## Formulario "Seguridad de los datos" de Google Play (propuesta, a verificar en la consola)
- ¿Recopila o comparte datos? → **No** (los datos nunca salen del dispositivo y no se transmiten).
- ¿Datos cifrados en tránsito? → no aplica (no hay tránsito).
- ¿Puede el usuario solicitar la eliminación? → la elimina él mismo en la app.
Revisar con el asistente de Play Console cualquier cambio de redacción antes de enviar. **No declarar nada que no se haya comprobado en el APK final** (`aapt dump permissions`, inspección de dependencias).
