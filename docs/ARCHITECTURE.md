# Arquitectura y decisiones

## 1. Modelo financiero: comparación y decisión

| | A. Seguimiento | B. Proveedor regulado custodia fondos | C. Híbrido |
|---|---|---|---|
| El dinero está en… | La cuenta bancaria del usuario | Cuenta/monedero gestionado por un tercero autorizado | A al inicio; B como módulo opcional |
| Licencia para Ahorra | Ninguna prevista* | La tiene el proveedor; Ahorra debe encajar en su contrato y en su modelo (agente, plataforma, marca blanca…) | Solo cuando se active B |
| Cargos automáticos | No existen | Sí, con mandato SEPA o iniciación de pagos (PSD2) | Solo en B |
| Riesgo de pérdida de dinero por un fallo de la app | Nulo (la app no mueve dinero) | Alto: conciliación, duplicados, fraude, devoluciones | Acotado a B |
| Servidor propio necesario | No | Sí (autorización, idempotencia, conciliación, webhooks) | Solo para B |
| Datos personales tratados | Ninguno sale del móvil | Identidad, IBAN, KYC (por el proveedor) | Solo en B |
| Tiempo hasta publicar | Corto | Meses (contratos, onboarding del proveedor, revisión de Play) | Corto + fase 2 |

\* Es una valoración técnica, **no jurídica**: debe confirmarla un asesor antes del lanzamiento (ver `LAUNCH_CHECKLIST.md`).

**Decisión (v1): modelo A, con arquitectura preparada para C.**
Es el más sencillo, seguro y viable para publicar en España: Ahorra calcula el plan y registra lo que la persona
confirma haber ahorrado; las transferencias las hace la persona en su banco. Mantiene la esencia del producto
(objetivo, plan mensual, progreso, motivación) sin custodiar dinero ni hacer cargos.

### "Recibir el dinero al completar el objetivo"
En el modelo A el dinero **nunca sale de la cuenta del usuario**, así que no hay nada que "entregar": al llegar al
objetivo la app lo marca como cumplido y celebra. La entrega automática solo tiene sentido en el modelo B, y funcionaría así
(no implementado, no simulado):

1. El usuario abre una cuenta de ahorro/monedero con un proveedor regulado y autoriza (SCA) un mandato.
2. Cada aportación la inicia el proveedor (o Ahorra como PISP autorizado) con una **clave de idempotencia** por cuota.
3. El **servidor** (nunca el móvil) concilia con los webhooks del proveedor y es la fuente de verdad del saldo.
4. Al llegar al objetivo, el servidor pide al proveedor una transferencia a la cuenta del propio usuario (IBAN verificado).
5. Si el usuario cambia de móvil o reinstala, el saldo vive en el proveedor, no en el dispositivo.

Si no se consigue un proveedor viable, la alternativa que conserva la esencia es el modelo A.

## 2. Módulos

```
core/   Kotlin/JVM puro, sin dependencias. Reglas de negocio. Probado con 36 pruebas.
  Money.kt          Aritmética en unidades menores (Long). Reparto exacto sin pérdida de céntimos. Parseo "1.500,25".
  PlanCalculator.kt Calendario de cuotas, próxima aportación, fecha de fin, progreso (enteros).
  Ledger.kt         Operaciones puras estado -> estado: crear, aportar, editar plan, pausar, importar (validación).
  Gamification.kt   Racha, nivel, logros. Solo por constancia; sin pérdidas ni azar.
app/    Android (Compose + Material 3)
  data/             Cifrado AES-256-GCM (clave en Android Keystore), archivo atómico, repositorio con cerrojo.
  work/             Recordatorios diarios con WorkManager (sin alarmas exactas, sin permisos especiales).
  ui/               Pantallas: inicio, nuevo objetivo, detalle, ajustes.
```

**Por qué el núcleo es independiente de Android:** el dinero es la parte crítica; así se prueba en cualquier máquina, en
milisegundos y sin emulador, y es el módulo que reutilizaría un backend si algún día hay modelo B.

## 3. Decisiones técnicas

