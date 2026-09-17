// 预约提前离场结算 端到端验证
// 流程: 1) 用户预约车位(2小时) 2) 入场 3) 提前离场 4) 验证费用=预约金额且扣款正确
'use strict';
const fs = require('fs');
const path = require('path');
const { BASE, http } = require('./lib');

const USERS = JSON.parse(fs.readFileSync(path.join(__dirname, 'users.json'), 'utf8'));
const u = USERS[4]; // 用第5个用户

const fmtDT = (d) => {
  const p = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
};

async function findFreeSpot() {
  const ov = await http('GET', '/spot/overview');
  for (const g of ov.data.areaGroups || []) {
    for (const s of g.spots || []) {
      if (s.status === 0) return s;
    }
  }
  return null;
}

(async () => {
  // 清理用户残留
  const cur = await http('GET', '/vehicle/current', { token: u.token });
  if (cur.ok && cur.data && cur.data.id) {
    await http('POST', `/vehicle/exit/${cur.data.id}`, { token: u.token });
  }

  const spot = await findFreeSpot();
  if (!spot) { console.log('无空闲车位'); process.exit(1); }
  console.log(`目标车位: ${spot.spotCode} (id=${spot.id}, 费率 ¥${spot.pricePerHour}/时)`);

  // 1) 创建预约: 从1小时后开始, 时长2小时
  const start = new Date(Date.now() + 60 * 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  const reservationAmount = spot.pricePerHour * 2;
  console.log(`创建预约: ${fmtDT(start)} ~ ${fmtDT(end)} (2小时, 预约金额 ¥${reservationAmount})`);
  const createRes = await http('POST', '/reservation/create', {
    token: u.token,
    body: { spotId: spot.id, startTime: fmtDT(start), endTime: fmtDT(end) },
  });
  if (!createRes.ok) { console.log('✗ 创建预约失败:', createRes.msg); process.exit(1); }
  const reservationId = createRes.data.id;
  console.log(`✓ 预约成功 id=${reservationId}`);

  // 2) 入场 (关联预约)
  const balanceBefore = Number((await http('GET', '/user/info', { token: u.token })).data.balance);
  console.log(`入场前余额: ${balanceBefore}`);
  const enter = await http('POST', '/vehicle/enter', {
    token: u.token,
    body: { spotId: spot.id, plateNumber: u.plate, reservationId },
  });
  if (!enter.ok) { console.log('✗ 入场失败:', enter.msg); process.exit(1); }
  console.log(`✓ 入场成功 recordId=${enter.data.id}`);

  // 3) 验证 current 接口返回预约金额
  const current = await http('GET', '/vehicle/current', { token: u.token });
  console.log(`✓ current 接口: reservationAmount=${current.data.reservationAmount} (应=${reservationAmount})`);

  // 4) 提前离场 (入场后立即, 实际停车≈0分钟, 预约2小时未使用完)
  const exit = await http('POST', `/vehicle/exit/${enter.data.id}`, { token: u.token });
  if (!exit.ok) { console.log('✗ 提前离场失败:', exit.msg); process.exit(1); }
  const balanceAfter = Number((await http('GET', '/user/info', { token: u.token })).data.balance);
  const deducted = balanceBefore - balanceAfter;
  console.log(`✓ 提前离场成功`);
  console.log(`  离场后余额: ${balanceAfter}, 扣款: ${deducted.toFixed(2)}`);
  console.log(`  记录费用: ${exit.data.totalFee} (预期=预约金额 ${reservationAmount})`);

  // 5) 验证扣费记录类型
  const pay = await http('GET', '/user/payment/records?page=1&pageSize=5', { token: u.token });
  const latest = (pay.data.records || [])[0];
  console.log(`  最新扣费记录: type=${latest.type}, amount=${latest.amount} (预期 type=reservation_settle)`);

  // 6) 断言
  const feeOk = Number(exit.data.totalFee) === reservationAmount;
  const dedOk = Math.abs(deducted - reservationAmount) < 0.01;
  const typeOk = latest.type === 'reservation_settle';
  const curOk = Number(current.data.reservationAmount) === reservationAmount;
  console.log('\n===== 断言 =====');
  console.log(`费用=预约金额: ${feeOk ? '✓' : '✗'} (${exit.data.totalFee} vs ${reservationAmount})`);
  console.log(`扣款=预约金额: ${dedOk ? '✓' : '✗'} (${deducted.toFixed(2)} vs ${reservationAmount})`);
  console.log(`结算类型=reservation_settle: ${typeOk ? '✓' : '✗'}`);
  console.log(`current返回预约金额: ${curOk ? '✓' : '✗'}`);
  const allOk = feeOk && dedOk && typeOk && curOk;
  console.log(allOk ? '\n✅ 全部通过: 预约提前离场按预约金额结算' : '\n❌ 存在失败项');
  process.exit(allOk ? 0 : 1);
})().catch(e => { console.error(e); process.exit(1); });
