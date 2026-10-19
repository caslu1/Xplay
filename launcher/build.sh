#!/usr/bin/env bash
# Builds build/XplayLauncher.apk without Gradle, using the Ubuntu/Debian packages:
#   apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
# The panel's config/config.json is bundled as the factory config used before the first sync.
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
KEYSTORE=${KEYSTORE:-../tools/xplay.keystore}
KS_PASS=${KS_PASS:-xplay123}
OUT=build

rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes"
mkdir -p res/raw && cp ../config/config.json res/raw/default_config.json

aapt package -f -m -J "$OUT/gen" -M AndroidManifest.xml -S res -I "$ANDROID_JAR"

javac -nowarn --release 8 -encoding UTF-8 -classpath "$ANDROID_JAR" -d "$OUT/classes" \
    $(find src "$OUT/gen" -name '*.java')

dalvik-exchange --dex --min-sdk-version=21 --output="$OUT/classes.dex" "$OUT/classes"

aapt package -f -M AndroidManifest.xml -S res -I "$ANDROID_JAR" -F "$OUT/unsigned.apk"
(cd "$OUT" && aapt add unsigned.apk classes.dex >/dev/null)

zipalign -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --ks-key-alias xplay \
    --out "$OUT/XplayLauncher.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/XplayLauncher.apk"
echo "OK: launcher/$OUT/XplayLauncher.apk"
