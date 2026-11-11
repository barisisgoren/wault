/* Ortak: dar ekranda açılır menü. */
(() => {
  'use strict';
  const nav = document.querySelector('.nav');
  const btn = document.querySelector('.menu-btn');
  if (!nav || !btn) return;

  const setOpen = open => {
    nav.classList.toggle('open', open);
    btn.setAttribute('aria-expanded', String(open));
    btn.setAttribute('aria-label', open ? 'Menüyü kapat' : 'Menü');
  };

  btn.addEventListener('click', () => setOpen(!nav.classList.contains('open')));
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && nav.classList.contains('open')) { setOpen(false); btn.focus(); }
  });
  // Menü dışına dokununca kapanır
  document.addEventListener('click', e => {
    if (nav.classList.contains('open') && !nav.contains(e.target)) setOpen(false);
  });
  // Geniş ekrana geçince açık kalmasın
  matchMedia('(min-width: 641px)').addEventListener('change', e => { if (e.matches) setOpen(false); });
})();
