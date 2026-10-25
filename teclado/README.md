# Teclado Xplay

Teclado em português para o celular, no mesmo estilo do app Xplay. Instala o APK
`releases/TecladoXplay-1.0.apk`, abre o app e segue os 2 passos (ligar nos ajustes
e escolher como teclado atual).

## O que tem

- **Microfone / ditado** 🎤 — toque no microfone na barra de cima e fale; o que você
  diz vira texto (usa o reconhecimento de voz do próprio aparelho, em `pt-BR`).
- **Área de transferência** 📋 — tudo que você copia fica guardado aqui. Toque para
  colar, no alfinete para fixar, no X para apagar.
- **Redimensionar** ↕ — o botão de redimensionar abre − / + para deixar o teclado
  maior ou menor (60% a 160%). O tamanho fica salvo.
- **Histórico** 🕘 — o que foi digitado e enviado fica numa lista, para reusar.
- **5+ temas** 🎨 — Escuro, Claro, Oceano, Neon Roxo, Floresta e Pôr do Sol.
- Acentos segurando a letra, sugestões de palavra, linha de números opcional,
  emojis, vibração e espaço longo para trocar de teclado.

Tudo é guardado **somente no aparelho** (nada é enviado para fora). A gravação do
que é digitado/copiado pode ser desligada na tela inicial do app, e campos de senha
nunca são gravados.

## Build

Mesmo esquema do Xplay (sem Gradle):

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
./build.sh   # gera build/TecladoXplay.apk
```

Assinado com `../tools/xplay.keystore`.
