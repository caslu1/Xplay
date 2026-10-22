# Xplay

Player IPTV simples para Android (celular e Android TV). Você entra com login Xtream (DNS, usuário e senha) ou com o link de uma lista M3U, e o app mostra os canais por grupo.

- Busca por nome e filtro por grupo (`group-title`)
- Favoritos: toque longo no canal
- No player: deslize para cima/baixo (ou use as setas/CH+ CH- do controle) para trocar de canal
- Menu ⋮: recarregar ou trocar a lista / login

## Build

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
./build.sh   # gera build/Xplay.apk
```

O APK é assinado com `tools/xplay.keystore` (senha `xplay123`). Use sempre a mesma chave para que as atualizações instalem por cima da versão anterior.

## Publicar uma versão

1. Aumente `versionCode` e `versionName` em `app/AndroidManifest.xml`.
2. Rode `tools/release.sh "o que mudou"`. Ele gera `releases/Xplay-<versão>.apk` e `releases/latest.json`.
3. Faça commit e push. Os apps instalados leem `latest.json` ao abrir e oferecem a atualização.

## Leitor PDF

A pasta [`pdfreader/`](pdfreader/) contém um app separado: um leitor de PDF nativo, somente leitura.
