// W-PERF-C curl benchmark
// Compares 7d (BEFORE) vs 1h (AFTER) response times
const crypto = require('crypto');

async function run() {
  // Login
  const pw = crypto.createHash('sha256').update('Abc12345').digest('hex');
  const r = await fetch('http://127.0.0.1:8080/web/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'super_admin', password: pw }),
  });
  const loginJson = await r.json();
  if (!loginJson.success) {
    console.error('Login failed:', JSON.stringify(loginJson));
    process.exit(1);
  }
  console.log('Login OK');

  const now = new Date();
  const pad = n => String(n).padStart(2, '0');
  const fmt = d => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  const nowStr = fmt(now);
  const hourAgo = fmt(new Date(now.getTime() - 3600000));
  const weekAgo = fmt(new Date(now.getTime() - 7*86400000));

  async function bench(label, url) {
    const times = [];
    for (let i = 0; i < 5; i++) {
      const t0 = Date.now();
      const res = await fetch(url);
      const body = await res.text();
      const ms = Date.now() - t0;
      times.push(ms);
    }
    const avg = Math.round(times.reduce((a,b)=>a+b,0) / times.length);
    console.log(`${label}: avg=${avg}ms (${times.join(',')})`);
    return { label, times, avg };
  }

  // Test 1: 7d baseline (pageSize=20, 7d range)
  const url7d = `http://127.0.0.1:8080/web/alarm/list?pageNum=1&pageSize=20&startTime=${encodeURIComponent(weekAgo)}&endTime=${encodeURIComponent(nowStr)}`;
  const r7d = await bench('7d (pageSize=20)', url7d);

  // Test 2: 1h default (pageSize=20, 1h range)
  const url1h = `http://127.0.0.1:8080/web/alarm/list?pageNum=1&pageSize=20&startTime=${encodeURIComponent(hourAgo)}&endTime=${encodeURIComponent(nowStr)}`;
  const r1h = await bench('1h (pageSize=20)', url1h);

  // Test 3: realtime KPI (pageSize=1, today 00:00 to now)
  const todayStart = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} 00:00:00`;
  const urlSz1 = `http://127.0.0.1:8080/web/alarm/list?pageNum=1&pageSize=1&startTime=${encodeURIComponent(todayStart)}&endTime=${encodeURIComponent(nowStr)}`;
  const rSz1 = await bench('realtime KPI (pageSize=1, today)', urlSz1);

  console.log('---');
  console.log('SPEEDUP: 7d→1h = ' + Math.round(r7d.avg / r1h.avg) + 'x');
  console.log('1h < 200ms TARGET: ' + (r1h.avg < 200 ? 'PASS ✓' : 'FAIL'));
  const result = { r7d, r1h, rSz1 };
  require('fs').writeFileSync('E:\\DEMO\\数据采集\\docs\\work-orders\\W-PERF-C-curl-results.json', JSON.stringify(result, null, 2));
  console.log('Results saved to W-PERF-C-curl-results.json');
}
run().catch(e => { console.error(e); process.exit(1); });
