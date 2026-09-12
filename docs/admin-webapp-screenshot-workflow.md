# Reproduzierbare Anleitung: Admin-Web-App-Screenshots aktualisieren (KuTu)

> Zweck: Screenshots für die neue Admin-Web-App (`newclient/kutu-app`) in der
> Dokumentation (`turner-wettkampf-app-doku`) reproduzierbar erzeugen bzw.
> aktualisieren, wenn sich die Admin-Oberfläche ändert.
> Diese Anleitung ist aus der Session vom 29.08.2026 extrahiert.

---

## Kontext

Zwei Repositories:

1. **Dokumentation** — `turner-wettkampf-app-doku` (GitBook-Markdown), Screenshots im Ordner `assets/`.
2. **Implementation** — `KuTu`. Die Admin-Web-App liegt unter `KuTu/newclient/kutu-app`.
   - Backend: Scala/Pekko-HTTP (sbt). Server wird in einem lokalen Datenverzeichnis betrieben.
   - Frontend: Angular 22 + Ionic 8. Die Admin-App wird vom lokalen Backend **selbst ausgeliefert** (SPA) — es ist NICHT nötig, `ng serve` zu starten.

### Lokale Server-Konfiguration (Standard)
- Config-Datei: `.server/kutuapp.conf`
  ```hocon
  app {
    fullversion = dev.server.test
    majorversion = dev.server
    remote {
      schema = "http"
      hostname = "localhost"
      port = 5757
    }
  }
  ```
- Server/Admin-App erreichbar unter: **`http://localhost:5757`** bzw. **`http://localhost:5757/admin`**
- Datenverzeichnis: `.server/kutuapp-server/kutuapp` (SQLite `db/kutu-dev.server.sqlite`)
  - Lokale Testwettkämpfe samt deren Admin-JWT liegen unter `.server/kutuapp-server/kutuapp/data/<WettkampfOrdner>/`.
  - Der Admin-JWT pro Wettkampf steht in der dortigen Datei **`.at.localhost`**.

### Warum "screenshotbar" ohne Anmeldung möglich ist
Jeder lokale Testwettkampf besitzt in `.at.localhost` einen **Admin-JWT** (Claim `admin=true`),
signiert mit dem Server-`jwtSecretKey`. Damit kann man sich gegen den lokalen Server
authentifizieren und sich einen Admin-Zugangs-Link erzeugen lassen.

---

## Voraussetzungen

- Google Chrome installiert (`/Applications/Google Chrome.app/.../Google Chrome`).
- Node.js (Node 25 getestet) + npm.
- Der lokale KuTu-Server läuft auf Port 5757 mit dem Datenverzeichnis `.server/`
  (enthält mindestens einen Testwettkampf mit `.at.localhost`).

> Tipp: Ein guter Testwettkampf mit realistischen Daten ist z.B. `GeTuTest`
> (UUID `2634ac89-b026-4d4b-b0da-671291000923`, 1 Verein "BTV Basel", 12 Anmeldungen).

---

## Schritt 1 — Admin-Access-Link vom Server holen

Den Admin-JWT aus der `.at.localhost` des gewünschten Wettkampfs lesen und damit den
Backend-Endpunkt `overview-links` aufrufen (liefert u.a. `adminAccessUrl`):

```bash
cd /Users/rolandseidel/IdeaProjects/KuTu
UUID="<Wettkampf-UUID>"                       # z.B. 2634ac89-b026-4d4b-b0da-671291000923
JWT=$(cat ".server/kutuapp-server/kutuapp/data/<WettkampfOrdner>/.at.localhost")
# Den Query-Teil (alles nach '?') des Links extrahieren:
ADMIN_URL=$(curl -s -H "x-access-token: $JWT" \
  "http://localhost:5757/api/competition/$UUID/overview-links" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['adminAccessUrl'])")
ADMIN_QUERY="${ADMIN_URL#*\?}"
echo "$ADMIN_QUERY"   # -> z.B. YWRtaW4mdXVpZD0yNjM0YWM4OS1...
```

