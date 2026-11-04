// ICN Line Roster — service worker
// app shell cache (roster data always comes live from Google) + push notifications (wake-up call / flight alerts)
const CACHE = 'icn-roster-shell-v2';
const SHELL = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

// same-origin shell files: network first, cache as fallback
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== self.location.origin) return;
  e.respondWith(
    fetch(e.request)
      .then(res => { const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); return res; })
      .catch(() => caches.match(e.request).then(r => r || caches.match('./index.html')))
  );
});

// push from Firebase Cloud Messaging (data message: title / body / tag / kind)
self.addEventListener('push', e => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch (err) { d = { body: e.data ? e.data.text() : '' }; }
  const p = d.data || d.notification || d;
  const wake = p.kind === 'wake';
  e.waitUntil(self.registration.showNotification(p.title || 'ICN Roster', {
    body: p.body || '',
    tag: p.tag || undefined,
    renotify: !!p.tag,
    icon: 'icon-192.png',
    badge: 'icon-192.png',
    requireInteraction: wake,                       // wake-up call stays until tapped (Android / desktop)
    vibrate: wake ? [500, 200, 500, 200, 900] : [200, 100, 200],
    data: { url: './' },
  }));
});

// tap → open / focus the app
self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(ws => {
    for (const w of ws) if ('focus' in w) return w.focus();
    return self.clients.openWindow('./');
  }));
});
