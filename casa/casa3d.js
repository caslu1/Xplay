/* Minha Casa — vista 3D da casa (Three.js).
 * Cômodos com piso e paredes (as paredes viradas para a câmera abaixam sozinhas),
 * móveis montados com blocos, aparelhos (ar, TV, lâmpadas) e luzes de verdade
 * para as lâmpadas acesas. Unidades em metros; o plano (x, y) vira (x, z) no 3D. */
(function () {
  'use strict';
  const T = window.THREE;
  const Casa3D = { supported: false };
  window.Casa3D = Casa3D;
  if (!T) return;
  try {
    const c = document.createElement('canvas');
    Casa3D.supported = !!(window.WebGLRenderingContext && (c.getContext('webgl') || c.getContext('experimental-webgl')));
  } catch (e) { Casa3D.supported = false; }
  if (!Casa3D.supported) return;

  const WALL_H = 2.5, WALL_T = 0.1, MAX_LAMPS = 10;
  let renderer, scene, camera, container, labelsEl, sun, ground;
  let root = null, staticKey = '', data = null, cb = {};
  let walls = [], floors = [], devs = {}, lampPool = [];
  let dirty = true, running = false, bounds = { W: 8, H: 8 };
  const view = { theta: -0.75, phi: 0.92, radius: 16, target: new T.Vector3(4, 0, 4) };
  let anim = null;
  const matCache = {}, texCache = {};

  /* ---------- materiais e texturas ---------- */
  function mat(color, opts) {
    const k = color + JSON.stringify(opts || {});
    if (!matCache[k]) matCache[k] = new T.MeshStandardMaterial(Object.assign({ color: new T.Color(color), roughness: 0.82, metalness: 0.02 }, opts || {}));
    return matCache[k];
  }
  function canvasTex(key, draw, size) {
    if (texCache[key]) return texCache[key];
    const c = document.createElement('canvas'); c.width = c.height = size || 256;
    draw(c.getContext('2d'), c.width);
    const t = new T.CanvasTexture(c);
    t.wrapS = t.wrapT = T.RepeatWrapping; t.encoding = T.sRGBEncoding; t.anisotropy = 4;
    return (texCache[key] = t);
  }
  function shade(hex, f) { const c = new T.Color(hex); c.offsetHSL(0, 0, f); return '#' + c.getHexString(); }
  function floorTex(kind, color) {
    return canvasTex('f' + kind + color, (g, n) => {
      g.fillStyle = color; g.fillRect(0, 0, n, n);
      if (kind === 'tile') {
        g.strokeStyle = 'rgba(0,0,0,.12)'; g.lineWidth = 2;
        for (let i = 0; i <= n; i += n / 4) { g.beginPath(); g.moveTo(i, 0); g.lineTo(i, n); g.stroke(); g.beginPath(); g.moveTo(0, i); g.lineTo(n, i); g.stroke(); }
      } else if (kind === 'carpet') {
        for (let i = 0; i < 2500; i++) { g.fillStyle = `rgba(${Math.random() < .5 ? '0,0,0' : '255,255,255'},.05)`; g.fillRect(Math.random() * n, Math.random() * n, 2, 2); }
      } else if (kind === 'concrete') {
        for (let i = 0; i < 1500; i++) { g.fillStyle = `rgba(0,0,0,${Math.random() * .06})`; g.fillRect(Math.random() * n, Math.random() * n, 3, 3); }
      } else { // madeira: tábuas
        const rows = 8, rh = n / rows;
        for (let r = 0; r < rows; r++) {
          const off = (r % 2) * n / 2 + (r * 37 % 50);
          for (let x = -n; x < n * 2; x += n / 1.5) {
            g.fillStyle = shade(color, (Math.random() - .5) * .08);
            g.fillRect(x + off, r * rh, n / 1.5 - 2, rh - 2);
          }
          g.fillStyle = 'rgba(0,0,0,.18)'; g.fillRect(0, r * rh + rh - 2, n, 2);
        }
        g.globalAlpha = .06; g.strokeStyle = '#000';
        for (let i = 0; i < 40; i++) { const y = Math.random() * n; g.beginPath(); g.moveTo(0, y); g.bezierCurveTo(n / 3, y + 3, n * 2 / 3, y - 3, n, y); g.stroke(); }
        g.globalAlpha = 1;
      }
    });
  }
  function textTex(text) {
    return canvasTex('t' + text, (g, n) => {
      g.clearRect(0, 0, n, n / 4);
      g.font = 'bold 44px system-ui, Roboto, sans-serif'; g.fillStyle = 'rgba(30,30,50,.55)';
      g.textBaseline = 'middle'; g.fillText(text.toUpperCase(), 8, 32);
    }, 512);
  }
  function screenTex() {
    return canvasTex('screen', (g, n) => {
      const gr = g.createLinearGradient(0, 0, n, n);
      gr.addColorStop(0, '#2b6cff'); gr.addColorStop(.5, '#9b4dff'); gr.addColorStop(1, '#ff6a8a');
      g.fillStyle = gr; g.fillRect(0, 0, n, n);
      g.fillStyle = 'rgba(255,255,255,.25)'; g.beginPath(); g.arc(n * .7, n * .35, n * .18, 0, 7); g.fill();
    });
  }

  /* ---------- peças ---------- */
  function box(parent, w, h, d, color, x, y, z, opts) {
    const m = new T.Mesh(new T.BoxGeometry(w, h, d), typeof color === 'string' ? mat(color, opts) : color);
    m.position.set(x || 0, (y || 0) + h / 2, z || 0); m.castShadow = true; m.receiveShadow = true;
    parent.add(m); return m;
  }
  function cyl(parent, rt, rb, h, color, x, y, z, seg) {
    const m = new T.Mesh(new T.CylinderGeometry(rt, rb, h, seg || 20), mat(color));
    m.position.set(x || 0, (y || 0) + h / 2, z || 0); m.castShadow = true; m.receiveShadow = true;
    parent.add(m); return m;
  }
  function legs(g, w, d, h, color, r) {
    const ix = w / 2 - 0.05, iz = d / 2 - 0.05;
    [[-ix, -iz], [ix, -iz], [-ix, iz], [ix, iz]].forEach(([x, z]) => box(g, r || 0.04, h, r || 0.04, color, x, 0, z));
  }

  const DEFAULTS = {
    sofa: ['#7d8597', 2.0, 0.9, 0.85], armchair: ['#a3847a', 0.85, 0.85, 0.85], bed: ['#9fb3c8', 1.6, 2.0, 1.0], crib: ['#f2f2f2', 0.7, 1.3, 0.9],
    nightstand: ['#8a6a4f', 0.45, 0.4, 0.55], wardrobe: ['#c9b49a', 1.6, 0.6, 2.1], dresser: ['#a07b5a', 1.1, 0.5, 0.85], desk: ['#b8946d', 1.2, 0.6, 0.75],
    chair: ['#555b66', 0.45, 0.5, 0.9], dining_table: ['#8b6a4a', 1.4, 0.85, 0.76], coffee_table: ['#9c7a58', 1.0, 0.55, 0.42], tv_stand: ['#4a3f38', 1.6, 0.4, 0.5],
    bookshelf: ['#9a7b5c', 0.9, 0.35, 1.9], cabinet: ['#cdbfae', 0.8, 0.45, 0.9], rug: ['#c8b6d8', 2.0, 1.4, 0.01], plant: ['#3f8f5a', 0.45, 0.45, 1.0],
    floor_lamp: ['#e8e2d6', 0.35, 0.35, 1.6], fridge: ['#d9dde3', 0.7, 0.7, 1.8], stove: ['#e5e5e5', 0.6, 0.6, 0.9], kitchen_counter: ['#e9e4dc', 1.8, 0.6, 0.9],
    sink: ['#e9e4dc', 0.9, 0.6, 0.9], microwave: ['#2c2c30', 0.5, 0.38, 0.3], washing_machine: ['#f1f1f1', 0.6, 0.6, 0.85], toilet: ['#f4f4f4', 0.4, 0.65, 0.75],
    shower: ['#cfe6f2', 0.9, 0.9, 2.0], bathtub: ['#f4f4f4', 1.6, 0.75, 0.55], bathroom_sink: ['#f4f4f4', 0.6, 0.45, 0.85], mirror: ['#cfe3f0', 0.6, 0.03, 0.8],
    door: ['#8a6446', 0.85, 0.05, 2.1], window: ['#bfe3ff', 1.2, 0.05, 1.1], other: ['#9aa0aa', 0.6, 0.6, 0.6],
  };
  Casa3D.DEFAULTS = DEFAULTS;
  const WALL_TYPES = { door: 1, window: 1, mirror: 1 };

  function furniture(it) {
    const d0 = DEFAULTS[it.type] || DEFAULTS.other;
    const c = /^#[0-9a-f]{6}$/i.test(it.color || '') ? it.color : d0[0];
    const w = Math.max(0.2, it.width || d0[1]), d = Math.max(0.02, it.depth || d0[2]), h = Math.max(0.01, it.height || d0[3]);
    const g = new T.Group();
    const wood = '#8b6a4a', dark = '#2a2a2e', white = '#f5f5f2';
    switch (it.type) {
      case 'sofa': case 'armchair': {
        const arm = Math.min(0.2, w * 0.12);
        box(g, w, 0.38, d, c, 0, 0.06, 0);
        box(g, w, h - 0.06, 0.2, c, 0, 0.06, -d / 2 + 0.1);
        box(g, arm, 0.62, d, shade(c, -.04), -w / 2 + arm / 2, 0.06, 0);
        box(g, arm, 0.62, d, shade(c, -.04), w / 2 - arm / 2, 0.06, 0);
        const n = it.type === 'sofa' ? Math.max(2, Math.round((w - 2 * arm) / 0.7)) : 1, cw = (w - 2 * arm) / n;
        for (let i = 0; i < n; i++) box(g, cw - 0.04, 0.12, d - 0.26, shade(c, .05), -w / 2 + arm + cw * (i + .5), 0.44, 0.08);
        legs(g, w, d, 0.06, dark, 0.05);
        break;
      }
      case 'bed': case 'crib': {
        box(g, w, 0.3, d, wood, 0, 0, 0);
        box(g, w - 0.06, 0.22, d - 0.08, white, 0, 0.3, 0.02);
        box(g, w, it.type === 'crib' ? 0.9 : 1.0, 0.08, shade(wood, -.05), 0, 0, -d / 2 + 0.04);
        const np = w > 1.2 ? 2 : 1, pw = (w - 0.2) / np;
        for (let i = 0; i < np; i++) box(g, pw - 0.06, 0.12, 0.35, '#ffffff', -w / 2 + 0.1 + pw * (i + .5), 0.52, -d / 2 + 0.3);
        box(g, w - 0.02, 0.06, d * 0.6, c, 0, 0.52, d * 0.2);
        break;
      }
      case 'nightstand': case 'dresser': case 'cabinet': case 'tv_stand': {
        box(g, w, h - 0.06, d, c, 0, 0.06, 0);
        const rows = it.type === 'dresser' ? 3 : it.type === 'nightstand' ? 2 : 1;
        for (let i = 1; i <= rows; i++) box(g, w - 0.06, 0.012, 0.01, shade(c, -.2), 0, 0.06 + (h - 0.06) * i / (rows + 1), d / 2 + 0.005);
        legs(g, w, d, 0.06, dark, 0.04);
        break;
      }
      case 'wardrobe': case 'fridge': {
        box(g, w, h, d, c, 0, 0, 0);
        box(g, 0.012, h - 0.1, 0.01, shade(c, -.25), it.type === 'fridge' ? 0 : 0, 0.05, d / 2 + 0.005).visible = it.type === 'wardrobe';
        if (it.type === 'fridge') box(g, w - 0.04, 0.012, 0.01, shade(c, -.25), 0, h * 0.62, d / 2 + 0.005);
        box(g, 0.025, 0.25, 0.03, '#b0b4ba', it.type === 'fridge' ? -w / 2 + 0.08 : -0.06, h * 0.5, d / 2 + 0.02);
        if (it.type === 'wardrobe') box(g, 0.025, 0.25, 0.03, '#b0b4ba', 0.06, h * 0.5, d / 2 + 0.02);
        break;
      }
      case 'bookshelf': {
        box(g, w, h, 0.03, c, 0, 0, -d / 2 + 0.015);
        box(g, 0.03, h, d, c, -w / 2 + 0.015, 0, 0); box(g, 0.03, h, d, c, w / 2 - 0.015, 0, 0);
        const sh = 5;
        for (let i = 0; i <= sh; i++) {
          box(g, w, 0.03, d, c, 0, (h - 0.03) * i / sh, 0);
          if (i < sh) for (let x = -w / 2 + 0.06; x < w / 2 - 0.08; x += 0.07) {
            if (Math.random() < .25) continue;
            const bh = (h / sh) * (0.55 + Math.random() * 0.3);
            box(g, 0.05, bh, d * 0.8, ['#c0504d', '#4f81bd', '#9bbb59', '#e8b84a', '#8064a2', '#333'][Math.floor(Math.random() * 6)], x, (h - 0.03) * i / sh + 0.03, 0.02);
          }
        }
        break;
      }
      case 'desk': case 'dining_table': case 'coffee_table': {
        box(g, w, 0.04, d, c, 0, h - 0.04, 0);
        legs(g, w, d, h - 0.04, it.type === 'desk' ? '#3a3a3e' : shade(c, -.1), 0.05);
        break;
      }
      case 'chair': {
        box(g, w, 0.05, d, c, 0, 0.43, 0);
        box(g, w, 0.45, 0.05, c, 0, 0.48, -d / 2 + 0.025);
        legs(g, w, d, 0.43, dark, 0.03);
        break;
      }
      case 'rug': {
        const m = box(g, w, 0.012, d, c, 0, 0, 0); m.castShadow = false;
        break;
      }
      case 'plant': {
        cyl(g, 0.16, 0.12, 0.32, '#c7a07a', 0, 0, 0);
        const f = new T.Mesh(new T.IcosahedronGeometry(Math.max(0.25, w * 0.6), 1), mat(c, { flatShading: true }));
        f.position.y = 0.32 + Math.max(0.25, h - 0.55); f.castShadow = true; g.add(f);
        break;
      }
      case 'floor_lamp': {
        cyl(g, 0.15, 0.15, 0.03, dark, 0, 0, 0); cyl(g, 0.015, 0.015, h - 0.3, dark, 0, 0.03, 0);
        const s = cyl(g, 0.12, 0.2, 0.3, c, 0, h - 0.3, 0); s.material = mat(c, { emissive: new T.Color('#ffdca0'), emissiveIntensity: 0.25 });
        break;
      }
      case 'stove': case 'kitchen_counter': case 'sink': {
        box(g, w, h - 0.04, d, c, 0, 0, 0);
        box(g, w, 0.04, d + 0.02, it.type === 'stove' ? dark : '#7d756c', 0, h - 0.04, 0.01);
        if (it.type === 'stove') for (const [x, z] of [[-.15, -.12], [.15, -.12], [-.15, .12], [.15, .12]]) cyl(g, 0.07, 0.07, 0.01, '#555', x * w / 0.6, h, z);
        if (it.type === 'sink') { box(g, 0.5, 0.012, 0.36, '#9aa3ab', 0, h, 0, { metalness: 0.6, roughness: 0.3 }); cyl(g, 0.012, 0.012, 0.25, '#b9bfc5', 0, h, -d / 2 + 0.1); }
        box(g, w - 0.04, 0.01, 0.01, shade(c, -.2), 0, h * 0.75, d / 2 + 0.005);
        break;
      }
      case 'microwave': box(g, w, h, d, c, 0, 0.9, 0); box(g, w * 0.6, h * 0.6, 0.01, '#111', -w * 0.12, 0.9 + h * 0.2, d / 2 + 0.005); break;
      case 'washing_machine': {
        box(g, w, h, d, c, 0, 0, 0);
        const door = new T.Mesh(new T.CylinderGeometry(w * 0.3, w * 0.3, 0.03, 28), mat('#30343a', { roughness: 0.2, metalness: 0.4 }));
        door.rotation.x = Math.PI / 2; door.position.set(0, h * 0.45, d / 2 + 0.015); g.add(door);
        break;
      }
      case 'toilet': {
        box(g, w * 0.7, 0.4, d * 0.6, c, 0, 0, 0.05);
        const seat = new T.Mesh(new T.CylinderGeometry(w / 2, w / 2.4, 0.06, 24), mat(c)); seat.scale.z = 1.25; seat.position.set(0, 0.43, 0.08); seat.castShadow = true; g.add(seat);
        box(g, w, 0.4, 0.18, c, 0, 0.35, -d / 2 + 0.09);
        break;
      }
      case 'shower': {
        box(g, w, 0.06, d, '#e8edf0', 0, 0, 0);
        const glass = new T.MeshStandardMaterial({ color: 0xcfe6f2, transparent: true, opacity: 0.25, roughness: 0.05 });
        box(g, w, h, 0.02, glass, 0, 0.06, d / 2 - 0.01).castShadow = false;
        box(g, 0.02, h, d, glass, w / 2 - 0.01, 0.06, 0).castShadow = false;
        cyl(g, 0.012, 0.012, 0.3, '#b9bfc5', -w / 2 + 0.1, h - 0.2, -d / 2 + 0.05);
        break;
      }
      case 'bathtub': box(g, w, h, d, c, 0, 0, 0); box(g, w - 0.14, 0.02, d - 0.14, '#bcd6e6', 0, h - 0.06, 0); break;
      case 'bathroom_sink': box(g, w, h - 0.12, d, '#cdbfae', 0, 0, 0); box(g, w, 0.12, d, c, 0, h - 0.12, 0); box(g, w * 0.6, 0.012, d * 0.55, '#d6dde2', 0, h, 0.02); break;
      case 'mirror': box(g, w, h, 0.03, mat(c, { metalness: 0.9, roughness: 0.05 }), 0, 1.0, 0).castShadow = false; break;
      case 'door': box(g, w, h, 0.05, c, 0, 0, 0); box(g, 0.12, 0.025, 0.04, '#c9c9c9', w / 2 - 0.12, h * 0.48, 0.04); break;
      case 'window': {
        const glass = new T.MeshStandardMaterial({ color: 0xbfe3ff, transparent: true, opacity: 0.45, roughness: 0.05, emissive: new T.Color('#9fd0ff'), emissiveIntensity: 0.25 });
        box(g, w, h, 0.04, glass, 0, 0.95, 0).castShadow = false;
        box(g, w + 0.06, 0.05, 0.08, '#f2f2f2', 0, 0.9, 0); box(g, w + 0.06, 0.05, 0.08, '#f2f2f2', 0, 0.95 + h, 0);
        break;
      }
      default: box(g, w, h, d, c, 0, 0, 0);
    }
    return g;
  }

  /* posição colada na parede mais próxima (para porta, janela, ar, TV) */
  function snapWall(room, x, y, inset) {
    const opts = [[y, 'top'], [room.h - y, 'bottom'], [x, 'left'], [room.w - x, 'right']].sort((a, b) => a[0] - b[0]);
    const side = opts[0][1], i = WALL_T + (inset || 0);
    if (side === 'top') return { x, y: i, rot: 0 };
    if (side === 'bottom') return { x, y: room.h - i, rot: Math.PI };
    if (side === 'left') return { x: i, y, rot: Math.PI / 2 };
    return { x: room.w - i, y, rot: -Math.PI / 2 };
  }

  /* ---------- montagem da cena ---------- */
  function disposeTree(o) {
    o.traverse(n => { if (n.geometry) n.geometry.dispose(); });
  }
  function build() {
    if (root) { scene.remove(root); disposeTree(root); }
    root = new T.Group(); scene.add(root);
    walls = []; floors = []; devs = {};
    const rooms = data.rooms;
    let W = 4, H = 4;
    rooms.forEach(r => { W = Math.max(W, r.x + r.w); H = Math.max(H, r.y + r.h); });
    bounds = { W, H };

    const slab = new T.Mesh(new T.BoxGeometry(W + 0.8, 0.25, H + 0.8), mat('#2b2747', { roughness: 1 }));
    slab.position.set(W / 2, -0.125, H / 2); slab.receiveShadow = true; root.add(slab);
    ground.position.set(W / 2, -0.26, H / 2); ground.scale.set(W + 14, H + 14, 1);

    rooms.forEach(r => {
      const kind = r.floor || (/cozinha|banheiro|lavanderia|área|area|varanda|wc|lavabo/i.test(r.name) ? 'tile' : 'wood');
      const base = r.floorColor || (kind === 'tile' ? '#e8e6e1' : kind === 'carpet' ? r.c : '#c99f74');
      const tex = floorTex(kind, base).clone(); tex.needsUpdate = true; tex.repeat.set(r.w / 2, r.h / 2);
      const fm = new T.Mesh(new T.BoxGeometry(r.w, 0.04, r.h), new T.MeshStandardMaterial({ map: tex, roughness: kind === 'tile' ? 0.35 : 0.75 }));
      fm.position.set(r.x + r.w / 2, 0.02, r.y + r.h / 2); fm.receiveShadow = true; fm.userData.roomId = r.id;
      root.add(fm); floors.push(fm);

      const label = new T.Mesh(new T.PlaneGeometry(2, 0.5), new T.MeshBasicMaterial({ map: textTex(r.name), transparent: true, depthWrite: false }));
      label.rotation.x = -Math.PI / 2; label.position.set(r.x + 1.15, 0.045, r.y + 0.42); root.add(label);

      const wc = r.wall || '#f1eee9';
      const wm = mat(wc, { roughness: 0.92 });
      // parede compartilhada com outro cômodo? (interna: fica a meia altura)
      const shared = (side) => rooms.some(o => {
        if (o === r) return false;
        const ovX = Math.min(r.x + r.w, o.x + o.w) - Math.max(r.x, o.x) > 0.3, ovY = Math.min(r.y + r.h, o.y + o.h) - Math.max(r.y, o.y) > 0.3;
        if (side === 't') return ovX && Math.abs(o.y + o.h - r.y) < 0.2;
        if (side === 'b') return ovX && Math.abs(o.y - (r.y + r.h)) < 0.2;
        if (side === 'l') return ovY && Math.abs(o.x + o.w - r.x) < 0.2;
        return ovY && Math.abs(o.x - (r.x + r.w)) < 0.2;
      });
      const mk = (w, d, x, z, nx, nz, side) => {
        const geo = new T.BoxGeometry(w, WALL_H, d); geo.translate(0, WALL_H / 2, 0);
        const m = new T.Mesh(geo, wm); m.position.set(x, 0.04, z); m.castShadow = true; m.receiveShadow = true;
        root.add(m); walls.push({ mesh: m, n: new T.Vector3(nx, 0, nz), c: new T.Vector3(x, 0, z), s: 1, inner: shared(side) });
      };
      mk(r.w, WALL_T, r.x + r.w / 2, r.y + WALL_T / 2, 0, -1, 't');
      mk(r.w, WALL_T, r.x + r.w / 2, r.y + r.h - WALL_T / 2, 0, 1, 'b');
      mk(WALL_T, r.h, r.x + WALL_T / 2, r.y + r.h / 2, -1, 0, 'l');
      mk(WALL_T, r.h, r.x + r.w - WALL_T / 2, r.y + r.h / 2, 1, 0, 'r');

      (data.furniture[r.id] || []).forEach(it => {
        const g = furniture(it);
        let px = it.x, py = it.y, rot = -(it.rotation || 0) * Math.PI / 180;
        if (WALL_TYPES[it.type]) { const s = snapWall(r, it.x, it.y, it.type === 'mirror' ? 0.02 : -0.02); px = s.x; py = s.y; rot = s.rot; }
        g.position.set(r.x + px, 0.04, r.y + py); g.rotation.y = rot;
        root.add(g);
      });
    });

    // aparelhos
    data.devices.forEach(d => {
      const r = rooms.find(q => q.id === d.room) || rooms[0]; if (!r) return;
      const lx = d.x - r.x, ly = d.y - r.y;
      const g = new T.Group(); const o = { g, type: d.type, parts: {} };
      if (d.type === 'ac') {
        const s = snapWall(r, lx, ly, 0.11); g.position.set(r.x + s.x, 0, r.y + s.y); g.rotation.y = s.rot;
        box(g, 0.9, 0.3, 0.22, '#f7f7f7', 0, 2.05, 0, { roughness: 0.4 });
        o.parts.strip = box(g, 0.7, 0.025, 0.01, '#cfd6de', 0, 2.12, 0.115);
        o.parts.flap = box(g, 0.86, 0.04, 0.05, '#e6e6e6', 0, 2.05, 0.1);
        const air = new T.Mesh(new T.PlaneGeometry(1.1, 1.3), new T.MeshBasicMaterial({ map: airTex(), transparent: true, depthWrite: false, opacity: 0 }));
        air.position.set(0, 1.45, 0.6); air.rotation.x = -0.55; g.add(air); o.parts.air = air;
        o.labelY = 2.5;
      } else if (d.type === 'tv') {
        const s = snapWall(r, lx, ly, 0.04); g.position.set(r.x + s.x, 0, r.y + s.y); g.rotation.y = s.rot;
        box(g, 1.3, 0.76, 0.05, '#111114', 0, 0.85, 0, { roughness: 0.3 });
        const scr = new T.Mesh(new T.PlaneGeometry(1.24, 0.7), new T.MeshStandardMaterial({ color: 0x050507, roughness: 0.2 }));
        scr.position.set(0, 1.23, 0.027); g.add(scr); o.parts.screen = scr;
        o.labelY = 1.9;
      } else if (d.type === 'light') {
        g.position.set(d.x, 0, d.y);
        cyl(g, 0.22, 0.22, 0.04, '#f2f2f2', 0, 2.42, 0);
        const bulb = new T.Mesh(new T.SphereGeometry(0.1, 20, 14), new T.MeshStandardMaterial({ color: 0xffffff, emissive: new T.Color('#000'), roughness: 0.3 }));
        bulb.position.y = 2.32; g.add(bulb); o.parts.bulb = bulb;
        o.labelY = 2.75;
      } else {
        g.position.set(d.x, 0, d.y);
        box(g, 0.22, 0.32, 0.22, '#3a3a40', 0, 0.04, 0, { roughness: 0.5 });
        o.parts.led = box(g, 0.06, 0.06, 0.01, '#444', 0, 0.26, 0.115);
        o.labelY = 0.75;
      }
      root.add(g);
      const el = document.createElement('div'); el.className = 'lbl'; el.dataset.dev = d.id;
      el.innerHTML = `<div class="bub"></div><div class="pn"></div>`;
      labelsEl.appendChild(el); o.el = el;
      devs[d.id] = o;
    });
    labelsEl.querySelectorAll('.lbl').forEach(el => { if (!devs[el.dataset.dev] || devs[el.dataset.dev].el !== el) el.remove(); });

    // sombra do sol cobre a casa toda
    const m = Math.max(W, H) * 0.75 + 3, sc = sun.shadow.camera;
    sc.left = -m; sc.right = m; sc.top = m; sc.bottom = -m; sc.near = 1; sc.far = 80; sc.updateProjectionMatrix();
    sun.position.set(W / 2 + 7, 18, H / 2 + 9); sun.target.position.set(W / 2, 0, H / 2);
    updateDevices();
  }
  function airTex() {
    return canvasTex('air', (g, n) => {
      const gr = g.createLinearGradient(0, 0, 0, n);
      gr.addColorStop(0, 'rgba(120,200,255,.55)'); gr.addColorStop(1, 'rgba(120,200,255,0)');
      g.fillStyle = gr; g.beginPath(); g.moveTo(n * .2, 0); g.lineTo(n * .8, 0); g.lineTo(n, n); g.lineTo(0, n); g.closePath(); g.fill();
      g.strokeStyle = 'rgba(255,255,255,.35)'; g.lineWidth = 3;
      for (let i = 0; i < 4; i++) { const y = n * (.2 + i * .2); g.beginPath(); g.moveTo(n * .25, y); g.bezierCurveTo(n * .45, y - 14, n * .55, y + 14, n * .75, y); g.stroke(); }
    });
  }

  function updateDevices() {
    let li = 0;
    data.devices.forEach(d => {
      const o = devs[d.id]; if (!o) return;
      const on = d.on && d.online;
      if (o.type === 'ac') {
        const heat = d.mode === 'heat', col = heat ? '#ff9a5e' : '#5ec8ff';
        o.parts.strip.material = on ? mat(col, { emissive: new T.Color(col), emissiveIntensity: 1.2 }) : mat('#cfd6de');
        o.parts.air.material.opacity = on ? 0.9 : 0;
        o.parts.air.material.color = new T.Color(heat ? '#ffb38a' : '#ffffff');
      } else if (o.type === 'tv') {
        o.parts.screen.material = on ? new T.MeshBasicMaterial({ map: screenTex() }) : new T.MeshStandardMaterial({ color: 0x050507, roughness: 0.15, metalness: 0.3 });
      } else if (o.type === 'light') {
        const col = new T.Color(d.color || '#ffd9a0');
        o.parts.bulb.material.emissive = on ? col : new T.Color('#000');
        o.parts.bulb.material.emissiveIntensity = on ? 1.6 : 0;
        if (li < lampPool.length) {
          const L = lampPool[li++];
          L.color = col; L.intensity = on ? 0.35 + 1.25 * (d.level == null ? 1 : d.level / 100) : 0;
          L.position.set(o.g.position.x, 2.2, o.g.position.z);
        }
      } else if (o.parts.led) {
        o.parts.led.material = on ? mat('#7fd0ff', { emissive: new T.Color('#7fd0ff') }) : mat('#444');
      }
      o.el.className = 'lbl' + (on ? ' on' : '') + (d.online ? '' : ' offline');
      o.el.style.setProperty('--g', d.glow || '#9fb8ff');
      o.el.querySelector('.bub').innerHTML = d.icon || '';
      o.el.querySelector('.pn').textContent = d.name;
    });
    for (; li < lampPool.length; li++) lampPool[li].intensity = 0;
    dirty = true;
  }

  /* ---------- câmera e gestos ---------- */
  function placeCamera() {
    const t = view.target, r = view.radius;
    camera.position.set(t.x + r * Math.sin(view.phi) * Math.sin(view.theta), t.y + r * Math.cos(view.phi), t.z + r * Math.sin(view.phi) * Math.cos(view.theta));
    camera.lookAt(t);
  }
  function homeView() {
    return { theta: -0.75, phi: 0.92, radius: Math.max(9, Math.max(bounds.W, bounds.H) * 1.55 + 3), target: new T.Vector3(bounds.W / 2, 0, bounds.H / 2) };
  }
  function animateTo(v, ms) {
    anim = { from: { theta: view.theta, phi: view.phi, radius: view.radius, target: view.target.clone() }, to: v, t0: performance.now(), ms: ms || 650 };
    dirty = true;
  }
  Casa3D.resetView = () => animateTo(homeView());
  Casa3D.focusRoom = id => {
    const r = data && data.rooms.find(q => q.id === id); if (!r) return;
    animateTo({ theta: view.theta, phi: 0.72, radius: Math.max(r.w, r.h) * 1.9 + 3, target: new T.Vector3(r.x + r.w / 2, 0.4, r.y + r.h / 2) });
  };

  function bindControls() {
    const el = renderer.domElement, ptrs = new Map();
    let g = null, downAt = 0, moved = 0;
    const dist = () => { const [a, b] = [...ptrs.values()]; return Math.hypot(a.x - b.x, a.y - b.y); };
    const mid = () => { const [a, b] = [...ptrs.values()]; return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }; };
    el.addEventListener('pointerdown', e => {
      el.setPointerCapture && el.setPointerCapture(e.pointerId);
      ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY }); anim = null;
      if (ptrs.size === 1) { g = { x: e.clientX, y: e.clientY, theta: view.theta, phi: view.phi }; downAt = performance.now(); moved = 0; }
      else if (ptrs.size === 2) g = { d0: dist(), r0: view.radius, m0: mid(), t0: view.target.clone() };
    });
    el.addEventListener('pointermove', e => {
      if (!ptrs.has(e.pointerId) || !g) return;
      const p = ptrs.get(e.pointerId); moved += Math.abs(e.clientX - p.x) + Math.abs(e.clientY - p.y);
      ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (ptrs.size === 1 && g.theta != null) {
        view.theta = g.theta - (e.clientX - g.x) * 0.008;
        view.phi = Math.max(0.2, Math.min(1.45, g.phi - (e.clientY - g.y) * 0.006));
      } else if (ptrs.size === 2 && g.d0) {
        view.radius = Math.max(3, Math.min(60, g.r0 * g.d0 / Math.max(20, dist())));
        const m = mid(), k = view.radius / 700;
        const right = new T.Vector3(Math.cos(view.theta), 0, -Math.sin(view.theta));
        const fwd = new T.Vector3(-Math.sin(view.theta), 0, -Math.cos(view.theta));
        view.target.copy(g.t0).addScaledVector(right, -(m.x - g.m0.x) * k).addScaledVector(fwd, (m.y - g.m0.y) * k);
      }
      dirty = true;
    });
    const up = e => {
      if (!ptrs.has(e.pointerId)) return;
      ptrs.delete(e.pointerId);
      if (ptrs.size === 0) {
        if (moved < 8 && performance.now() - downAt < 450 && g && g.theta != null) tap(e);
        g = null;
      } else if (ptrs.size === 1) { const [p] = [...ptrs.values()]; g = { x: p.x, y: p.y, theta: view.theta, phi: view.phi }; moved = 99; }
    };
    el.addEventListener('pointerup', up); el.addEventListener('pointercancel', up);
    el.addEventListener('wheel', e => { e.preventDefault(); view.radius = Math.max(3, Math.min(60, view.radius * (e.deltaY < 0 ? 0.9 : 1.1))); dirty = true; }, { passive: false });
    el.addEventListener('dblclick', () => Casa3D.resetView());
    labelsEl.addEventListener('click', e => { const l = e.target.closest('.lbl'); if (l && cb.onDevice) cb.onDevice(l.dataset.dev); });
  }
  const ray = new T.Raycaster(), ndc = new T.Vector2();
  function tap(e) {
    const b = renderer.domElement.getBoundingClientRect();
    ndc.set(((e.clientX - b.left) / b.width) * 2 - 1, -((e.clientY - b.top) / b.height) * 2 + 1);
    ray.setFromCamera(ndc, camera);
    const hit = ray.intersectObjects(floors, false)[0];
    if (hit && cb.onRoom) cb.onRoom(hit.object.userData.roomId);
  }

  /* ---------- laço de desenho ---------- */
  const tmp = new T.Vector3();
  function frame() {
    if (!running) return;
    if (!container.isConnected || document.hidden) { running = false; return; }
    requestAnimationFrame(frame);
    if (anim) {
      const k = Math.min(1, (performance.now() - anim.t0) / anim.ms), e = 1 - Math.pow(1 - k, 3);
      view.theta = anim.from.theta + (anim.to.theta - anim.from.theta) * e;
      view.phi = anim.from.phi + (anim.to.phi - anim.from.phi) * e;
      view.radius = anim.from.radius + (anim.to.radius - anim.from.radius) * e;
      view.target.lerpVectors(anim.from.target, anim.to.target, e);
      if (k >= 1) anim = null;
      dirty = true;
    }
    // paredes viradas para a câmera abaixam (vista de "casa de bonecas")
    let moving = false;
    walls.forEach(w => {
      tmp.set(camera.position.x - w.c.x, 0, camera.position.z - w.c.z);
      const target = tmp.dot(w.n) > 0 ? 0.08 : w.inner ? 0.42 : 1;
      if (Math.abs(w.s - target) > 0.005) { w.s += (target - w.s) * 0.25; w.mesh.scale.y = w.s; moving = true; }
    });
    if (moving) dirty = true;
    if (!dirty) return;
    dirty = false;
    placeCamera();
    renderer.render(scene, camera);
    // etiquetas dos aparelhos
    const W = container.clientWidth, H = container.clientHeight, showNames = view.radius < 11;
    Object.values(devs).forEach(o => {
      tmp.set(o.g.position.x, o.labelY, o.g.position.z).project(camera);
      const vis = tmp.z < 1 && Math.abs(tmp.x) < 1.15 && Math.abs(tmp.y) < 1.15;
      o.el.style.display = vis ? '' : 'none';
      if (vis) o.el.style.transform = `translate(${((tmp.x + 1) / 2 * W).toFixed(1)}px, ${((1 - tmp.y) / 2 * H).toFixed(1)}px)`;
      o.el.classList.toggle('named', showNames);
    });
  }
  function start() { if (!running) { running = true; dirty = true; requestAnimationFrame(frame); } }
  document.addEventListener('visibilitychange', () => { if (!document.hidden && container && container.isConnected) start(); });

  function init() {
    if (T.ColorManagement) T.ColorManagement.legacyMode = false; // cores em sRGB, como no CSS
    container = document.createElement('div'); container.className = 'c3d';
    renderer = new T.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' });
    renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
    renderer.shadowMap.enabled = true; renderer.shadowMap.type = T.PCFSoftShadowMap;
    renderer.outputEncoding = T.sRGBEncoding;
    renderer.toneMapping = T.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.25;
    renderer.domElement.style.touchAction = 'none';
    container.appendChild(renderer.domElement);
    labelsEl = document.createElement('div'); labelsEl.className = 'c3d-labels'; container.appendChild(labelsEl);
    scene = new T.Scene();
    camera = new T.PerspectiveCamera(38, 1, 0.1, 300);
    scene.add(new T.HemisphereLight(0xe6ecff, 0x40365e, 0.7));
    scene.add(new T.AmbientLight(0xffffff, 0.12));
    sun = new T.DirectionalLight(0xfff1e0, 0.85); sun.castShadow = true;
    sun.shadow.mapSize.set(2048, 2048); sun.shadow.bias = -0.0004; sun.shadow.normalBias = 0.03;
    scene.add(sun); scene.add(sun.target);
    ground = new T.Mesh(new T.PlaneGeometry(1, 1), new T.ShadowMaterial({ opacity: 0.28 }));
    ground.rotation.x = -Math.PI / 2; ground.receiveShadow = true; scene.add(ground);
    for (let i = 0; i < MAX_LAMPS; i++) { const L = new T.PointLight(0xffd9a0, 0, 6.5, 2); scene.add(L); lampPool.push(L); }
    bindControls();
    if (window.ResizeObserver) new ResizeObserver(resize).observe(container);
    window.addEventListener('resize', resize);
  }
  function resize() {
    if (!container || !container.isConnected) return;
    const w = container.clientWidth, h = container.clientHeight; if (!w || !h) return;
    renderer.setSize(w, h, false); renderer.domElement.style.width = w + 'px'; renderer.domElement.style.height = h + 'px';
    camera.aspect = w / h; camera.updateProjectionMatrix(); dirty = true;
  }

  /* data: { rooms:[{id,name,x,y,w,h,c,floor,floorColor,wall}], devices:[{id,type,room,x,y,on,online,level,color,mode,name,icon,glow}], furniture:{roomId:[...]} } */
  Casa3D.mount = function (slot, d, callbacks) {
    if (!renderer) init();
    cb = callbacks || {};
    if (container.parentNode !== slot) slot.appendChild(container);
    const key = JSON.stringify([d.rooms, d.furniture, d.devices.map(x => [x.id, x.type, x.room, x.x, x.y])]);
    data = d;
    const first = !staticKey;
    if (key !== staticKey) { staticKey = key; build(); } else updateDevices();
    resize();
    if (first) { const h = homeView(); Object.assign(view, h); }
    start();
  };
})();
