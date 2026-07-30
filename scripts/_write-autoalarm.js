const fs = require('fs');
const crypto = require('crypto');

(async () => {
  const pw = crypto.createHash('sha256').update('Abc12345').digest('hex');
  const r = await fetch('http://127.0.0.1:8080/web/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'super_admin', password: pw })
  });
  const setCookie = r.headers.get('set-cookie');
  const m = setCookie.match(/satoken=([^;]+)/);
  const token = m[1];
  const html = '<!DOCTYPE html><html><body><script>document.cookie="satoken=' + token + '; path=/; max-age=2592000"; window.location.replace("http://127.0.0.1:8080/#/alarm");</script></body></html>';
  fs.writeFileSync('E:/DEMO/数据采集/DataupLoad/web/js/w-perf-c-alarm.html', html, 'utf8');
  console.log('wrote. token=', token.substring(0, 8), '...');
  // Verify
  const r2 = await fetch('http://127.0.0.1:8080/js/w-perf-c-alarm.html');
  console.log('verify status:', r2.status);
})();
