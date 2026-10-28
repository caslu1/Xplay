// Renders each .screen in screens.html to docs/mockups/<id>.png at 1920x1080.
const { chromium } = require('playwright');
const path = require('path'), fs = require('fs');
const order = ['login','home','live','epg','movies','detail','series','player','liveplay','sports','search','settings'];
(async () => {
  const out = path.resolve(__dirname, '../../docs/mockups');
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  await p.goto('file://' + path.join(__dirname, 'screens.html'));
  await p.evaluate(() => document.fonts.ready);
  for (const [i, id] of order.entries()) {
    const name = String(i + 1).padStart(2, '0') + '-' + id + '.png';
    await p.locator('#' + id).screenshot({ path: path.join(out, name) });
    console.log(name);
  }
  await b.close();
})();
