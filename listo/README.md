# Listo

App de tarefas para Android com **visão de calendário** e **listas ilimitadas**.

- **Calendário**: mês inteiro (domingo primeiro), deslize para trocar de mês, ponto nos dias com tarefas, botão "hoje". Embaixo, as tarefas do dia com horário e bandeira de prioridade. Toque na bolinha para concluir, no cartão para editar, segure para excluir.
- **Listas**: cartões coloridos (estudos, compras, filmes…), com ícone. Toque no item para marcar, no × para remover, em "+ Adicionar item" para incluir, no título ou ícone para editar/excluir a lista.
- Botão **+** cria uma tarefa (no calendário) ou uma lista (na aba Listas).
- Tudo fica salvo no aparelho, sem internet e sem conta.

## Build

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
listo/build.sh   # gera listo/build/Listo.apk
```

O APK pronto fica em `listo/releases/Listo-1.0.apk`. Na primeira execução o `build.sh` cria uma chave de assinatura em `listo/tools/listo.keystore` (fora do git); guarde-a para que as próximas versões instalem por cima.