| Decisión | Motivo | Coste |
|---|---|---|
| Kotlin + Jetpack Compose + Material 3 | Stack recomendado por Android; UI declarativa y accesible | Ninguno |
| Sin backend ni cuenta en la v1 | Privacidad máxima, funciona sin conexión, sin coste de servidor | Sin sincronización entre móviles (se cubre con exportar/importar) |
| Un archivo JSON cifrado en vez de Room/SQLite | Volumen minúsculo (≤ 20.000 aportaciones), escritura atómica simple, sin dependencias | Si los datos crecieran mucho, migrar a Room |
| AES-256-GCM + Keystore | Cifrado autenticado: detecta cualquier alteración; la clave no se puede extraer | La clave no se restaura en otro móvil (por diseño; ver exportar) |
| `allowBackup=false` + reglas de extracción vacías | Un backup en la nube del archivo cifrado sería ilegible y filtraría metadatos | El usuario usa "Exportar mis datos" |
| Sin permiso INTERNET | La app **no puede** enviar datos fuera; verificable por cualquiera en el manifiesto | Sin funciones online |
| WorkManager periódico (24 h) | Sobrevive a cierres y reinicios; sin permisos de alarma exacta | La hora es aproximada |
| Dinero en `Long` (céntimos) | Sin errores de coma flotante | — |
| Aportaciones solo "confirmadas" | Nunca se muestra como ahorrado algo pendiente o simulado | El usuario debe anotar lo que hace |

## 4. Reglas de negocio importantes
- **Reparto exacto:** 100 € en 3 meses = 33,34 + 33,33 + 33,33. La suma es siempre el total.
- **Fechas:** el día 31 se ajusta al último día de los meses cortos. Si el día de pago ya pasó este mes, empieza el siguiente.
- **Idempotencia:** cada aportación y cada objetivo llevan un ID generado al abrir la pantalla; un doble toque o reintento no duplica nada.
- **Cambiar objetivo/plazo:** el plan se recalcula desde hoy sobre lo que falta; lo ahorrado no se toca.
- **Pausa:** al reanudar, el plazo se desplaza (cuotas sin cubrir se reparten desde hoy). Los meses en pausa no rompen la racha.
- **Gamificación ética:** racha por meses con aportación, nivel por número de aportaciones (no por importe), cosméticos por nivel.
  Sin azar, sin recompensas monetarias, sin pérdidas por romper la racha, sin temporizadores ni presión.
- **Importar:** el archivo se trata como no fiable: versión, duplicados, huérfanos, importes y fechas se validan; el estado de cada objetivo se recalcula.
- **Si no se puede descifrar** el archivo (p. ej. restauración en otro móvil), la app **no lo sobrescribe**: ofrece empezar de nuevo explícitamente.

## 5. Alcance de la v1 frente al encargo original

| Pedido | Estado |
|---|---|
| Usar sin registrarse | ✅ (no hay cuentas) |
| Crear cuenta | ❌ Diferido: no hay backend (ver modelo C) |
| Objetivo, motivo, plazo, plan mensual | ✅ |
| Aportaciones manuales | ✅ |
| Pagos automáticos autorizados / recibir el dinero / pagos fallidos / conciliación | ❌ No aplican en el modelo A; diseño en §1 |
| Progreso, historial, extras, pausas, reprogramación | ✅ |
| Recordatorios y notificaciones | ✅ (aportación pendiente; sin notificar "pagos realizados" porque la app no los ejecuta) |
| Gamificación (barras, rachas, niveles, logros, cosméticos, celebración) | ✅ (las animaciones son sencillas; no hay "retos" voluntarios aún) |
| Claro/oscuro, accesibilidad, español e inglés | ✅ parcial: falta auditoría de accesibilidad con TalkBack |
| Exportar / borrar datos | ✅ |
| Informes automáticos, copias de seguridad automáticas | ❌ Solo exportación manual |
| Multimoneda | ✅ (por objetivo; no hay conversión) |
| Internacionalización de reglas legales por país | ✅ Solo diseño: ver `LAUNCH_CHECKLIST.md` |
