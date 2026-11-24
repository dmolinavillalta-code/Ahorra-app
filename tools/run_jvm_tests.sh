#!/usr/bin/env bash
# Ejecuta las pruebas del núcleo y del cifrado SIN Gradle ni Android SDK:
# solo necesita JDK 17+ y el compilador de Kotlin (se descarga de GitHub Releases).
# Es un sustituto para entornos sin acceso a Google Maven/Gradle; la verificación oficial es `./gradlew test` en CI.
set -euo pipefail
KOTLIN_VERSION="${KOTLIN_VERSION:-2.4.21}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${WORK_DIR:-$ROOT/build/jvm-harness}"
mkdir -p "$WORK"

if [ ! -x "$WORK/kotlinc/bin/kotlinc" ]; then
  echo "Descargando kotlinc $KOTLIN_VERSION..."
  curl -fsSL -o "$WORK/kotlinc.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip"
  unzip -q -o "$WORK/kotlinc.zip" -d "$WORK"
fi

OUT="$WORK/out"; rm -rf "$OUT"; mkdir -p "$OUT"
STDLIB="$WORK/kotlinc/lib/kotlin-stdlib.jar"
"$WORK/kotlinc/bin/kotlinc" -Xallow-kotlin-package -no-stdlib -cp "$STDLIB" -d "$OUT" \
  "$ROOT/core/src/main/kotlin" "$ROOT/core/src/test/kotlin" \
  "$ROOT/app/src/main/kotlin/es/ahorra/app/data/PayloadCipher.kt" \
  "$ROOT/app/src/test/kotlin/es/ahorra/app/data/AesGcmPayloadCipherTest.kt" \
  "$ROOT/tools/jvm-harness/KotlinTestStub.kt" "$ROOT/tools/jvm-harness/Runner.kt" 2> >(grep -v '^warning:' >&2)
java -cp "$OUT:$STDLIB" RunnerKt "$OUT"
