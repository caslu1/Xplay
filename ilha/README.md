# Ilha

Ilha ao redor da câmera frontal para Android 6+: música, chamadas, mensagens e atividades ao vivo.

- **Música**: capa e barras animadas na ilha; toque para abrir com barra de progresso e controles.
- **Mensagens de voz**: áudios do WhatsApp, WhatsApp Business e Telegram (e apps sem player) aparecem com o ícone do app e um cronômetro.
- **Notificações**: aparecem na ilha com os botões do app e resposta rápida (WhatsApp, Telegram etc.).
- **Chamadas**: atender/recusar pela ilha e cronômetro durante a ligação.
- **Duas atividades**: música + timer/download/navegação; toque na bolinha ao lado para trocar.
- **Encaixe na câmera**: detecção automática do recorte e ajuste fino de posição, tamanho e largura.
- **Por app**: escolha quais apps aparecem e atalho para desligar o pop-up do sistema (para não ver duas vezes).

## Instalar

1. Baixe `releases/Ilha-1.3.apk` e instale (permita "fontes desconhecidas").
2. Abra o Ilha e ative **Acesso às notificações** e **Acessibilidade**.
   No Android 13+, se a opção estiver cinza: Info do app → ⋮ → *Permitir configurações restritas*.
3. Em "Encaixe na câmera", toque em *Detectar automaticamente* e ajuste até o anel verde ficar em volta da câmera.

## Build

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
./build.sh   # gera build/Ilha.apk
```

Assinado com `tools/ilha.keystore` (senha `ilha1234`); use sempre a mesma chave para atualizar por cima.
