#!/usr/bin/env bash
# Builds the launcher and publishes it for installs and in-app updates:
#   releases/launcher/XplayLauncher-<version>.apk  and  releases/launcher/latest.json
# Usage: tools/release-launcher.sh "What changed in this version"
set -euo pipefail
cd "$(dirname "$0")/.."
BRANCH=ccr-c1251b75-o5q4e9
RAW="https://raw.githubusercontent.com/caslu1/Xplay/$BRANCH/releases/launcher"

launcher/build.sh
CODE=$(sed -n 's/.*android:versionCode="\([0-9]*\)".*/\1/p' launcher/AndroidManifest.xml)
NAME=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' launcher/AndroidManifest.xml)
mkdir -p releases/launcher
find releases/launcher -name 'XplayLauncher-*.apk' ! -name "XplayLauncher-$NAME.apk" -delete
cp launcher/build/XplayLauncher.apk "releases/launcher/XplayLauncher-$NAME.apk"
python3 - "$CODE" "$NAME" "$RAW/XplayLauncher-$NAME.apk" "${1:-}" > releases/launcher/latest.json <<'PY'
import json, sys
code, name, apk, notes = sys.argv[1:5]
print(json.dumps({"versionCode": int(code), "versionName": name, "apk": apk, "notes": notes},
                 ensure_ascii=False, indent=2))
PY
echo "Published launcher $NAME ($CODE)"
cat releases/launcher/latest.json
