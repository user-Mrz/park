// 并发压力测试主脚本
// 用法: node run.js [并发数] [总请求数]
// 场景:
//   S1 微信登录并发 (wechat-login, 免验证码)
//   S2 公开车位概览并发读 (spot/overview)
//   S3 车位列表并发读 (spot/list)
//   S4 并发入场同一车位 (CAS 正确性: 只应 1 人成功)
//   S5 并发预约同一车位 (CAS 正确性: 只应 1 人成功)
//   S6 并发出场结算 (乐观锁扣款: 余额不为负)
//   S7 混合负载 (登录+读+入场+出场 混合)
'use strict';
const fs = require('fs');
const path = require('path');
const { BASE, runConcurrent, fmt, http } = require('./lib');

const CONC = parseInt(process.argv[2] || '50', 10);
const TOTAL = parseInt(process.argv[3] || '500', 10);
const usersFile = path.join(__dirname, 'users.json');
if (!fs.existsSync(usersFile)) {
  console.error('未找到 users.json，请先运行: node prepare.js <数量>');
  process.exit(1);
}
const USERS = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
console.log(`并发=${CONC} 总请求=${TOTAL} 测试用户=${USERS.length}\n`);

// 确保用户没有在场车辆 (清理上次残留)
async function clearUserParking(u) {
  const cur = await http('GET', '/vehicle/current', { token: u.token });
  if (cur.ok && cur.data && cur.data.id) {
    await http('POST', `/vehicle/exit/${cur.data.id}`, { token: u.token });
    return true;
  }
  return false;
}

// 清理所有测试用户的在场车辆
async function clearAllParking() {
  let cleared = 0;
  for (const u of USERS) {
    if (await clearUserParking(u)) cleared++;
  }
  if (cleared) console.log(`(清理 ${cleared} 辆残留在场车辆)`);
  return cleared;
}

// 查找空闲车位 (按需数量)
async function findFreeSpots(count) {
  const ov = await http('GET', '/spot/overview');
  const spots = [];
  for (const g of ov.data.areaGroups || []) {
    for (const s of g.spots || []) {
      if (s.status === 0) spots.push(s);
    }
  }
  return spots.slice(0, count);
}

// ---------- S1: 微信登录并发 ----------
// 注意: 部署 jar 无 deviceId 字段, 登录走 code 分支 (mockOpenid = 'wx_' + code)
// 因此用稳定 code (= deviceId) 复用已存在的压测用户, 避免触发 uk_phone 空串唯一索引缺陷
async function s1() {
  const r = await runConcurrent(TOTAL, CONC, async (i) => {
    const u = USERS[i % USERS.length];
    return http('POST', '/auth/wechat-login', {
      body: { code: u.deviceId, nickname: u.nickname },
    });
  }, { label: `S1 微信登录 (并发${CONC})` });
  console.log(fmt(r));
  return r;
}

// ---------- S2: 公开概览并发读 ----------
async function s2() {
  const r = await runConcurrent(TOTAL, CONC, async () => {
    return http('GET', '/spot/overview');
  }, { label: `S2 车位概览读 (并发${CONC})` });
  console.log(fmt(r));
  return r;
}

// ---------- S3: 车位列表并发读 (需登录) ----------
async function s3() {
  const token = USERS[0].token;
  const r = await runConcurrent(TOTAL, CONC, async () => {
    return http('GET', '/spot/list?page=1&pageSize=20', { token });
  }, { label: `S3 车位列表读 (并发${CONC})` });
  console.log(fmt(r));
  return r;
}

// ---------- S4: 并发入场同一车位 ----------
async function s4() {
  await clearAllParking();
  const spots = await findFreeSpots(1);
  if (!spots.length) { console.log('S4 跳过: 无空闲车位'); return null; }
  const spotId = spots[0].id;
  console.log(`S4 目标车位: ${spots[0].spotCode} (id=${spotId})`);

  const n = Math.min(USERS.length, CONC * 2);
  const r = await runConcurrent(n, n, async (i) => {
    const u = USERS[i];
    return http('POST', '/vehicle/enter', {
      token: u.token,
      body: { spotId, plateNumber: u.plate },
    });
  }, { label: `S4 并发入场同一车位 x${n} (CAS)` });
  console.log(fmt(r));
  console.log(`  -> 成功入场数=${r.ok} (预期=1), 失败=${r.fail} (预期=${n - 1})`);

  // 清理: 让成功入场的车辆出场, 释放车位
  await clearAllParking();
  return { ...r, n };
}

