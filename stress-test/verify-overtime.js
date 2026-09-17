// 场景2: 预约1小时但超时停车(实际3小时) -> 费用=max(实际, 预约金额), 多占多付
'use strict';
const { http } = require('./lib');
const fs = require('fs');
const { execSync } = require('child_process');
const USERS = JSON.parse(fs.readFileSync(require('path').join(__dirname, 'users.json'), 'utf8'));
const fmtDT = (d) => { const p = (x) => String(x).padStart(2, '0'); return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds()); };
const MYSQL = 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';

(async () => {
  const u6 = USERS[6];
  let cur = await http('GET', '/vehicle/current', { token: u6.token });
  if (cur.ok && cur.data && cur.data.id) await http('POST', '/vehicle/exit/' + cur.data.id, { token: u6.token });
  const ov = await http('GET', '/spot/overview');
  let spot = null;
  for (const g of ov.data.areaGroups || []) for (const s of g.spots || []) if (s.status === 0 && s.pricePerHour === 3) { spot = s; break; }
  if (!spot) { console.log('无3元车位'); process.exit(1); }
  const start = new Date(Date.now() + 30 * 60 * 1000);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  const createRes = await http('POST', '/reservation/create', { token: u6.token, body: { spotId: spot.id, startTime: fmtDT(start), endTime: fmtDT(end) } });
  console.log('预约金额(1小时×3元): ' + createRes.data.totalAmount);
  const ent = await http('POST', '/vehicle/enter', { token: u6.token, body: { spotId: spot.id, plateNumber: u6.plate, reservationId: createRes.data.id } });
  const recordId = ent.data.id;
  execSync('"' + MYSQL + '" --host=127.0.0.1 --user=root --password=123456 --database=parking_system --execute="UPDATE t_vehicle_record SET entry_time = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE id = ' + recordId + ';"', { stdio: 'ignore' });
  const before = Number((await http('GET', '/user/info', { token: u6.token })).data.balance);
  const ex = await http('POST', '/vehicle/exit/' + recordId, { token: u6.token });
  const after = Number((await http('GET', '/user/info', { token: u6.token })).data.balance);
  console.log('场景2 超时停车离场: 费用=' + ex.data.totalFee + ' (预期=9, 3小时×3元, 超预约5元按实际), 扣款=' + (before - after).toFixed(2));
  console.log('  ' + (Number(ex.data.totalFee) === 9 ? '✓ 超时停车按实际时长计费(多占多付)' : '✗ 异常: ' + ex.data.totalFee));
})().catch(e => { console.error(e); process.exit(1); });
