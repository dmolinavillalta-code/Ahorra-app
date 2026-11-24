# Pendientes para el lanzamiento y costes

## Bloqueantes (sin esto no se debe publicar)
- [ ] **Primera ejecución de CI en verde** (compilación real de la app, lint, pruebas unitarias, pruebas de interfaz). Hoy la app Android **no se ha compilado**.
- [ ] **Revisión jurídica** de la política de privacidad y confirmación de que el modelo A no requiere autorización financiera en España.
- [ ] **Prueba manual en dispositivos reales** (al menos Android 8, 13 y 16; pantalla pequeña y tablet; claro/oscuro; fuente grande; TalkBack).
- [ ] **Pruebas de penetración / revisión de seguridad** independiente.
- [ ] Decidir y **comprobar el nombre** (disponibilidad en Play, marcas registradas, dominio). "Ahorra" es provisional.
- [ ] Cuenta de desarrollador de Play verificada y clave de subida creada.
- [ ] Verificar los requisitos vigentes de Play marcados **(verificar)** en `PLAY_STORE_GUIDE.md`.

## Recomendado
- [ ] Prueba automatizada del `AhorraRepository` (concurrencia, fallo de guardado) y de `StorageFormat` (ida y vuelta, JSON corrupto).
- [ ] Activar Dependabot y análisis de vulnerabilidades en el repositorio.
- [ ] Exportación cifrada con contraseña; bloqueo opcional con biometría; evaluar `FLAG_SECURE`.
- [ ] Auditoría de accesibilidad (etiquetas de contenido, tamaños táctiles, contraste del tema con cada acento).
- [ ] Capturas, icono final de marca y textos de la ficha en español e inglés.
- [ ] Retos personales voluntarios (previstos en el encargo, aún no implementados).
- [ ] Decidir la edad mínima y la declaración de público objetivo.

## Si se quiere ofrecer pagos reales (fase 2)
- [ ] Elegir proveedor regulado (iniciación de pagos / dinero electrónico / banca como servicio) y revisar su modelo contractual.
- [ ] Backend mínimo: autorización, idempotencia, conciliación, webhooks firmados, gestión de secretos, auditoría.
- [ ] Reabrir política de privacidad, modelo de amenazas, formulario de seguridad de datos y declaración financiera de Play.
- [ ] Revisión jurídica específica por país antes de habilitar cada jurisdicción.

## Costes
| Concepto | Tipo | Importe orientativo | Obligatorio en v1 |
|---|---|---|---|
| Cuenta de desarrollador de Google Play | Único | 25 USD **(verificar)** | Sí |
| GitHub + Actions | Gratis en repos públicos; cuota gratuita limitada en privados | 0 € | No (se puede compilar en local con Android Studio) |
| Hosting de la política de privacidad (GitHub Pages u otro) | Recurrente | 0 € | Sí (URL pública) |
| Dominio propio | Anual | ~10-15 € | No |
| Revisión jurídica | Único | Variable | Muy recomendable |
| Pruebas de penetración | Único | Variable (cientos–miles de €) | Muy recomendable |
| Servidor / backend | Recurrente | 0 € (no existe en v1) | No |
| Proveedor de pagos | Recurrente + comisión | Variable; contrato | Solo en fase 2 |

**Coste recurrente en v1: 0 €.** No hay servicios de pago integrados en el código.
