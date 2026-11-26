# Política de privacidad — BORRADOR para revisión jurídica

> **Este texto es un punto de partida, no asesoramiento legal.** Debe revisarlo un profesional (RGPD, LOPDGDD, LSSI) antes
> de publicarse, y completarse con los datos reales de la persona o entidad responsable. Los campos `[ENTRE CORCHETES]` están pendientes.

**Última actualización:** [FECHA]
**Responsable:** [NOMBRE O RAZÓN SOCIAL], [NIF], [DIRECCIÓN], [CORREO DE CONTACTO]

## 1. Qué es Ahorra
Ahorra es una aplicación para planificar y llevar el seguimiento de objetivos de ahorro. **No es una entidad financiera,
no custodia dinero, no hace cargos ni transferencias.**

## 2. Qué datos se tratan y dónde
Los datos que introduces (nombre de tus objetivos, cantidades, fechas, notas de aportaciones, el nombre con el que quieras que te salude la app, retos y ajustes) **se guardan
únicamente en tu dispositivo**, cifrados. **No tenemos servidores que los reciban**, no hay cuentas de usuario y la app
no tiene permiso de acceso a Internet. Por eso, el responsable no recibe ni accede a esos datos.

La app **no** recoge: contactos, ubicación, fotos, identificadores publicitarios, identificadores del dispositivo,
datos de uso ni analítica, y no incluye publicidad ni SDK de terceros de seguimiento.

## 3. Finalidad y base jurídica
Prestarte las funciones de la app que has solicitado al instalarla y usarla (ejecución del servicio). Las notificaciones
de recordatorio solo se activan si tú lo decides (consentimiento) y puedes desactivarlas cuando quieras.

## 4. Permisos
**Notificaciones** (Android 13+), para los recordatorios, y únicamente si los activas. **Biometría** (huella o rostro), si activas el desbloqueo biométrico: lo gestiona el sistema Android y la app no recibe ni almacena ningún dato biométrico.

## 5. Exportación y archivos
Si exportas una copia, puedes protegerla con una contraseña (se cifra en tu móvil; no la conocemos y no se puede recuperar) o guardarla **sin cifrar**. El archivo se crea en la ubicación que elijas. Su custodia es tuya.
Si lo compartes o lo almacenas en un servicio de terceros, ese tratamiento se rige por las condiciones de ese servicio.

## 6. Conservación y borrado
Los datos permanecen en tu dispositivo mientras la app esté instalada. Puedes borrarlos en cualquier momento en
*Ajustes → Borrar todos mis datos* (elimina también la clave de cifrado) o desinstalando la app.

## 7. Tus derechos
Como no tratamos tus datos en nuestros sistemas, puedes ejercer tú mismo acceso, rectificación, portabilidad (exportar)
y supresión (borrar) desde la app. Para cualquier duda: [CORREO]. Puedes reclamar ante la Agencia Española de Protección
de Datos (www.aepd.es).

## 8. Menores
[A DECIDIR CON ASESOR: edad mínima de uso y declaración en Google Play.]

## 9. Seguridad
Cifrado AES-256-GCM con clave en el almacén seguro de Android. Bloqueo opcional con PIN (guardado solo como hash con sal) y huella, y opción de ocultar el contenido en capturas. Ninguna medida es infalible: consulta los riesgos residuales
en el documento técnico `THREAT_MODEL.md`.

## 10. Cambios
Si la app incorpora funciones que traten datos fuera del dispositivo (por ejemplo, cuentas o pagos), esta política se
actualizará **antes** de activarlas y se te informará.

---
### Notas para el revisor jurídico (no publicar)
- Confirmar que, al no recibir datos, el responsable no es "responsable del tratamiento" de los datos locales; aun así, publicar política y contacto.
- Si se añade backend, pagos o analítica: reabrir §2, §3, transferencias internacionales (cláusulas tipo/decisión de adecuación), encargados y registro de actividades.
- Procedimiento de brechas (72 h a la AEPD) solo aplicará si se tratan datos en servidores propios.
- Otras jurisdicciones (UE, Reino Unido, EE. UU., Latinoamérica): requisitos distintos; no activar la distribución sin revisión.
