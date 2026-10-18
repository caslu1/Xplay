#!/usr/bin/env bash
# Builds the APK and publishes it for in-app updates:
#   releases/Xplay-<version>.apk  and  releases/latest.json (read by Updater.java).
# Usage: tools/release.sh "What changed in this version"
set -euo pipefail
cd "$(dirname "$0")/.."
BRANCH=ccr-e02be4ba-gz9bjy
RAW="https://raw.githubusercontent.com/caslu1/Xplay/$BRANCH/releases"

./build.sh
CODE=$(sed -n 's/.*android:versionCode="\([0-9]*\)".*/\1/p' app/AndroidManifest.xml)
NAME=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' app/AndroidManifest.xml)
mkdir -p releases
find releases -name 'Xplay-*.apk' ! -name "Xplay-$NAME.apk" -delete
cp build/Xplay.apk "releases/Xplay-$NAME.apk"
python3 - "$CODE" "$NAME" "$RAW/Xplay-$NAME.apk" "${1:-}" > releases/latest.json <<'PY'
import json, sys
code, name, apk, notes = sys.argv[1:5]
print(json.dumps({"versionCode": int(code), "versionName": name, "apk": apk, "notes": notes},
                 ensure_ascii=False, indent=2))
PY
echo "Published $NAME ($CODE)"
cat releases/latest.json
