#!/usr/bin/env bash
# Builds the web version of Gestor Simples: build/web (folder) and build/GestorSimples-web.zip.
# Publish by dragging build/web onto https://app.netlify.com/drop (or any static host with HTTPS).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build/web && mkdir -p build/web
cp assets/* build/web/
(cd build/web && rm -f ../GestorSimples-web.zip && python3 -c "import zipfile,os;z=zipfile.ZipFile('../GestorSimples-web.zip','w',zipfile.ZIP_DEFLATED);[z.write(f) for f in sorted(os.listdir('.'))];z.close()")
echo "OK: build/web e build/GestorSimples-web.zip"
