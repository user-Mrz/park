// 管理员接口压测: 本地签发 admin JWT, 压测 /api/admin/** 只读接口
// 用法: node admin-stress.js [并发] [总数]
'use strict';
const crypto = require('crypto');
const { runConcurrent, fmt, http } = require('./lib');

const CONC = parseInt(process.argv[2] || '50', 10);
const TOTAL = parseInt(process.argv[3] || '500', 10);
const SECRET = 'parking-system-secret-key-2024-parking-lot-management-jwt-token';

function signAdminJwt(adminId) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const payload = { sub: String(adminId), phone: '', role: 'admin', iat: now, exp: now + 7200 };
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const data = `${b64(header)}.${b64(payload)}`;
  const sig = crypto.createHmac('sha256', Buffer.from(SECRET, 'utf8')).update(data).digest('base64url');
  return `${data}.${sig}`;
}

(async () => {
  const token = signAdminJwt(1);
  // 验证 admin token
  const check = await http('GET', '/admin/dashboard', { token });
  console.log('admin token 验证:', check.ok ? 'OK' : JSON.stringify(check));

  const scenarios = [
    { label: `A1 工作台 dashboard (并发${CONC})`, path: '/admin/dashboard' },
    { label: `A2 用户列表 (并发${CONC})`, path: '/admin/users?page=1&pageSize=20' },
    { label: `A3 车位列表 (并发${CONC})`, path: '/admin/spots?page=1&pageSize=20' },
    { label: `A4 停车记录 (并发${CONC})`, path: '/admin/vehicle-records?page=1&pageSize=20' },
    { label: `A5 充值记录 (并发${CONC})`, path: '/admin/recharge-records?page=1&pageSize=20' },
    { label: `A6 扣费记录 (并发${CONC})`, path: '/admin/payment-records?page=1&pageSize=20' },
  ];
  for (const s of scenarios) {
    const r = await runConcurrent(TOTAL, CONC, async () => http('GET', s.path, { token }), { label: s.label });
    console.log(fmt(r));
  }
})().catch(e => { console.error(e); process.exit(1); });
