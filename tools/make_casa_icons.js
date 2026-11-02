// Gera os ícones PNG do app Minha Casa a partir de casa/icon.svg (usa o Chromium do Playwright).
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const root = path.resolve(__dirname, '..');
  const svg = fs.readFileSync(path.join(root, 'casa/icon.svg'), 'utf8');
  const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
  const b = await chromium.launch();
  for (const [dpi, px] of Object.entries(sizes)) {
    const p = await b.newPage({ viewport: { width: px, height: px } });
    await p.setContent(`<style>html,body{margin:0;background:transparent}svg{width:${px}px;height:${px}px;display:block}</style>${svg}`);
    const dir = path.join(root, `casa-app/res/drawable-${dpi}`);
    fs.mkdirSync(dir, { recursive: true });
    await p.screenshot({ path: path.join(dir, 'ic_launcher.png'), omitBackground: true });
    await p.close();
  }
  await b.close();
})();
