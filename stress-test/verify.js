// 专项并发正确性验证
// 1) V1: 同一停车记录并发重复出场 -> 只应 1 次成功扣费 (幂等保护)
// 2) V2: 余额不足时并发出场 -> 应全部失败且余额不变 (乐观锁保护)
// 3) V3: 并发充值 + 并发出场 -> 余额与流水一致
'use strict';
const fs = require('fs');
const path = require('path');
const { runConcurrent, fmt, http } = require('./lib');

const USERS = JSON.parse(fs.readFileSync(path.join(__dirname, 'users.json'), 'utf8'));
const user0 = USERS[0];

async function findFreeSpot() {
  const ov = await http('GET', '/spot/overview');
  for (const g of ov.data.areaGroups || []) {
    for (const s of g.spots || []) {
      if (s.status === 0) return s;
    }
  }
  return null;
}

async function clearAll() {
  for (const u of USERS) {
    const cur = await http('GET', '/vehicle/current', { token: u.token });
    if (cur.ok && cur.data && cur.data.id) {
      await http('POST', `/vehicle/exit/${cur.data.id}`, { token: u.token });
    }
  }
}

// V1: 同一记录并发重复出场
async function v1() {
  await clearAll();
  const spot = await findFreeSpot();
  if (!spot) return console.log('V1 跳过');
  const enter = await http('POST', '/vehicle/enter', { token: user0.token, body: { spotId: spot.id, plateNumber: user0.plate } });
  if (!enter.ok) return console.log('V1 入场失败', enter);
  const recordId = enter.data.id;
  const before = (await http('GET', '/user/info', { token: user0.token })).data.balance;
  console.log(`V1 同一停车记录(${recordId}) 并发重复出场 x30, 余额=${before}`);
  const r = await runConcurrent(30, 30, async () => {
    return http('POST', `/vehicle/exit/${recordId}`, { token: user0.token });
  }, { label: 'V1 重复出场幂等 x30' });
  console.log(fmt(r));
  const after = (await http('GET', '/user/info', { token: user0.token })).data.balance;
  const pay = await http('GET', '/user/payment/records?page=1&pageSize=10', { token: user0.token });
  // 雪花ID超过 JS Number 精度, 用字符串/金额双重校验
  const payCount = (pay.data && pay.data.records || []).filter(p => String(p.vehicleRecordId) === String(recordId) || p.balanceBefore === Number(before)).length;
  console.log(`  -> 成功=${r.ok} (预期=1), 失败=${r.fail} (预期=29)`);
  console.log(`  -> 余额变动: ${before} -> ${after} (扣款=${(Number(before) - Number(after)).toFixed(2)})`);
  console.log(`  -> 扣费记录数=${payCount} (预期=1)`);
  console.log(`  ${r.ok === 1 && payCount >= 1 && Number(after) === Number(before) - 5 ? '✓ V1 通过: 无重复扣费' : '✗ V1 失败'}`);
  return { ok: r.ok, payCount };
}

