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

## Painel de clientes

Códigos de 9 dígitos, recargas, usuários dos aparelhos e a chave do TMDB: veja [docs/PAINEL.md](docs/PAINEL.md).

## Publicar uma versão

1. Aumente `versionCode` e `versionName` em `app/AndroidManifest.xml`.
2. Rode `tools/release.sh "o que mudou"`. Ele gera `releases/Xplay-<versão>.apk` e `releases/latest.json`.
3. Faça commit e push. Os apps instalados leem `latest.json` ao abrir e oferecem a atualização.

## Xplay Mobile (celular)

App separado, na pasta [`mobile/`](mobile), feito para celular: entra direto com **login Xtream** (servidor, usuário e senha) ou **link M3U**, sem o código do painel.

- Perfis ("Quem está assistindo?"), cada um com seus favoritos e o seu "Continuar assistindo"
- Início com destaques em carrossel, continuar assistindo (tempo restante, temporada e episódio) e fileiras por categoria
- Abas Filmes, Séries, Ao vivo, Favoritos e Ajustes; busca por filmes, séries e canais
- Detalhes com duração, ano, país, nota, gêneros, "Continuar T1:E6 em 8:27", trailer, favorito, temporadas e episódios
- Player: ±10s (também com toque duplo), bloqueio de tela, faixa de áudio, legendas, velocidade, formato da tela, lista de episódios, próximo episódio automático, brilho e volume deslizando o dedo; na TV ao vivo, troca de canal e programação atual

```sh
./mobile/build.sh   # gera mobile/build/XplayMobile.apk
```