Hintergrund: Der Query-Parameter ist base64-codiert und beginnt mit `admin&uuid=…&secret=…`.
Die Angular-App (`app.component.ts`) decodiert ihn, speichert das Secret in
`localStorage["kutu-admin-secrets"]` (Array von `{uuid, titel, datum, secret}`) und
navigiert nach `/admin/competitions`.

---

## Schritt 2 — Puppeteer-Skript (Drive Chrome headless, Mobile-Viewport)

Arbeitsverzeichnis anlegen und `puppeteer-core` installieren (treibt das installierte
Chrome, lädt KEIN eigenes Browser-Binary herunter):

```bash
TMP=/var/folders/kw/fmmzxy1n6tx56jt_0lk4w7pm0000gn/T/opencode/kutu-shots
mkdir -p "$TMP" && cd "$TMP"
npm init -y >/dev/null 2>&1
npm install puppeteer-core@24
```

`shoot.mjs` (Vorgabe aus der Referenz-Session — siehe unten) mit Umgebungsvariablen
starten:

```bash
cd "$TMP"
rm -rf chrome-profile
ADMIN_QUERY="$(cat /tmp/kutu-adminquery.txt)" \
OUT_DIR="/Users/rolandseidel/IdeaProjects/turner-wettkampf-app-doku/assets" \
node shoot.mjs
```

### Referenz-`shoot.mjs`

Grundgerüst (anpassbar). Mobile-Viewport 390×844, DPR 3 (ergibt 1170×2532 px):

```js
import puppeteer from 'puppeteer-core';

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const BASE = 'http://localhost:5757';
const UUID = process.env.UUID;
const ADMIN_QUERY = process.env.ADMIN_QUERY;
const OUT = process.env.OUT_DIR;
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
const waitFor = async (fn, t = 25000, i = 300) => {
  const s = Date.now();
  while (Date.now() - s < t) { try { if (await fn()) return; } catch {} await sleep(i); }
};

const browser = await puppeteer.launch({
  executablePath: CHROME, headless: 'new', userDataDir: './chrome-profile',
  args: ['--no-sandbox', '--hide-scrollbars']
});
const page = await browser.newPage();
await page.emulate({
  viewport: { width: 390, height: 844, deviceScaleFactor: 3, isMobile: true, hasTouch: true },
  userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) ...Safari/604.1'
});
const errs = [];
page.on('pageerror', e => errs.push(e.message));

// Admin-Zugang öffnen -> Secret wird gespeichert -> /admin/competitions
await page.goto(`${BASE}/?${ADMIN_QUERY}`, { waitUntil: 'networkidle2', timeout: 60000 });
await sleep(1500);
if (!page.url().includes('/admin/competitions')) {
  await page.goto(`${BASE}/admin/competitions`, { waitUntil: 'networkidle2', timeout: 60000 });
  await sleep(1500);
}
await waitFor(() => page.evaluate(() => document.body.innerText.includes('GeTuTest')));

// Wettkampf in "Meine Wettkämpfe" anklicken -> Admin-Übersicht
await page.evaluate(() => {
  for (const it of document.querySelectorAll('ion-item')) if (it.innerText.includes('GeTuTest')) { it.click(); return; }
});
await waitFor(() => page.evaluate(() => !!document.querySelector('ion-card')));
await sleep(1500);
await page.screenshot({ path: `${OUT}/webadmin-overview.png` });

// "Admin-Zugang übertragen" -> Modal mit Gültigkeitsdauer
await page.evaluate(() => {
  for (const it of document.querySelectorAll('ion-item')) if (it.innerText.includes('Admin-Zugang übertragen')) { it.click(); return; }
});
await waitFor(() => page.evaluate(() => document.body.innerText.includes('Gültigkeitsdauer')));
await sleep(1500);
await page.screenshot({ path: `${OUT}/webadmin-admin-access-link.png` });

// Modal schliessen, "Wettkampf löschen" -> Dialog (nur Screenshot, danach Abbrechen!)
await page.keyboard.press('Escape');
await sleep(1200);
await page.evaluate(() => {
  for (const it of document.querySelectorAll('ion-item')) if (it.innerText.includes('Wettkampf löschen')) { it.click(); return; }
});
await waitFor(() => page.evaluate(() => document.body.innerText.includes('Nur aus dieser Liste entfernen')));
await sleep(1500);
await page.screenshot({ path: `${OUT}/webadmin-competition-delete-dialog.png` });

// Abbrechen — nichts löschen!
await page.evaluate(() => {
  const b = [...document.querySelectorAll('.alert-button')].find(x => x.innerText.includes('Abbrechen'));
  if (b) b.click();
});
await sleep(800);
console.log('ERRORS:', errs.join(' | ') || 'none');
await browser.close();
```

