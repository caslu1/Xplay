# Leitor PDF

Leitor de PDF nativo para Android, simples e somente leitura. Não tem dependências: usa o `PdfRenderer` do próprio Android (5.0+).

- Abra um PDF pelo botão de pasta, ou escolha "Leitor PDF" ao abrir um PDF em outro app (gerenciador de arquivos, navegador, e-mail…)
- Rolagem vertical contínua pelas páginas
- Zoom com pinça (1× a 4×) ou toque duplo; arraste para os lados com o zoom ativo
- Toque simples esconde/mostra a barra superior
- Indicador "página / total"; toque nele para ir a uma página
- Voltar fecha o documento e mostra a tela inicial com os arquivos recentes (toque longo remove da lista); Voltar na tela inicial sai do app
- Lembra a página em que você parou em cada arquivo
- PDFs protegidos por senha não são suportados (o `PdfRenderer` não abre)

## Build

```sh
sudo apt-get install android-sdk-platform-23 aapt apksigner zipalign dalvik-exchange
./build.sh   # gera build/LeitorPDF.apk
```

O APK é assinado com `tools/pdfreader.keystore` (senha `pdfreader123`). Use sempre a mesma chave para que as atualizações instalem por cima.

O APK pronto da versão atual está em `releases/LeitorPDF-1.1.apk`.
