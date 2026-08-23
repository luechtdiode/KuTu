// start with `node wssmoke.mjs`

const HOST = 'http://localhost:5757';
const WS = 'ws://localhost:5757';
const COMP = '2634ac89-b026-4d4b-b0da-671291000923';

function mkSocket(url, name) {
  const s = { name, url, ws: null, welcome: false, keepAlives: 0, otherMsgs: 0, closedAt: null, openAt: null, errors: [] };
  return new Promise((resolve) => {
    let settled = false;
    try { s.ws = new WebSocket(url); } catch (e) { s.errors.push('ctor:' + e.message); resolve(s); return; }
    s.ws.onopen = () => { s.openAt = Date.now(); };
    s.ws.onmessage = (evt) => {
      if (typeof evt.data === 'string' && evt.data.startsWith('Connection established.')) { s.welcome = true; return; }
      if (evt.data === 'keepAlive') { s.keepAlives++; return; }
      s.otherMsgs++;
    };
    s.ws.onclose = (evt) => { s.closedAt = Date.now(); s.closeCode = evt.code; if (!settled) { settled = true; resolve(s); } };
    s.ws.onerror = (e) => { s.errors.push(e.message || 'error'); };
    setTimeout(() => { if (!settled) { settled = true; resolve(s); } }, 500);
  });
}

const t0 = Date.now();
const elapsed = () => ((Date.now() - t0) / 1000).toFixed(1) + 's';
const results = [];

async function main() {
  // Phase A: competition channel + registration channel concurrently (multi-channel coexistence)
  results.push(await mkSocket(`${WS}/api/durchgang/${COMP}/all/ws?clientid=smoketest-comp-A`, 'comp-A (competition channel)'));
  results[0].opened = elapsed();
  console.log(`[${elapsed()}] opened comp-A`);
  results.push(await mkSocket(`${WS}/api/registrations/${COMP}/sync-ws?clientid=smoketest-reg-B`, 'reg-B (registration channel)'));
  results[1].opened = elapsed();
  console.log(`[${elapsed()}] opened reg-B`);

  await new Promise(r => setTimeout(r, 2000));

  // Phase B: second device on competition channel
  results.push(await mkSocket(`${WS}/api/durchgang/${COMP}/all/ws?clientid=smoketest-comp-C`, 'comp-C (2nd device, same channel)'));
  console.log(`[${elapsed()}] opened comp-C`);

  await new Promise(r => setTimeout(r, 2000));

  // Phase D: legacy pattern - two sockets sharing ONE clientid (what old code did; documents deferred backend bug)
  results.push(await mkSocket(`${WS}/api/durchgang/${COMP}/all/ws?clientid=smoketest-dup-D`, 'dup-D1 (same clientid #1)'));
  results.push(await mkSocket(`${WS}/api/durchgang/${COMP}/all/ws?clientid=smoketest-dup-D`, 'dup-D2 (same clientid #2)'));

  // observe for 32s -> >=3 keepAlive cycles @10s
  const deadline = Date.now() + 32000;
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 4000));
    const line = results.map(s => `${s.name}: ka=${s.keepAlives}${s.closedAt ? ' CLOSED(code=' + s.closeCode + ')' : ''}`).join(' | ');
    console.log(`[${elapsed()}] ${line}`);
  }

  console.log('\n=== RESULT ===');
  let pass = true;
  for (const s of results) {
    const isDup = s.name.startsWith('dup');
    const ok = s.welcome && !s.closedAt && s.keepAlives >= 3;
    if (!isDup && !ok) pass = false;
    console.log(`${ok ? 'PASS' : (isDup ? 'INFO' : 'FAIL')}  ${s.name}`);
    console.log(`      url=${s.url.replace(WS, '')}`);
    console.log(`      welcome=${s.welcome} keepAlives=${s.keepAlives} dataEvents=${s.otherMsgs} closed=${!!s.closedAt}${s.closedAt ? ' code=' + s.closeCode : ''} errors=${JSON.stringify(s.errors)}`);
  }
  console.log(pass ? '\nSMOKE TEST PASSED' : '\nSMOKE TEST FAILED');
  for (const s of results) { try { s.ws && s.ws.close(); } catch {} }
  process.exit(pass ? 0 : 1);
}

main().catch(e => { console.error(e); process.exit(2); });
