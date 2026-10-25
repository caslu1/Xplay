# Xplay e Gestor Simples

Este repositório tem dois apps Android que usam o mesmo projeto Supabase:

| App | Pasta | Para que serve | Guia |
|---|---|---|---|
| **Xplay** (IPTV) | `app/`, `admin/` | Player IPTV para os clientes; o painel `admin/index.html` cadastra clientes, códigos e recargas | [docs/PAINEL.md](docs/PAINEL.md) |
| **Gestor Simples** | `gestor/` | Gestão de clientes, planos, vencimentos e financeiro da revenda, com login e dados na nuvem | [docs/GESTOR.md](docs/GESTOR.md) |

No Supabase, o Xplay usa as tabelas `clients`, `recharges`, `devices` e `config` (só administradores em `panel_admins`); o Gestor usa `gestor_data` (cada conta vê só a própria linha).

---

## Xplay

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
