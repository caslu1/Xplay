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

---

# Xplay Launcher (TV Box / Fire TV / Android TV)

Tela inicial personalizável para TV, controlada por um painel web. Tudo fica no GitHub: o painel grava `config/config.json` no repositório e o launcher lê esse arquivo.

**Baixar o APK:** [`releases/launcher/XplayLauncher-1.0.apk`](releases/launcher/XplayLauncher-1.0.apk)

## O que o launcher tem

- Topo com logo, botão ⟳ (sincroniza com o painel), configurações, ícone de rede e relógio
- Fundo e logo definidos no painel, banner de propaganda rotativo, botões principais coloridos
- Fileira de apps do painel + apps que o usuário adiciona com **+** (tela "Selecionar App"); toque longo para remover, ver informações ou desinstalar
- Loja de apps: instala e atualiza APKs cadastrados no painel
- Gestão de dispositivos: cada aparelho tem um código (`XXXX-XXXX-XXXX`). No modo "somente cadastrados", aparelhos fora da lista ou vencidos veem a tela de bloqueio com o código, os planos e o contato de suporte
- Temas de cores, PIN para as configurações, atualização automática do próprio launcher
- Funciona offline com a última configuração baixada

Para usar como tela inicial: instale, aperte HOME e escolha **Xplay Launcher → Sempre** (ou Configurações do launcher → Launcher padrão). No Fire TV o sistema não deixa trocar a tela inicial sem ferramentas extras; o launcher abre normalmente pela lista de apps.

## Painel

O painel fica em `docs/` e é publicado pelo GitHub Pages em **https://caslu1.github.io/Xplay/**.

Para ativar (uma vez só): no GitHub, abra o repositório → **Settings** → **Pages** → em *Build and deployment* escolha **Source: Deploy from a branch**, **Branch: `ccr-c1251b75-o5q4e9`** e pasta **`/docs`** → **Save**. Em 1–2 minutos o endereço acima abre.

Entre com o repositório, a branch e um token do GitHub (fine-grained, só este repositório, **Contents: Read and write**). O token fica só no seu navegador. Edite e clique em **Publicar**; as TVs aplicam ao abrir o launcher, no ⟳ ou a cada X minutos.

Seções: Dashboard, Branding (Fundo/Logo), Propaganda, Botões Principais, Grade de Apps, Loja de Apps, Gestão de Dispositivos, Planos e Preços, Configurações (inclui editor JSON e backup), Gerenciar Temas.

## Build do launcher

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
launcher/build.sh                       # gera launcher/build/XplayLauncher.apk
tools/release-launcher.sh "novidades"   # publica em releases/launcher/ (aumente versionCode antes)
```

O workflow `.github/workflows/launcher.yml` compila o APK a cada push (artefato `XplayLauncher-apk`) e tira prints no emulador, publicados na branch `launcher-screenshots`.
