# Guía de publicación en Google Play

> Lo marcado **(verificar)** son requisitos que cambian con frecuencia o que no he podido comprobar en la consola:
> confírmalos en la documentación oficial de Play Console antes de actuar. Nada de esto está hecho todavía.

## Requisitos técnicos ya contemplados
- **Nivel de API objetivo:** `targetSdk = 36`. Comprobado en la documentación de Android (consultada el 2026-11-24): las apps nuevas y
  las actualizaciones enviadas desde el 31-08-2026 deben apuntar a Android 16 (API 36) o superior. **(verificar)** si ha aparecido API 37 como exigencia.
- **Formato:** Android App Bundle (`./gradlew :app:bundleRelease`).
- **Firma:** Play App Signing (Google guarda la clave de firma; tú subes con una *clave de subida*).
- **Permisos:** solo `POST_NOTIFICATIONS`. Ver `DATA_INVENTORY_AND_PERMISSIONS.md`.

## Pasos

### 1. Cuenta de desarrollador (manual, no automatizable)
1. Crea la cuenta en Play Console (pago único, hoy 25 USD **(verificar)**).
2. Completa la verificación de identidad (persona u organización). Una cuenta de **organización** exige número D-U-N-S **(verificar)**.
3. **(verificar)** Si es una cuenta personal nueva, Play puede exigir una **prueba cerrada** con un número mínimo de testers durante
   un número mínimo de días antes de pedir producción. Planifica este plazo.

### 2. Repositorio y CI
1. Sube el proyecto a GitHub. Activa Actions.
2. Ejecuta el workflow **CI**. La primera ejecución es la verificación real de compilación (ver `README.md`, "Qué no está verificado").
   Corrige lo que indique y repite hasta que los tres trabajos (`core-tests`, `app`, `ui-tests`) pasen.

### 3. Clave de subida
```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 upload.jks     # (macOS: base64 -i upload.jks)
```
Guarda `upload.jks` y sus contraseñas en un gestor de contraseñas **fuera del repositorio** (hay copia de seguridad: si la pierdes,
tendrás que pedir un reinicio de clave de subida a Google). Crea estos secretos en GitHub → Settings → Secrets → Actions:
`AHORRA_KEYSTORE_B64`, `AHORRA_KEYSTORE_PASSWORD`, `AHORRA_KEY_ALIAS`, `AHORRA_KEY_PASSWORD`.

### 4. Generar el AAB
Actions → **Release (AAB firmado)** → *Run workflow*. Descarga el artefacto `ahorra-release-aab`.
(La primera vez, Play te pedirá activar Play App Signing al subir el primer AAB.)

### 5. Ficha de la aplicación
- Título (≤ 30 caracteres; el nombre "Ahorra" es provisional: **comprueba disponibilidad y marcas** antes de usarlo), descripción corta y larga, capturas, icono 512×512, gráfico de funciones.
- **No** prometas rentabilidad, préstamos ni custodia de dinero. Texto claro: "Seguimiento de ahorro. No mueve dinero".
- Categoría: Finanzas **(verificar)** — las apps de esta categoría tienen declaraciones adicionales.
- Política de privacidad: publica `PRIVACY_POLICY_DRAFT.md` **ya revisada por un jurista** en una URL pública (p. ej. GitHub Pages) y enlázala en la ficha.
- Correo de contacto y, si es organización, dirección.

### 6. Declaraciones en Play Console (las responde una persona)
- **Seguridad de los datos:** propuesta en `DATA_INVENTORY_AND_PERMISSIONS.md`.
- **Funciones financieras / apps financieras (Financial features):** declara que la app es de seguimiento, sin préstamos, custodia ni pagos **(verificar el formulario vigente)**.
- **Clasificación de contenido (IARC), público objetivo y anuncios** (sin anuncios).
- **Acceso a la app:** no requiere cuenta ni credenciales (no hay que dar acceso a los revisores).
- **Eliminación de cuenta:** no hay cuentas; el borrado de datos es local **(verificar si hay que declararlo igualmente)**.

### 7. Pruebas y lanzamiento
1. Pista interna → pista cerrada → producción, con despliegue escalonado (p. ej. 10 % → 50 % → 100 %).
2. Revisa el informe previo al lanzamiento de Play (pruebas automáticas en dispositivos reales).
3. Tras el envío, la revisión de Google puede tardar días. Si te la rechazan, el motivo llega por correo y en el panel.

## Mantenimiento
- Sube `versionCode` en cada versión. Mantén `targetSdk` al día (Google sube el mínimo cada año, normalmente el 31 de agosto).
- Actualiza dependencias (`gradle/libs.versions.toml`) y vuelve a pasar CI antes de cada lanzamiento.
