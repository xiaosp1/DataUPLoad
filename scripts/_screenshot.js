const fs = require('fs');
const crypto = require('crypto');
const { spawnSync } = require('child_process');

const MSEDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const SPA_JS = 'C:\\temp\\w-perf-c-js';
const OUT_DIR = 'E:\\DEMO\\数据采集\\docs\\work-orders';

// Step 1: Login and get satoken
async function getToken() {
  const pw = crypto.createHash('sha256').update('Abc12345').digest('hex');
  const r = await fetch('http://127.0.0.1:8080/web/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'super_admin', password: pw }),
  });
  const setCookie = r.headers.get('set-cookie') || '';
  const m = setCookie.match(/satoken=([^;]+)/);
  if (!m) throw new Error('No satoken');
  console.log('satoken: ' + m[1].substring(0, 8) + '...');
  return m[1];
}

// Step 2: Write auto-login HTML files INTO the SPA origin (DataupLoad/web/js/)
function deployLoginFiles(token) {
  const alarmHtml = `<!DOCTYPE html>
<html><head><meta charset='utf-8'><title>auto-alarm</title></head>
<body>
<script>
document.cookie = 'satoken=${token}; path=/; max-age=2592000; SameSite=Lax';
window.location.replace('http://127.0.0.1:8080/#/alarm');
</script>
</body></html>`;
  const realtimeHtml = `<!DOCTYPE html>
<html><head><meta charset='utf-8'><title>auto-realtime</title></head>
<body>
<script>
document.cookie = 'satoken=${token}; path=/; max-age=2592000; SameSite=Lax';
window.location.replace('http://127.0.0.1:8080/#/realtime');
</script>
</body></html>`;
  // Deploy to the SPA's static dir so it's served from the same origin
  fs.writeFileSync('E:/DEMO/数据采集/DataupLoad/web/js/w-perf-c-alarm.html', alarmHtml, 'utf8');
  fs.writeFileSync('E:/DEMO/数据采集/DataupLoad/web/js/w-perf-c-realtime.html', realtimeHtml, 'utf8');
  console.log('Deployed auto-login HTMLs to SPA origin');
  
  // Also put in clean path
  fs.mkdirSync(SPA_JS, { recursive: true });
  fs.writeFileSync(SPA_JS + '/alarm.html', alarmHtml, 'utf8');
  fs.writeFileSync(SPA_JS + '/realtime.html', realtimeHtml, 'utf8');
}

// Step 3: Screenshot one URL
function shoot(url, outPath, budgetMs, label) {
  console.log(`\n[${label}] ${url}`);
  try { fs.unlinkSync(outPath); } catch (e) {}
  const profileDir = 'C:\\temp\\edge-shoot-' + Date.now();
  fs.mkdirSync(profileDir, { recursive: true });
  const args = [
    '--headless=new', '--disable-gpu', '--no-sandbox', '--hide-scrollbars',
    '--window-size=1440,900',
    '--virtual-time-budget=' + budgetMs,
    '--user-data-dir=' + profileDir,
    '--screenshot=' + outPath,
    url,
  ];
  console.log('  args: ' + args.filter(a => !a.startsWith('--user-data')).join(' ') + ' [profile omitted]');
  const r = spawnSync(MSEDGE, args, { encoding: 'utf8', timeout: 120000 });
  const exists = fs.existsSync(outPath);
  const sz = exists ? fs.statSync(outPath).size : 0;
  console.log('  exit=' + r.status + ' exists=' + exists + ' bytes=' + sz);
  return { exists, bytes: sz };
}

(async () => {
  try {
    const token = await getToken();
    deployLoginFiles(token);

    // Use SPA-URL for auto-login HTML (served from same origin)
    const res = [];

    // Alarm page
    const r1 = shoot(
      'http://127.0.0.1:8080/js/w-perf-c-alarm.html',
      OUT_DIR + '/W-PERF-C-alarm.png',
      30000,
      'ALARM'
    );
    res.push(r1);

    // Realtime page
    const r2 = shoot(
      'http://127.0.0.1:8080/js/w-perf-c-realtime.html',
      OUT_DIR + '/W-PERF-C-realtime.png',
      40000,
      'REALTIME'
    );
    res.push(r2);

    // Cleanup auto-login files from SPA
    try { fs.unlinkSync('E:/DEMO/数据采集/DataupLoad/web/js/w-perf-c-alarm.html'); } catch (e) {}
    try { fs.unlinkSync('E:/DEMO/数据采集/DataupLoad/web/js/w-perf-c-realtime.html'); } catch (e) {}
    console.log('cleanup done');

    // Verify files are different
    const f1 = OUT_DIR + '/W-PERF-C-alarm.png';
    const f2 = OUT_DIR + '/W-PERF-C-realtime.png';
    if (fs.existsSync(f1) && fs.existsSync(f2)) {
      const h1 = crypto.createHash('sha256').update(fs.readFileSync(f1)).digest('hex');
      const h2 = crypto.createHash('sha256').update(fs.readFileSync(f2)).digest('hex');
      console.log('alarm sha: ' + h1.substring(0, 16));
      console.log('realtime sha: ' + h2.substring(0, 16));
      if (h1 === h2) console.log('WARNING: screenshots are IDENTICAL');
      else console.log('Screenshots are DIFFERENT ✓');
    }

    console.log('\nDone. Result:', JSON.stringify({ alarm: r1, realtime: r2 }));
  } catch (e) {
    console.error('FATAL:', e.message);
    process.exit(1);
  }
})();
