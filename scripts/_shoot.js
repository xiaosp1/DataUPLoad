const { spawnSync } = require('child_process');
const fs = require('fs');

const MSEDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const url = 'http://127.0.0.1:8080/js/w-perf-c-alarm.html';
const out = 'E:\\DEMO\\数据采集\\docs\\work-orders\\W-PERF-C-alarm.png';
const budget = 25000;
const profileDir = 'C:\\temp\\edge-alarm-' + Date.now();
fs.mkdirSync(profileDir, { recursive: true });
try { fs.unlinkSync(out); } catch (e) {}

const args = [
  '--headless=new',
  '--disable-gpu',
  '--no-sandbox',
  '--hide-scrollbars',
  '--window-size=1440,900',
  '--virtual-time-budget=' + budget,
  '--user-data-dir=' + profileDir,
  '--screenshot=' + out,
  url,
];

console.log('args:', args.join(' '));
const r = spawnSync(MSEDGE, args, { encoding: 'utf8', timeout: 90000 });
console.log('exit:', r.status, 'signal:', r.signal);
console.log('stderr (first 300):', (r.stderr || '').substring(0, 300));
console.log('stdout (first 300):', (r.stdout || '').substring(0, 300));
if (fs.existsSync(out)) {
  const sz = fs.statSync(out).size;
  const sha = require('crypto').createHash('sha256').update(fs.readFileSync(out)).digest('hex');
  console.log('bytes:', sz, 'sha:', sha);
} else {
  console.log('FILE NOT CREATED');
}
