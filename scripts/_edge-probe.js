const { spawnSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const MSEDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

function shoot(url, outPng, budgetMs, label) {
  console.log(`\n[${label}] url=${url} → ${outPng} (budget=${budgetMs}ms)`);
  const profileDir = 'C:\\temp\\edge-profile-' + Date.now();
  fs.mkdirSync(profileDir, { recursive: true });
  // First clean any prior file
  try { fs.unlinkSync(outPng); } catch (e) {}
  const args = [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    '--hide-scrollbars',
    '--window-size=1440,900',
    '--virtual-time-budget=' + budgetMs,
    '--user-data-dir=' + profileDir,
    '--screenshot=' + outPng,
    url,
  ];
  const r = spawnSync(MSEDGE, args, { encoding: 'utf8', timeout: 60000 });
  console.log('  exit=' + r.status + ' stderr=' + (r.stderr || '').substring(0, 300));
  if (fs.existsSync(outPng)) {
    const sz = fs.statSync(outPng).size;
    const h = require('crypto').createHash('sha256').update(fs.readFileSync(outPng)).digest('hex').substring(0, 16);
    console.log(`  bytes=${sz} sha=${h}`);
  } else {
    console.log('  FILE NOT CREATED');
  }
}

// Test 1: direct /index.html (no auth — should redirect to login)
shoot('http://127.0.0.1:8080/index.html', 'C:\\temp\\test-direct.png', 10000, 'direct-index');

// Test 2: /js/w-perf-c-alarm.html (already deployed from previous run? no, was unlinked)
shoot('http://127.0.0.1:8080/js/w-perf-c-alarm.html', 'C:\\temp\\test-autoalarm.png', 10000, 'auto-alarm');

// Test 3: a totally different URL to confirm files are independent
shoot('http://127.0.0.1:8080/index.html#/alarm', 'C:\\temp\\test-hashtarget.png', 10000, 'hash-target');
