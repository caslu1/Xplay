# Damas

Jogo de damas para Android (celular e Android TV), com as regras brasileiras:
tabuleiro 8x8, captura obrigatória, pedra captura para trás, dama voadora e lei da
maioria (é obrigatório capturar o maior número de peças).

- Contra o celular (Fácil, Médio, Difícil) ou dois jogadores no mesmo aparelho
- Desfazer lance, placar de vitórias/derrotas
- Lembretes locais para voltar a jogar (1, 3 e 7 dias depois de sair do app; sem internet)
- Captura múltipla: toque na peça e depois no destino final (ou casa a casa)
- Controle remoto: setas movem o cursor, OK seleciona

## Build

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
./build.sh   # gera build/Damas.apk
```

O APK pronto está em `releases/Damas-1.1.apk`.
