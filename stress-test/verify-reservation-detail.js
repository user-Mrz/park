// 验证: 预约详情"预约不存在或已失效" bug 修复
// 流程: 1) 创建活跃预约 2) 调 /spot/overview 取预约id(应为字符串) 3) 用该id调 /reservation/{id} 详情
'use strict';
const fs = require('fs');
const path = require('path');
const { BASE, http } = require('./lib');

const USERS = JSON.parse(fs.readFileSync(path.join(__dirname, 'users.json'), 'utf8'));
const u = USERS[7];
const fmtDT = (d) => { const p = (x) => String(x).padStart(2, '0'); return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds()); };

(async () => {
  // 清理用户已有预约(取消所有活跃预约)
  const list = await http('GET', '/user/reservations?page=1&pageSize=20', { token: u.token });
  for (const r of (list.data && list.data.records || [])) {
    if (r.status === 1) await http('POST', `/reservation/cancel/${r.id}`, { token: u.token });
  }

  // 1) 创建活跃预约
  const ov0 = await http('GET', '/spot/overview');
  let spot = null;
  for (const g of ov0.data.areaGroups || []) for (const s of g.spots || []) if (s.status === 0) { spot = s; break; }
  const start = new Date(Date.now() + 30 * 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  const createRes = await http('POST', '/reservation/create', {
    token: u.token,
    body: { spotId: spot.id, startTime: fmtDT(start), endTime: fmtDT(end) },
  });
  console.log(`1) 创建预约: id=${createRes.data.id} (类型=${typeof createRes.data.id})`);

  // 2) 从 overview 拿预约 id (模拟首页数据源)
  const ov = await http('GET', '/spot/overview');
  const myRes = (ov.data.activeReservations || []).find(r => String(r.id) === String(createRes.data.id));
  if (!myRes) { console.log('✗ overview 中未找到刚创建的预约'); process.exit(1); }
  console.log(`2) overview 返回 id=${myRes.id} (类型=${typeof myRes.id})`);
  const idOk = typeof myRes.id === 'string';
  console.log(`   id 为字符串(无精度丢失): ${idOk ? '✓' : '✗'}`);

  // 3) 用 overview 的 id 调详情接口 (模拟小程序 reserve-detail 页)
  const detail = await http('GET', '/reservation/' + myRes.id, { token: u.token });
  console.log(`3) 详情接口: ${detail.ok ? '✓ 找到预约' : '✗ 返回 ' + detail.msg}`);
  if (detail.ok) console.log(`   预约编号=${detail.data.reservationNo}, 状态=${detail.data.status}, 金额=${detail.data.totalAmount}`);

  // 4) 关键断言: 前端会走到 "!reservation" 分支吗?
  const frontendShowsError = !detail.ok || !detail.data;
  console.log(`4) 前端显示"预约不存在或已失效": ${frontendShowsError ? '✗ 仍有问题' : '✓ 已修复'}`);

  // 清理: 取消预约
  await http('POST', `/reservation/cancel/${createRes.data.id}`, { token: u.token });

  const allOk = idOk && detail.ok && !frontendShowsError;
  console.log(allOk ? '\n✅ 修复验证通过' : '\n❌ 仍存在问题');
  process.exit(allOk ? 0 : 1);
})().catch(e => { console.error(e); process.exit(1); });
