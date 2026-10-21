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

## Addon para Stremio

A pasta `stremio-addon/` tem um addon que leva a mesma lista Xtream (canais ao vivo, filmes e séries, com categorias, busca e guia "Agora") para dentro do Stremio. Precisa só do Node 18 ou mais novo, sem `npm install`.

```sh
node stremio-addon/server.js     # porta 7000; use PORT=8080 para mudar
```

Abra `http://<ip-do-servidor>:7000/configure`, preencha DNS, usuário e senha e toque em **Instalar no Stremio** (ou copie o link e cole na busca de addons do Stremio). O login vai codificado no link do addon, então um único servidor atende várias pessoas — e quem tiver o link tem o seu login.

### Publicar e pegar o link (Render, grátis)

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/caslu1/Xplay)

1. Toque no botão acima, entre (ou crie uma conta) no Render e confirme. O `render.yaml` já configura tudo.
2. Quando terminar, o Render mostra um endereço como `https://xplay-stremio.onrender.com`.
3. Abra `https://xplay-stremio.onrender.com/configure`, preencha DNS, usuário e senha e toque em **Copiar link do addon**. Esse é o seu link (`https://.../<código>/manifest.json`); cole na busca de addons do Stremio ou toque em **Instalar no Stremio**.

No plano grátis o servidor dorme após 15 minutos sem uso; a primeira abertura depois disso demora uns 30 segundos.

O Stremio só aceita addons remotos por **HTTPS** (`http://127.0.0.1` funciona apenas no mesmo aparelho). Para usar na TV ou no celular, publique o addon atrás de HTTPS, por exemplo num serviço como Render/Railway/Fly.io ou com um túnel (`cloudflared tunnel --url http://localhost:7000`).
