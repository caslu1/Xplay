# Minha Casa

App web (PWA) para ver e controlar os aparelhos da casa pelo celular. Ele usa a API oficial do SmartThings, então aparecem os mesmos aparelhos, cômodos e cenas que você já tem no app da Samsung.

## O que dá para fazer

- **Aparelhos**: lista por cômodo, com abas por cômodo e busca. Toque no ícone para ligar/desligar; toque no cartão para abrir os controles.
  - **Ar-condicionado**: liga/desliga, temperatura desejada (16–30 °C), modo (gelar, desumidificar, ventilar, auto, aquecer) e ventilação.
  - **TV**: liga/desliga, volume, mudo, canal +/− e fonte (HDMI etc.).
  - **Lâmpadas**: liga/desliga e brilho.
  - Aparelhos offline aparecem apagados, com o ícone de nuvem cortada.
  - **Lâmpadas**: liga/desliga, brilho (barra deslizante), tom de branco (quente, neutra, fria) e cor, quando a lâmpada tiver esses recursos.
- **Mapa 3D**: a casa em 3D de verdade (maquete com piso de madeira ou cerâmica, paredes, móveis, sombras). Lâmpadas acesas iluminam o cômodo na cor escolhida, o ar ligado mostra o vento saindo e a TV ligada acende a tela. Arraste para girar, use a pinça para zoom e para mover, toque num cômodo para entrar nele, toque duas vezes para voltar e toque num aparelho para controlar. As paredes da frente abaixam sozinhas e as paredes entre cômodos ficam a meia altura, para dar para ver dentro. **Planta** mostra a vista de cima.
- **Editar a planta**: arraste cômodos, móveis e aparelhos e puxe o ↘ para mudar o tamanho de um cômodo. **+ Móvel** abre um catálogo com 30 tipos (sofá, cama, guarda-roupa, mesa, geladeira, fogão, vaso, box, porta, janela…). Toque num móvel para girar ou excluir.
- **Criar cômodo com fotos (📷)**: tire de 2 a 4 fotos de um cômodo e a IA (Claude, da Anthropic) mede o espaço, reconhece os móveis (tipo, tamanho, cor e posição), as cores do piso e das paredes, e onde estão o ar, a TV e as lâmpadas. O app monta tudo em 3D. Depois dá para ajustar à mão em **Editar**.
- **Início**: quantos aparelhos estão ligados ou offline, a média de temperatura dos cômodos e o botão **Desligar tudo**.
- **Rotinas**: executa as cenas da sua conta SmartThings.
- **Casas**: toque em "Minha casa ▾" para trocar de casa, se a conta tiver mais de uma.
- Os dados são atualizados sozinhos a cada 30 segundos.

Sem token, o app abre em **modo demonstração** com aparelhos de exemplo (nada é enviado para a casa).

## Fotos → 3D: chave da API do Claude

A análise das fotos usa a API do Claude, que é paga por uso na sua conta da Anthropic (cerca de US$ 0,10 a 0,30 por cômodo).

1. Crie uma conta em <https://platform.claude.com>, adicione créditos e abra **Settings → API Keys → Create key**.
2. No app, cole a chave em **Menu → Fotos → 3D (IA)** (ou na própria tela de fotos, na primeira vez).

A chave fica salva só no celular. As fotos vão direto do celular para a Anthropic: são reduzidas para 1280 px e não ficam guardadas no app.

As medidas são estimativas a partir das fotos. Confira e ajuste em **Editar** se algo ficar fora do lugar.

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

1. Baixe [`releases/MinhaCasa-1.2.apk`](../releases/MinhaCasa-1.2.apk) no celular (no GitHub, abra o arquivo e toque em **Download raw file**).
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
- `casa/casa3d.js`: a vista 3D (cômodos, móveis, aparelhos e luzes)
- `casa/three.min.js`: biblioteca 3D [Three.js](https://threejs.org) r147 (licença MIT)
- `casa/sw.js`: guarda a tela para abrir rápido
- `casa-app/`: projeto Android (casca nativa que mostra `casa/index.html`)
- `build-casa.sh`: gera o APK
