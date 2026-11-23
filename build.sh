#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
GEN_DIR="$BUILD_DIR/gen"
OBJ_DIR="$BUILD_DIR/obj"
RES_DIR="$BUILD_DIR/res"
DEX_DIR="$BUILD_DIR/dex"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
if [[ ! -d "$SDK_DIR" ]]; then
    echo "Erro: Android SDK nao encontrado. Defina ANDROID_HOME." >&2
    exit 1
fi

BUILD_TOOLS_DIR="$(ls -d "$SDK_DIR/build-tools/"* 2>/dev/null | sort -V | tail -n 1)"
PLATFORM_DIR="$(ls -d "$SDK_DIR/platforms/android-"* 2>/dev/null | sort -V | tail -n 1)"

if [[ -z "$BUILD_TOOLS_DIR" || -z "$PLATFORM_DIR" ]]; then
    echo "Erro: build-tools ou platforms nao encontrados em $SDK_DIR." >&2
    exit 1
fi

AAPT2="$BUILD_TOOLS_DIR/aapt2"
D8="$BUILD_TOOLS_DIR/d8"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
ANDROID_JAR="$PLATFORM_DIR/android.jar"

for asset in "assets/ARA.sqlite" "assets/hinario_lite.db" "assets/meditacoes_2026.json"; do
    if [[ ! -f "$ROOT_DIR/$asset" ]]; then
        echo "Erro: asset ausente: $asset. Execute python3 scripts/prepare_assets.py" >&2
        exit 1
    fi
done

echo "==> SDK: $SDK_DIR"
echo "==> Build-tools: $(basename "$BUILD_TOOLS_DIR") | Platform: $(basename "$PLATFORM_DIR")"

rm -rf "$BUILD_DIR"
mkdir -p "$GEN_DIR" "$OBJ_DIR" "$RES_DIR" "$DEX_DIR"

echo "==> 1/6 Compilando recursos XML (aapt2 compile)..."
"$AAPT2" compile --dir "$ROOT_DIR/res" -o "$RES_DIR/resources.zip"

echo "==> 2/6 Empacotando recursos e assets sem compressao para .sqlite/.db (aapt2 link)..."
"$AAPT2" link \
    -o "$BUILD_DIR/unaligned.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$ROOT_DIR/AndroidManifest.xml" \
    -A "$ROOT_DIR/assets" \
    -0 sqlite \
    -0 db \
    --min-sdk-version 9 \
    --target-sdk-version 10 \
    --java "$GEN_DIR" \
    "$RES_DIR/resources.zip"

echo "==> 3/6 Compilando fontes Java (javac -source 8 -target 8)..."
mapfile -t JAVA_FILES < <(find "$ROOT_DIR/src" "$GEN_DIR" -name "*.java" | sort)
javac \
    -source 8 \
    -target 8 \
    -encoding UTF-8 \
    -Xlint:-options \
    -classpath "$ANDROID_JAR" \
    -d "$OBJ_DIR" \
    "${JAVA_FILES[@]}"

echo "==> 4/6 Gerando classes.dex compativel com Dalvik API 9 (d8 --min-api 9)..."
mapfile -t CLASS_FILES < <(find "$OBJ_DIR" -name "*.class" | sort)
"$D8" \
    --min-api 9 \
    --lib "$ANDROID_JAR" \
    --output "$DEX_DIR" \
    "${CLASS_FILES[@]}"

echo "==> 5/6 Inserindo classes.dex e alinhando APK (zipalign)..."
(cd "$DEX_DIR" && jar uf "$BUILD_DIR/unaligned.apk" classes.dex)
"$ZIPALIGN" -f 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

KEYSTORE="$ROOT_DIR/debug.keystore"
if [[ ! -f "$KEYSTORE" ]]; then
    echo "==> Gerando keystore de debug RSA/SHA1withRSA para Android 2.3..."
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -storepass android \
        -keypass android \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -sigalg SHA1withRSA \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

echo "==> 6/6 Assinando APK com v1 JAR signing (apksigner --min-sdk-version 9)..."
"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --min-sdk-version 9 \
    --v1-signing-enabled true \
    --v2-signing-enabled false \
    --v3-signing-enabled false \
    --v4-signing-enabled false \
    --out "$BUILD_DIR/kairos-lite-debug.apk" \
    "$BUILD_DIR/aligned.apk"

rm -f "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

APK_SIZE="$(du -h "$BUILD_DIR/kairos-lite-debug.apk" | cut -f1)"
echo "==> Build concluido com sucesso: build/kairos-lite-debug.apk ($APK_SIZE)"