---

## Schritt 3 — Screenshots prüfen

Das Modell kann Bilder nicht direkt ansehen. Deshalb den **gerenderten DOM-Inhalt** pro
Schritt verifizieren (per kurzem Skript), statt nur auf Dateigröße zu schauen:

- Admin-Übersicht enthält u.a.: `Startliste`, `Admin-Zugang übertragen`, `Wettkampf löschen`.
- Admin-Access-Modal enthält: `Gültigkeitsdauer` + `ion-select-option` mit Werten
  `1..365` und `unbegrenzte Tage` (0).
- Lösch-Dialog enthält beide Optionen: `Nur aus dieser Liste entfernen` / `Auch vom Server löschen`.

Beispiel-Verifikation fürs Modal (`verify3.mjs`-Ansatz):

```js
// Modal-SELECTOR: nicht der Komponenten-Tag, sondern:
const m = document.querySelector('ion-modal.admin-access-link-modal') || document.querySelector('ion-modal');
console.log(m.innerText);
console.log([...document.querySelectorAll('ion-modal ion-select-option')].map(o => o.value + '=' + o.innerText.trim()));
```

---

## Wichtige Hinweise / Fallstricke

- **Modal-Selektor**: Das Modal ist ein `ion-modal` mit `cssClass: 'admin-access-link-modal'`.
  Der Komponenten-Tag `admin-access-link-modal` ist im DOM NICHT direkt abfragbar.
- **`headless: 'new'`** + `userDataDir` auf ein separates Profil, damit kein bestehender
  Browserzustand / keine echten Secrets beeinflusst werden. Profil danach löschen.
- **Keine Daten verändern**: Beim Lösch-Dialog immer auf `Abbrechen` klicken. Nur Lese-/Navigation.
- **KuTu-Repo nicht verunreinigen**: `git status` in `KuTu` nach der Session prüfen —
  es darf nichts ausser evtl. `.server/kutuapp.conf` (unverändert) auftauchen. Nur die
  Doc-Repo-`assets/`-Dateien sind neue/modifizierte Artefakte.
- **Konsolen-Fehler**: Ein «Failed to load module script: MIME type» für einen lazy-chunk
  kann auftreten, ist aber unkritisch (Seiten renderten korrekt).
- **Admin-Link in der Doku ist Titel/Datum-abhängig**: `title=…&datum=…` stehen im base64-
  Payload; über `titel`/`datum` wird das `StoredSecret` befüllt.

---

## Ergebnis-Artefakte (Stand 29.08.2026)

Neu/aktualisiert in `turner-wettkampf-app-doku/assets/`:
- `webadmin-overview.png` — Admin-Übersicht (inkl. `Startliste` + `Admin-Zugang übertragen`)
- `webadmin-admin-access-link.png` — Modal «Admin-Zugang übertragen» mit `Gültigkeitsdauer`
- `webadmin-competition-delete-dialog.png` — Lösch-Dialog (2 Optionen)

Doc-Dateien, die darauf verweisen:
- `wettkampf-vorbereitung/webadmin.md`
- `wettkampf-vorbereitung/wettkampf_uebersicht.md`
- `stammdatenpflege/wettkampf_loschen.md`