// ---------- S5: 并发预约同一车位 ----------
async function s5() {
  const spots = await findFreeSpots(1);
  if (!spots.length) { console.log('S5 跳过: 无空闲车位'); return null; }
  const spotId = spots[0].id;
  console.log(`S5 目标车位: ${spots[0].spotCode} (id=${spotId})`);

  const n = Math.min(USERS.length, CONC * 2);
  const start = new Date(Date.now() + 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  const fmtDT = (d) => {
    const p = (x) => String(x).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
  };
  const r = await runConcurrent(n, n, async (i) => {
    const u = USERS[i];
    return http('POST', '/reservation/create', {
      token: u.token,
      body: { spotId, startTime: fmtDT(start), endTime: fmtDT(end) },
    });
  }, { label: `S5 并发预约同一车位 x${n} (CAS)` });
  console.log(fmt(r));
  console.log(`  -> 成功预约数=${r.ok} (预期=1), 失败=${r.fail} (预期=${n - 1})`);

  // 清理: 取消成功的预约, 释放车位
  const resList = await http('GET', '/user/reservations?page=1&pageSize=50', { token: USERS[0].token });
  // 通过 admin 接口查询? 这里简化: 直接查最近创建的预约并取消
  for (const u of USERS.slice(0, n)) {
    const list = await http('GET', `/user/reservations?page=1&pageSize=10`, { token: u.token });
    if (list.ok && list.data && list.data.records) {
      for (const res of list.data.records) {
        if (res.spotId === spotId && res.status === 1) {
          await http('POST', `/reservation/cancel/${res.id}`, { token: u.token });
        }
      }
    }
  }
  return { ...r, n };
}

// ---------- S6: 并发出场结算 ----------
async function s6() {
  await clearAllParking();
  const freeSpots = await findFreeSpots(USERS.length);
  const n = Math.min(USERS.length, freeSpots.length);
  if (n < 2) { console.log('S6 跳过: 空闲车位不足'); return null; }

  // 1) 每个用户入场一个不同车位
  const entered = [];
  for (let i = 0; i < n; i++) {
    const u = USERS[i];
    const r = await http('POST', '/vehicle/enter', {
      token: u.token,
      body: { spotId: freeSpots[i].id, plateNumber: u.plate },
    });
    if (r.ok) entered.push({ ...u, spotId: freeSpots[i].id, recordId: r.data.id, price: freeSpots[i].pricePerHour });
  }
  console.log(`S6 已入场 ${entered.length} 辆车, 并发结算...`);
  if (entered.length < 2) { console.log('S6 跳过: 入场不足'); return null; }

  // 2) 同时出场 (并发结算)
  const r = await runConcurrent(entered.length, entered.length, async (i) => {
    const u = entered[i];
    return http('POST', `/vehicle/exit/${u.recordId}`, { token: u.token });
  }, { label: `S6 并发出场结算 x${entered.length} (乐观锁扣款)` });
  console.log(fmt(r));

  // 3) 验证余额一致性: 每个用户余额应 >= 0
  let okBalance = 0, badBalance = 0, deducted = 0;
  for (const u of entered) {
    const info = await http('GET', '/user/info', { token: u.token });
    const bal = Number(info.data.balance);
    if (bal < 0) { badBalance++; console.log(`  !! 用户 ${u.userId} 余额为负: ${bal}`); }
    else okBalance++;
    // 是否有扣费记录
    const pay = await http('GET', '/user/payment/records?page=1&pageSize=5', { token: u.token });
    if (pay.ok && pay.data && pay.data.records && pay.data.records.length) deducted++;
  }
  console.log(`  -> 余额非负校验: 通过=${okBalance}, 异常=${badBalance}; 有扣费记录=${deducted}/${entered.length}`);
  return { ...r, n: entered.length, okBalance, badBalance, deducted };
}

// ---------- S7: 混合负载 ----------
async function s7() {
  await clearAllParking();
  const freeSpots = await findFreeSpots(30);
  const nPark = Math.min(15, USERS.length, freeSpots.length);
  const parkPool = [];
  for (let i = 0; i < nPark; i++) {
    const u = USERS[i % USERS.length];
    const r = await http('POST', '/vehicle/enter', {
      token: u.token,
      body: { spotId: freeSpots[i].id, plateNumber: u.plate },
    });
    if (r.ok) parkPool.push({ token: u.token, recordId: r.data.id });
  }
  console.log(`S7 混合负载: 预停 ${parkPool.length} 辆车, 出场请求池=${parkPool.length}`);

  let exitUsed = 0;
  const r = await runConcurrent(TOTAL, CONC, async (i) => {
    const u = USERS[i % USERS.length];
    const pick = i % 100;
    if (pick < 40) {
      return http('POST', '/auth/wechat-login', {
        body: { code: u.deviceId, nickname: u.nickname },
      });
    } else if (pick < 70) {
      return http('GET', '/spot/overview');
    } else if (pick < 90) {
      return http('GET', '/spot/list?page=1&pageSize=20', { token: u.token });
    } else {
      // 10% 出场, 每个 recordId 只结算一次 (幂等)
      if (!parkPool.length) return http('GET', '/spot/overview');
      const p = parkPool[exitUsed++ % parkPool.length];
      if (p.used) return http('GET', '/spot/overview');
      p.used = true;
      return http('POST', `/vehicle/exit/${p.recordId}`, { token: p.token });
    }
  }, { label: `S7 混合负载 40%登录/30%概览/20%列表/10%出场 (并发${CONC})` });
  console.log(fmt(r));
  return r;
}

(async () => {
  const results = [];
  results.push(await s1());
  results.push(await s2());
  results.push(await s3());
  results.push(await s4());
  results.push(await s5());
  results.push(await s6());
  results.push(await s7());

  const out = path.join(__dirname, 'results.json');
  fs.writeFileSync(out, JSON.stringify(results.filter(Boolean), null, 2));
  console.log(`\n结果已保存: ${out}`);
})().catch(e => { console.error(e); process.exit(1); });
