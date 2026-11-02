# Minha Casa

App web (PWA) para ver e controlar os aparelhos da casa pelo celular. Ele usa a API oficial do SmartThings, então aparecem os mesmos aparelhos, cômodos e cenas que você já tem no app da Samsung.

## O que dá para fazer

- **Aparelhos**: lista por cômodo, com abas por cômodo e busca. Toque no ícone para ligar/desligar; toque no cartão para abrir os controles.
  - **Ar-condicionado**: liga/desliga, temperatura desejada (16–30 °C), modo (gelar, desumidificar, ventilar, auto, aquecer) e ventilação.
  - **TV**: liga/desliga, volume, mudo, canal +/− e fonte (HDMI etc.).
  - **Lâmpadas**: liga/desliga e brilho.
  - Aparelhos offline aparecem apagados, com o ícone de nuvem cortada.
- **Início**: quantos aparelhos estão ligados ou offline, a média de temperatura dos cômodos e o botão **Desligar tudo**.
- **Rotinas**: executa as cenas da sua conta SmartThings.
- **Casas**: toque em "Minha casa ▾" para trocar de casa, se a conta tiver mais de uma.
- Os dados são atualizados sozinhos a cada 30 segundos.

Sem token, o app abre em **modo demonstração** com aparelhos de exemplo (nada é enviado para a casa).

## Conectar à sua casa

1. Abra <https://account.smartthings.com/tokens> e entre com a mesma conta Samsung do app SmartThings.
2. **Generate new token** → dê um nome → marque todas as permissões de **Devices**, **Locations** e **Scenes** → gere e copie o token.
3. No app, vá em **Menu → Conexão**, cole o token e toque em **Conectar à minha casa**.

O token fica salvo só no navegador do celular. Não compartilhe o link com o token, nem o token. Ele dá acesso aos seus aparelhos.

## Abrir no celular

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
