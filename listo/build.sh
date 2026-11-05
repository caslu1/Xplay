#!/usr/bin/env bash
# Builds build/Listo.apk without Gradle, using the Ubuntu/Debian packages:
#   apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
KEYSTORE=${KEYSTORE:-tools/listo.keystore}
KS_PASS=${KS_PASS:-listo123}
OUT=build

rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes"

aapt package -f -m -J "$OUT/gen" -M app/AndroidManifest.xml -S app/res -I "$ANDROID_JAR"

javac -nowarn --release 8 -encoding UTF-8 -classpath "$ANDROID_JAR" -d "$OUT/classes" \
    $(find app/src "$OUT/gen" -name '*.java')

dalvik-exchange --dex --min-sdk-version=21 --output="$OUT/classes.dex" "$OUT/classes"

aapt package -f -M app/AndroidManifest.xml -S app/res -I "$ANDROID_JAR" -F "$OUT/unsigned.apk"
(cd "$OUT" && aapt add unsigned.apk classes.dex >/dev/null)

zipalign -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias listo -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Listo" >/dev/null
fi
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --ks-key-alias listo \
    --out "$OUT/Listo.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/Listo.apk"
echo "OK: $OUT/Listo.apk"
