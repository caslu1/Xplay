#!/usr/bin/env bash
# Gera build/casa/MinhaCasa.apk (app Minha Casa) sem Gradle, com as mesmas ferramentas do Xplay:
#   apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
KEYSTORE=${KEYSTORE:-tools/casa.keystore}
KS_PASS=${KS_PASS:-casa123}
SRC=casa-app
OUT=build/casa

rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/assets"
cp casa/index.html casa/icon.svg casa/three.min.js casa/casa3d.js "$OUT/assets/"

aapt package -f -m -J "$OUT/gen" -M $SRC/AndroidManifest.xml -S $SRC/res -I "$ANDROID_JAR"

javac -nowarn --release 8 -encoding UTF-8 -classpath "$ANDROID_JAR" -d "$OUT/classes" \
    $(find $SRC/src "$OUT/gen" -name '*.java')

dalvik-exchange --dex --min-sdk-version=21 --output="$OUT/classes.dex" "$OUT/classes"

aapt package -f -M $SRC/AndroidManifest.xml -S $SRC/res -A "$OUT/assets" -I "$ANDROID_JAR" -F "$OUT/unsigned.apk"
(cd "$OUT" && aapt add unsigned.apk classes.dex >/dev/null)

zipalign -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias casa -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Minha Casa" >/dev/null
fi
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --ks-key-alias casa \
    --out "$OUT/MinhaCasa.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/MinhaCasa.apk"
echo "OK: $OUT/MinhaCasa.apk"
