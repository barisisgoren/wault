/* Wault web demosu: sürüklenebilir telefon, POS ve barkod okuyucu. Bağımlılık yok. */
(() => {
  'use strict';

  // Uygulamadaki tanıtım kartları (DemoCards.promo) ve renkleri (CardArt.kt)
  const CARDS = [
    { id: 'b1', kind: 'pay', title: 'Banka 1', sub: 'Platinum', color: '#2D8F66', net: 'mastercard', last4: '4417', exp: '08/28' },
    { id: 'b2', kind: 'pay', title: 'Banka 2', sub: 'Gold', color: '#A3283A', net: 'visa', last4: '8851', exp: '03/29' },
    { id: 'b3', kind: 'pay', title: 'Banka 3', sub: 'Classic', color: '#4B46D4', net: 'amex', last4: '1007', exp: '11/27' },
    { id: 'b4', kind: 'pay', title: 'Banka 4', sub: 'Bankkart', color: '#119E9A', net: 'troy', last4: '6021', exp: '06/28' },
    { id: 'mk', kind: 'loyalty', title: 'Market', sub: 'Üyelik kartı', color: '#EC6740', member: '5300 1182 7741', points: '1.240 puan', code: '8690000000012' },
    { id: 'oz', kind: 'show', title: 'Özel Kart', sub: 'Sahte kart', color: '#D6DAE1', darkInk: true, net: 'visa', last4: '1234', exp: '12/29' },
  ];
  const AMOUNTS = ['₺248,50', '₺86,90', '₺1.120,00', '₺37,25', '₺512,40', '₺14,99'];

  // Sahne düzenleri (sahne koordinatları). Dar ekranda cihazlar üstte, telefon altta.
  const LAYOUTS = {
    wide: { w: 1100, h: 700, scanner: [24, 120, 1], pos: [846, 150, 1], phone: [575, 330] },
    narrow: { w: 440, h: 1010, scanner: [0, 34, 0.66], pos: [276, 40, 0.7], phone: [220, 680] },
  };
  const PHONE_W = 250, PHONE_H = 520;
  const STACK_TOP = 82, STRIP = 44, COLLAPSED_TOP = 448;
  const MIN_X = PHONE_W / 2 + 80; // soldaki Çevir tuşu sahnede kalsın

  const $ = (s, r = document) => r.querySelector(s);
  const stageHost = $('#stageHost'), stage = $('#stage');
  const scanner = $('#scanner'), pos = $('#pos');
  const wrap = $('#phoneWrap'), tilt = $('#phoneTilt'), phone = $('#phone'), inner = $('#phoneInner');
  const screen = $('#screen'), stackEl = $('#stack'), detail = $('#detail'), readybar = $('#readybar');
  const paid = $('#paid'), toastEl = $('#toast'), flipBtn = $('#flipBtn');

  const S = {
    selected: null,
    ready: 'b1',
    flipped: false,
    amount: AMOUNTS[0],
    sound: true,
    missions: new Set(),
  };
  let layout = null, scale = 1;
  const P = { x: 0, y: 0, rot: 0 };

  if (new URLSearchParams(location.search).has('debug')) stage.classList.add('debug');

  /* ───────────── küçük yardımcılar ───────────── */
  const card = id => CARDS.find(c => c.id === id);
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const svgUse = (id, cls = '') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><use href="#${id}"/></svg>`;

  function netLogo(c) {
    if (!c.net) return '';
    if (c.net === 'troy') return `<img class="c-net troy" src="assets/troy.svg" alt="TROY">`;
    const white = c.net === 'visa' && !c.darkInk ? ' white' : '';
    const alt = { visa: 'Visa', mastercard: 'Mastercard', amex: 'American Express' }[c.net];
    return `<img class="c-net ${c.net}${white}" src="assets/net_${c.net}.png" alt="${alt}">`;
  }

  // EAN-13 çizimi
  const L = ['0001101', '0011001', '0010011', '0111101', '0100011', '0110001', '0101111', '0111011', '0110111', '0001011'];
  const PARITY = ['LLLLLL', 'LLGLGG', 'LLGGLG', 'LLGGGL', 'LGLLGG', 'LGGLLG', 'LGGGLL', 'LGLGLG', 'LGLGGL', 'LGGLGL'];
  const R = L.map(p => [...p].map(b => (b === '0' ? '1' : '0')).join(''));
  const G = R.map(p => [...p].reverse().join(''));
  function ean13Svg(code) {
    const d = [...code].map(Number);
    let bits = '101';
    for (let i = 1; i <= 6; i++) bits += (PARITY[d[0]][i - 1] === 'L' ? L : G)[d[i]];
    bits += '01010';
    for (let i = 7; i <= 12; i++) bits += R[d[i]];
    bits += '101';
    let rects = '', x = 0;
    while (x < bits.length) {
      if (bits[x] === '1') {
        let w = 1;
        while (bits[x + w] === '1') w++;
        rects += `<rect x="${x}" y="0" width="${w}" height="60"/>`;
        x += w;
      } else x++;
    }
    return `<svg class="barcode" viewBox="-4 0 103 60" preserveAspectRatio="none" aria-label="EAN-13 barkod ${code}">${rects}</svg>`;
  }

  /* ───────────── ses ───────────── */
  let actx = null;
  function tone(freq, dur, when = 0, type = 'square', vol = 0.05) {
    if (!S.sound) return;
    try {
      actx = actx || new (window.AudioContext || window.webkitAudioContext)();
      const t = actx.currentTime + when;
      const o = actx.createOscillator(), g = actx.createGain();
      o.type = type; o.frequency.value = freq;
      g.gain.setValueAtTime(0, t);
      g.gain.linearRampToValueAtTime(vol, t + 0.008);
      g.gain.setValueAtTime(vol, t + dur - 0.02);
      g.gain.linearRampToValueAtTime(0, t + dur);
      o.connect(g).connect(actx.destination);
      o.start(t); o.stop(t + dur + 0.02);
    } catch (_) { /* ses yoksa sessiz devam */ }
  }
  const beepScan = () => tone(2350, 0.11, 0, 'square', 0.035);
  const beepOk = () => { tone(1320, 0.12, 0, 'sine', 0.09); tone(1760, 0.22, 0.13, 'sine', 0.09); };
  const beepWarn = () => { tone(420, 0.14, 0, 'triangle', 0.08); tone(360, 0.18, 0.16, 'triangle', 0.08); };

  /* ───────────── telefon ekranı ───────────── */
  function renderCards() {
    stackEl.innerHTML = '';
    CARDS.forEach((c, i) => {
      const el = document.createElement('button');
      el.type = 'button';
      el.className = 'card' + (c.darkInk ? ' dark-ink' : '');
      el.dataset.id = c.id;
      el.style.setProperty('--card', c.color);
      el.style.zIndex = i + 1;
      el.setAttribute('aria-label', `${c.title}, ${c.sub}`);
      let icon, right = '', bottom = '';
      if (c.kind === 'pay') {
        icon = `<span class="c-ico">${svgUse('i-card')}</span>`;
        right = `<span class="c-cl">${svgUse('i-contactless')}</span>`;
        bottom = `<span class="c-meta"><small>${c.exp}</small><span class="c-num">•••• ${c.last4}</span></span>${netLogo(c)}`;
      } else if (c.kind === 'loyalty') {
        icon = `<span class="c-ico soft">${c.title[0]}</span>`;
        right = `<span class="c-right">${c.points}</span>`;
        bottom = `<span class="c-meta"><small>Üye no</small><span class="c-num">${c.member}</span></span>`;
      } else {
        icon = `<span class="c-ico soft">${c.title[0]}</span>`;
        bottom = `<span class="c-meta"><small>${c.exp}</small><span class="c-num">•••• ${c.last4}</span></span>${netLogo(c)}`;
      }
      el.innerHTML = `<span class="card-face">
        <span class="card-top">${icon}<span class="c-titles"><span class="c-title">${c.title}</span><span class="c-sub">${c.sub}</span></span>${right}</span>
        <span class="card-bottom">${bottom}</span></span>`;
      el.addEventListener('click', () => onCardTap(c.id));
      stackEl.appendChild(el);
    });
  }

  function layoutCards() {
    let k = 0;
    CARDS.forEach((c, i) => {
      const el = stackEl.children[i];
      el.classList.toggle('is-ready', c.id === S.ready);
      let y, s = 1;
      if (!S.selected) y = STACK_TOP + i * STRIP;
      else if (c.id === S.selected) y = STACK_TOP;
      else { y = COLLAPSED_TOP + k * 6; s = 0.94 + k * 0.01; k++; }
      el.style.transform = `translateY(${y}px) scale(${s})`;
      el.tabIndex = S.selected && c.id !== S.selected ? -1 : 0;
    });
    screen.classList.toggle('has-selection', !!S.selected);
    renderReadyBar();
    renderDetail();
  }

  function renderReadyBar() {
    const c = card(S.ready);
    readybar.style.setProperty('--card', c.color);
    readybar.innerHTML = `<span class="rb-ico">${svgUse('i-contactless')}</span>
      <span class="rb-text"><b>Ödemeye hazır</b><small>${c.title} · •••• ${c.last4}</small></span>
      <span class="rb-chip"></span>`;
  }

  function renderDetail() {
    const c = S.selected && card(S.selected);
    if (!c) return;
    if (c.kind === 'pay') {
      detail.innerHTML = `<div class="d-panel d-center">
          <div class="d-big-cl">${svgUse('i-contactless')}</div>
          <div class="d-head">Ödemeye hazır</div>
          <div class="d-foot">Telefonu çevir ve arkasını terminale yaklaştır.</div>
        </div>
        <div class="d-row"><span class="c-ico">${svgUse('i-card')}</span><span><b>${c.title}</b><small>Ödeme uygulaması</small></span></div>`;
    } else if (c.kind === 'loyalty') {
      detail.innerHTML = `<div class="d-panel">${ean13Svg(c.code)}<div class="barcode-num">${c.code}</div></div>
        <div class="d-caption">Ekran parlaklığı kod görünürken en yüksekte.</div>
        <div class="d-pills"><b>Puan</b><span>${c.points}</span></div>`;
    } else {
      detail.innerHTML = `<div class="d-panel d-center">
          <div class="d-head">Sahte kart</div>
          <div class="d-foot">Yalnızca görünüm. Ödeme terminaliyle konuşmaz, para çekmez.</div>
        </div>`;
    }
  }

  let toastTimer;
  function toast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.remove('show'), 1900);
  }

  function onCardTap(id) {
    if (S.selected === id) { S.selected = null; layoutCards(); return; }
    S.selected = id;
    const c = card(id);
    if (c.kind === 'pay' && S.ready !== id) {
      S.ready = id;
      toast(`Ödeme uygulaması: ${c.title}`);
      if (['idle', 'near'].includes(pos.dataset.state)) setPos(pos.dataset.state);
    }
    if (c.kind === 'pay') mission('tap');
    layoutCards();
  }

  function showPaid(c, amount) {
    paid.style.setProperty('--card', c.color);
    $('.paid-amount', paid).textContent = amount;
    $('.paid-card-text', paid).textContent = `${c.title}  •••• ${c.last4}`;
    paid.classList.remove('show');
    void paid.offsetWidth; // animasyonu yeniden başlat
    paid.classList.add('show');
    paid.setAttribute('aria-hidden', 'false');
    // Demo akmaya devam etsin: onay ekranı kendiliğinden kapanır, Tamam ile erken de kapatılabilir
    clearTimeout(paidTimer);
    paidTimer = setTimeout(hidePaid, 4200);
  }
  let paidTimer;
  function hidePaid() {
    clearTimeout(paidTimer);
    paid.classList.remove('show');
    paid.setAttribute('aria-hidden', 'true');
  }

  /* ───────────── çevirme ───────────── */
  function flip(to = !S.flipped) {
    S.flipped = to;
    inner.classList.toggle('flipped', to);
    flipBtn.setAttribute('aria-pressed', String(to));
    // Telefon zaten bir cihazın önündeyse çevirme bitince yeniden okut
    setTimeout(() => { zones.forEach(z => { if (z.inside) z.schedule(150); }); }, 720);
  }

  /* ───────────── POS ───────────── */
  let posBusy = false;
  function setPos(state) {
    pos.dataset.state = state;
    const c = card(S.ready);
    const text = {
      idle: [`Terminal hazır · ${S.amount}`, 'Kartı ya da telefonu yaklaştırın'],
      near: [`Bekleniyor · ${S.amount}`, 'Telefonu sabit tut'],
      reading: ['Okunuyor', 'Telefonu çekmeyin'],
      ok: [`Onaylandı · ${S.lastAmount || S.amount}`, `${c.title} •••• ${c.last4} · temassız`],
    }[state];
    $('.pos-readout .readout-text', pos).innerHTML = `<b>${text[0]}</b><small>${text[1]}</small>`;
  }
  function posLeds(n) { pos.querySelectorAll('.pos-leds i').forEach((l, i) => l.classList.toggle('on', i < n)); }

  function posRead() {
    if (posBusy) return false;
    if (!S.flipped) {
      // Temassız anten telefonun arkasında: yaklaşınca telefon arkasını kendisi çevirir.
      // flip() çevirme bitince bölgeyi yeniden okutur.
      setPos('near');
      flip(true);
      return false;
    }
    posBusy = true;
    const c = card(S.ready);
    const amount = S.amount;
    S.lastAmount = amount;
    setPos('reading');
    [1, 2, 3, 4].forEach(n => setTimeout(() => posLeds(n), n * 170));
    setTimeout(() => {
      setPos('ok');
      $('.pos-ok-amount', pos).textContent = amount;
      $('.r-amount', pos).textContent = amount.replace('₺', '') + ' TL';
      $('.r-card', pos).textContent = `${c.title} •••• ${c.last4}`;
      pos.classList.remove('tearing');
      pos.classList.add('printed');
      beepOk();
      showPaid(c, amount);
      mission('pay');
      setTimeout(() => { if (S.flipped) flip(false); }, 1100);
      setTimeout(posReset, 5200);
    }, 900);
    return true;
  }

  function posReset() {
    pos.classList.add('tearing');
    posLeds(0);
    const next = AMOUNTS[(AMOUNTS.indexOf(S.amount) + 1) % AMOUNTS.length];
    S.amount = next;
    $('.pos-amount', pos).textContent = next;
    setTimeout(() => {
      pos.classList.remove('printed', 'tearing');
      posBusy = false;
      setPos(zones[1].inside ? 'near' : 'idle');
      zones[1].armed = !zones[1].inside;
    }, 600);
  }

  /* ───────────── barkod okuyucu ───────────── */
  let scanTimer;
  function setScanner(state, title, sub) {
    scanner.dataset.state = state;
    $('.readout-text', scanner).innerHTML = `<b>${title}</b><small>${sub}</small>`;
  }
  function scannerIdle() {
    setScanner(zones[0].inside ? 'near' : 'idle', 'Okuyucu hazır', 'Barkodu ışığa tut');
  }
  function scanRead() {
    clearTimeout(scanTimer);
    if (S.flipped) {
      // Barkod ekranda: arkası dönükse telefon ön yüzünü okuyucuya çevirir
      setScanner('near', 'Okuyucu hazır', 'Ekran okuyucuya çevriliyor');
      flip(false);
      return false;
    }
    const c = S.selected && card(S.selected);
    const showsCode = c && c.kind === 'loyalty' && !paid.classList.contains('show');
    if (showsCode) {
      screen.classList.remove('scanning'); void screen.offsetWidth; screen.classList.add('scanning');
      setTimeout(() => {
        beepScan();
        setScanner('ok', `EAN-13 ${c.code}`, `${c.title} üyesi tanındı · ${c.points}`);
        mission('scan');
      }, 380);
      scanTimer = setTimeout(scannerIdle, 3600);
      return true;
    }
    beepWarn();
    setScanner('err', 'Barkod bulunamadı', 'Market kartını aç, kodu göster');
    scanTimer = setTimeout(scannerIdle, 2600);
    return false;
  }

  /* ───────────── yakınlık bölgeleri ───────────── */
  function makeZone(el, owner, threshold, onRead, onNear) {
    const z = {
      el, owner, threshold, inside: false, armed: true, timer: 0,
      schedule(delay = 480) {
        clearTimeout(z.timer);
        z.timer = setTimeout(() => {
          if (!z.inside || !z.armed) return;
          if (onRead()) z.armed = false; // başarılı okumadan sonra çıkıp tekrar gelmek gerekir
        }, delay);
      },
      update(phoneRect) {
        const r = el.getBoundingClientRect();
        const w = Math.max(0, Math.min(phoneRect.right, r.right) - Math.max(phoneRect.left, r.left));
        const h = Math.max(0, Math.min(phoneRect.bottom, r.bottom) - Math.max(phoneRect.top, r.top));
        const inside = (w * h) / (r.width * r.height) > threshold;
        if (inside === z.inside) return;
        z.inside = inside;
        onNear(inside);
        if (inside) z.schedule();
        else { clearTimeout(z.timer); z.armed = true; }
      },
    };
    return z;
  }
  const zones = [
    makeZone($('.zone', scanner), scanner, 0.3, scanRead, near => {
      if (['idle', 'near'].includes(scanner.dataset.state)) scanner.dataset.state = near ? 'near' : 'idle';
    }),
    makeZone($('.zone', pos), pos, 0.42, posRead, near => {
      wrap.classList.toggle('near-pos', near);
      if (['idle', 'near'].includes(pos.dataset.state)) setPos(near ? 'near' : 'idle');
    }),
  ];
  function checkZones() {
    const r = phone.getBoundingClientRect();
    zones.forEach(z => z.update(r));
  }

  /* ───────────── sahne ve sürükleme ───────────── */
  function placeDevice(el, [x, y, s]) {
    el.style.left = x + 'px';
    el.style.top = y + 'px';
    el.style.transform = `scale(${s})`;
  }
  function placePhone() {
    wrap.style.transform = `translate(${P.x - PHONE_W / 2}px, ${P.y - PHONE_H / 2}px)`;
    tilt.style.transform = `rotate(${P.rot}deg)`;
  }
  function fit() {
    const width = stageHost.clientWidth;
    const next = width < 760 ? LAYOUTS.narrow : LAYOUTS.wide;
    scale = Math.min(1, width / next.w);
    stage.style.width = next.w + 'px';
    stage.style.height = next.h + 'px';
    stage.style.left = Math.max(0, (width - next.w * scale) / 2) + 'px';
    stage.style.transform = `scale(${scale})`;
    stageHost.style.height = next.h * scale + 'px';
    if (next !== layout) {
      layout = next;
      placeDevice(scanner, layout.scanner);
      placeDevice(pos, layout.pos);
      [P.x, P.y] = layout.phone;
      placePhone();
    }
    requestAnimationFrame(checkZones);
  }

  let drag = null, suppressClick = false;
  wrap.addEventListener('pointerdown', e => {
    if (e.button !== 0 || e.target.closest('.flip-btn')) return;
    drag = { id: e.pointerId, sx: e.clientX, sy: e.clientY, ox: P.x, oy: P.y, moved: false, lx: e.clientX, lt: performance.now(), v: 0 };
  });
  window.addEventListener('pointermove', e => {
    if (!drag || e.pointerId !== drag.id) return;
    const dx = (e.clientX - drag.sx) / scale, dy = (e.clientY - drag.sy) / scale;
    if (!drag.moved) {
      if (Math.hypot(e.clientX - drag.sx, e.clientY - drag.sy) < 6) return;
      drag.moved = true;
      wrap.classList.add('dragging');
      try { wrap.setPointerCapture(e.pointerId); } catch (_) {}
    }
    e.preventDefault();
    P.x = clamp(drag.ox + dx, MIN_X, layout.w - 70);
    P.y = clamp(drag.oy + dy, 150, layout.h - 120);
    const now = performance.now();
    const v = (e.clientX - drag.lx) / Math.max(1, now - drag.lt);
    drag.v = drag.v * 0.7 + v * 0.3;
    drag.lx = e.clientX; drag.lt = now;
    P.rot = clamp(drag.v * 9, -9, 9);
    placePhone();
    checkZones();
  }, { passive: false });
  const endDrag = e => {
    if (!drag || e.pointerId !== drag.id) return;
    if (drag.moved) {
      suppressClick = true;
      setTimeout(() => { suppressClick = false; }, 0);
    }
    drag = null;
    wrap.classList.remove('dragging');
    P.rot = 0;
    placePhone();
    checkZones();
  };
  window.addEventListener('pointerup', endDrag);
  window.addEventListener('pointercancel', endDrag);
  wrap.addEventListener('click', e => {
    if (suppressClick) { e.stopPropagation(); e.preventDefault(); }
  }, true);

  // Klavye: odak telefondayken ok tuşları taşır, F çevirir
  wrap.addEventListener('keydown', e => {
    const step = e.shiftKey ? 40 : 14;
    const moves = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] };
    if (moves[e.key] && !e.target.closest('.card, button')) {
      e.preventDefault();
      P.x = clamp(P.x + moves[e.key][0], MIN_X, layout.w - 70);
      P.y = clamp(P.y + moves[e.key][1], 150, layout.h - 120);
      placePhone(); checkZones();
    }
  });
  phone.tabIndex = 0;
  document.addEventListener('keydown', e => {
    if ((e.key === 'f' || e.key === 'F') && !e.ctrlKey && !e.metaKey && !/input|textarea/i.test(e.target.tagName)) {
      if (stageHost.getBoundingClientRect().top < innerHeight && stageHost.getBoundingClientRect().bottom > 0) flip();
    }
  });

  /* ───────────── düğmeler ───────────── */
  flipBtn.addEventListener('click', () => flip());
  $('#doneBtn').addEventListener('click', () => { S.selected = null; layoutCards(); });
  $('#paidDone').addEventListener('click', hidePaid);
  const soundBtn = $('#soundBtn');
  soundBtn.addEventListener('click', () => {
    S.sound = !S.sound;
    soundBtn.textContent = S.sound ? 'Ses açık' : 'Ses kapalı';
    soundBtn.setAttribute('aria-pressed', String(S.sound));
  });
  $('#resetBtn').addEventListener('click', () => {
    S.selected = null; S.ready = 'b1'; S.missions.clear();
    document.querySelectorAll('.missions li').forEach(li => li.classList.remove('done'));
    hidePaid();
    if (S.flipped) flip(false);
    layout = null; fit();
    layoutCards();
    clearTimeout(scanTimer); scannerIdle();
    if (!posBusy) setPos('idle');
  });

  function mission(key) {
    if (S.missions.has(key)) return;
    S.missions.add(key);
    const li = document.querySelector(`.missions li[data-mission="${key}"]`);
    if (li) li.classList.add('done');
  }

  /* ───────────── başlat ───────────── */
  renderCards();
  layoutCards();
  fit();
  new ResizeObserver(fit).observe(stageHost);
  window.addEventListener('scroll', () => requestAnimationFrame(checkZones), { passive: true });
})();
