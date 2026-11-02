# Minha Casa

App web (PWA) para ver e controlar os aparelhos da casa pelo celular. Ele usa a API oficial do SmartThings, então aparecem os mesmos aparelhos, cômodos e cenas que você já tem no app da Samsung.

## O que dá para fazer

- **Aparelhos**: lista por cômodo, com abas por cômodo e busca. Toque no ícone para ligar/desligar; toque no cartão para abrir os controles.
  - **Ar-condicionado**: liga/desliga, temperatura desejada (16–30 °C), modo (gelar, desumidificar, ventilar, auto, aquecer) e ventilação.
  - **TV**: liga/desliga, volume, mudo, canal +/− e fonte (HDMI etc.).
  - **Lâmpadas**: liga/desliga e brilho.
  - Aparelhos offline aparecem apagados, com o ícone de nuvem cortada.
  - **Lâmpadas**: liga/desliga, brilho (barra deslizante), tom de branco (quente, neutra, fria) e cor, quando a lâmpada tiver esses recursos.
- **Mapa 3D**: planta da casa em 3D com cada aparelho no lugar. Arraste para girar, use a pinça para zoom e toque num aparelho para controlá-lo. O que está ligado brilha no chão (azul no ar, amarelo ou a cor da luz nas lâmpadas). **Planta** mostra a vista de cima. Em **Editar**, arraste os cômodos e os aparelhos e puxe o ↘ para mudar o tamanho de um cômodo. A planta fica salva no celular.
- **Início**: quantos aparelhos estão ligados ou offline, a média de temperatura dos cômodos e o botão **Desligar tudo**.
- **Rotinas**: executa as cenas da sua conta SmartThings.
- **Casas**: toque em "Minha casa ▾" para trocar de casa, se a conta tiver mais de uma.
- Os dados são atualizados sozinhos a cada 30 segundos.

Sem token, o app abre em **modo demonstração** com aparelhos de exemplo (nada é enviado para a casa).

## Lâmpadas de outras marcas

O app mostra tudo o que está na sua conta SmartThings. Lâmpadas de outras marcas aparecem aqui depois de ligadas ao SmartThings:

1. No app **SmartThings**, toque em **+ → Adicionar aparelho → Parceiros** (ou "Por marca").
2. Escolha a marca (Smart Life/Tuya, Positivo, Philips Hue, TP-Link Tapo, Xiaomi/Yeelight, Intelbras etc.) e entre com a conta do app da lâmpada.
3. Coloque cada lâmpada num cômodo. No Minha Casa, toque em **⋮ → Atualizar**.

Lâmpadas só Bluetooth, sem Wi-Fi nem hub, não aparecem na nuvem e não podem ser controladas pelo app.

## Conectar à sua casa

1. Abra <https://account.smartthings.com/tokens> e entre com a mesma conta Samsung do app SmartThings.
2. **Generate new token** → dê um nome → marque todas as permissões de **Devices**, **Locations** e **Scenes** → gere e copie o token.
3. No app, vá em **Menu → Conexão**, cole o token e toque em **Conectar à minha casa**.

O token fica salvo só no navegador do celular. Não compartilhe o link com o token, nem o token. Ele dá acesso aos seus aparelhos.

## Instalar o APK (Android)

1. Baixe [`releases/MinhaCasa-1.1.apk`](../releases/MinhaCasa-1.1.apk) no celular (no GitHub, abra o arquivo e toque em **Download raw file**).
2. Abra o arquivo e permita **Instalar apps desconhecidos** quando o Android pedir.
3. Abra **Minha Casa**, vá em **Menu → Conexão** e cole o token.

O APK roda o mesmo app desta pasta, mas as chamadas ao SmartThings saem pelo Android, sem passar pelas restrições do navegador. O botão Voltar fecha os painéis, e os toques vibram de leve.

Para gerar o APK de novo: `./build-casa.sh` (cria `build/casa/MinhaCasa.apk`). Ele é assinado com `tools/casa.keystore` (senha `casa123`). Use sempre essa chave para que as atualizações instalem por cima. Ao lançar uma versão nova, aumente `versionCode`/`versionName` em `casa-app/AndroidManifest.xml`.

## Abrir no navegador (opcional)

O app precisa estar num endereço `https://`. O jeito mais simples é o GitHub Pages:

1. No GitHub, abra o repositório → **Settings → Pages**.
2. Em **Source**, escolha **Deploy from a branch**, branch `main`, pasta `/ (root)` e salve.
3. Depois de 1–2 minutos o app fica em `https://<seu-usuário>.github.io/<repositório>/casa/`.
4. No Chrome do Android, abra esse endereço, toque em ⋮ → **Adicionar à tela inicial**. Ele abre em tela cheia, como um app.

Também dá para abrir `casa/index.html` direto no navegador do computador.

## Arquivos

- `casa/index.html`: o app inteiro (HTML, CSS e JavaScript, sem dependências)
- `casa/manifest.json`, `casa/icon.svg`: nome e ícone na tela inicial
- `casa/sw.js`: guarda a tela para abrir rápido
- `casa-app/`: projeto Android (casca nativa que mostra `casa/index.html`)
- `build-casa.sh`: gera o APK