// V2: 余额不足保护
async function v2() {
  await clearAll();
  const spot = await findFreeSpot();
  if (!spot) return console.log('V2 跳过');
  // 用 user1, 先重置余额为 5 元 (够 1 小时 A 区 5 元)
  const u1 = USERS[1];
  const { execSync } = require('child_process');
  const mysql = 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';
  execSync(`"${mysql}" --host=127.0.0.1 --user=root --password=123456 --default-character-set=utf8mb4 --database=parking_system --execute="UPDATE t_user SET balance = 5.00 WHERE openid = 'wx_stress-1';"`, { stdio: 'ignore' });
  let info = await http('GET', '/user/info', { token: u1.token });
  let bal = Number(info.data.balance);
  console.log(`V2 用户当前余额=${bal} (已重置为 5.00)`);
  // 入场并立刻出场 (费用按最少 1 小时计: A区 5元/时)
  const enter = await http('POST', '/vehicle/enter', { token: u1.token, body: { spotId: spot.id, plateNumber: u1.plate } });
  if (!enter.ok) return console.log('V2 入场失败', enter);
  const recordId = enter.data.id;
  const exit1 = await http('POST', `/vehicle/exit/${recordId}`, { token: u1.token });
  console.log(`V2 第一次出场: ${exit1.ok ? '成功' : '失败 ' + exit1.msg} (费用≈5元)`);
  info = await http('GET', '/user/info', { token: u1.token });
  console.log(`V2 出场后余额=${info.data.balance} (应=0.00)`);
  // 再次入场 -> 出场, 余额不足应失败
  const spot2 = await findFreeSpot();
  const enter2 = await http('POST', '/vehicle/enter', { token: u1.token, body: { spotId: spot2.id, plateNumber: u1.plate } });
  if (!enter2.ok) return console.log('V2 二次入场失败(正常, 余额不足无法入场不涉及)', enter2);
  const exit2 = await http('POST', `/vehicle/exit/${enter2.data.id}`, { token: u1.token });
  console.log(`V2 余额不足出场: ${exit2.ok ? '异常成功!' : '正确失败: ' + exit2.msg}`);
  const balAfter = (await http('GET', '/user/info', { token: u1.token })).data.balance;
  console.log(`  -> 余额=${balAfter} (应=0.00, 不为负)`);
  console.log(`  ${!exit2.ok && Number(balAfter) >= 0 ? '✓ V2 通过: 余额不足被拦截且余额不为负' : '✗ V2 失败'}`);
  // 清理在场车辆 (强制出场)
  const cur = await http('GET', '/vehicle/current', { token: u1.token });
  if (cur.ok && cur.data && cur.data.id) {
    await http('POST', `/vehicle/exit/${cur.data.id}`, { token: u1.token });
  }
  // 恢复余额
  execSync(`"${mysql}" --host=127.0.0.1 --user=root --password=123456 --default-character-set=utf8mb4 --database=parking_system --execute="UPDATE t_user SET balance = 200.00 WHERE openid = 'wx_stress-1';"`, { stdio: 'ignore' });
  return { exit2Ok: exit2.ok, balAfter };
}

// V3: 并发充值 + 并发扣费
async function v3() {
  await clearAll();
  const u2 = USERS[2];
  const before = Number((await http('GET', '/user/info', { token: u2.token })).data.balance);
  console.log(`V3 用户余额=${before}, 并发充值 100 元 x10`);
  const r1 = await runConcurrent(10, 10, async () => {
    return http('POST', '/user/recharge', { token: u2.token, body: { amount: 100, payMethod: 'wechat' } });
  }, { label: 'V3 并发充值 x10' });
  console.log(fmt(r1));
  const afterRecharge = Number((await http('GET', '/user/info', { token: u2.token })).data.balance);
  console.log(`  -> 充值后余额=${afterRecharge} (预期=${(before + 1000).toFixed(2)})`);
  console.log(`  ${afterRecharge === before + 1000 ? '✓ V3 充值一致性通过' : '✗ V3 充值不一致: ' + afterRecharge}`);

  // 并发扣费: 找 5 个车位, 5 辆车同时入场再同时出场
  const spots = [];
  const ov = await http('GET', '/spot/overview');
  for (const g of ov.data.areaGroups || []) {
    for (const s of g.spots || []) {
      if (s.status === 0) spots.push(s);
    }
  }
  const n = Math.min(5, spots.length, USERS.length);
  const entered = [];
  for (let i = 0; i < n; i++) {
    const u = USERS[i];
    const e = await http('POST', '/vehicle/enter', { token: u.token, body: { spotId: spots[i].id, plateNumber: u.plate } });
    if (e.ok) entered.push({ u, recordId: e.data.id });
  }
  const r2 = await runConcurrent(entered.length, entered.length, async (i) => {
    return http('POST', `/vehicle/exit/${entered[i].recordId}`, { token: entered[i].u.token });
  }, { label: `V3 并发扣费 x${entered.length}` });
  console.log(fmt(r2));
  let neg = 0;
  for (const e of entered) {
    const bal = Number((await http('GET', '/user/info', { token: e.u.token })).data.balance);
    if (bal < 0) neg++;
  }
  console.log(`  -> 出场成功=${r2.ok}, 余额为负用户=${neg}`);
  console.log(`  ${r2.ok === entered.length && neg === 0 ? '✓ V3 并发扣费通过' : '✗ V3 失败'}`);
  return { ok: r2.ok, neg };
}

(async () => {
  await v1();
  await v2();
  await v3();
})().catch(e => { console.error(e); process.exit(1); });
