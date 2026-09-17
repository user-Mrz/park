// 核心业务流程 E2E 测试(黑盒)
// 用法: node e2e-core.js [--keep]   默认结束时做 API 级清理; --keep 保留现场
'use strict';

const net = require('net');
const path = require('path');
const { spawnSync } = require('child_process');

const BASE = 'http://localhost:8080/api';
const MYSQL = 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const ADMIN = { username: 'admin', password: 'admin123' };
const KEEP = process.argv.includes('--keep');

const results = [];
let passCount = 0;
let failCount = 0;
const usedSpotIds = new Set(); // 本测试使用过的车位
const registeredUsers = [];    // { phone, password, userId, token, openidLike }

function check(name, ok, detail) {
  if (ok) passCount++; else failCount++;
  results.push({ name, ok, detail: detail || '' });
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name}${detail ? '  -> ' + detail : ''}`);
}

function section(t) { console.log(`\n===== ${t} =====`); }

// ---------- HTTP ----------
async function req(method, p, { token, body, timeout = 20000 } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeout);
  try {
    const res = await fetch(BASE + p, {
      method, headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: ctrl.signal,
    });
    let data = null;
    try { data = await res.json(); } catch { /* empty */ }
    const ok = data && data.code === 200;
    return { httpStatus: res.status, ok, code: data ? data.code : null, msg: data ? (data.message || '') : '', data: ok ? data.data : null, raw: data };
  } finally { clearTimeout(timer); }
}

// ---------- Redis (轻量协议, 免外部命令) ----------
function redisGet(key) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(6379, '127.0.0.1');
    sock.setTimeout(3000);
    sock.on('connect', () => sock.write(`*2\r\n$3\r\nGET\r\n$${Buffer.byteLength(key)}\r\n${key}\r\n`));
    sock.on('data', (buf) => {
      sock.destroy();
      const s = buf.toString();
      if (s.startsWith('$')) {
        const m = s.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/);
        resolve(m && m[1] !== '-1' ? m[2] : null);
      } else resolve(null);
    });
    sock.on('error', reject);
    sock.on('timeout', () => { sock.destroy(); reject(new Error('redis timeout')); });
  });
}

async function getCaptcha() {
  const r = await req('GET', '/auth/captcha');
  if (!r.ok) throw new Error('captcha api failed: ' + JSON.stringify(r.raw));
  const code = await redisGet('captcha:' + r.data.key);
  if (!code) throw new Error('captcha code not found in redis');
  return { key: r.data.key, code };
}

async function register(phone, password, nickname, plateNumber) {
  const cap = await getCaptcha();
  const r = await req('POST', '/auth/register', { body: { phone, password, nickname, plateNumber, captchaKey: cap.key, captchaCode: cap.code } });
  if (r.ok && r.data && r.data.userId != null) r.data.userId = String(r.data.userId); // 雪花ID超JS安全整数
  return r;
}

async function loginPhone(phone, password) {
  const cap = await getCaptcha();
  const r = await req('POST', '/auth/login', { body: { phone, password, captchaKey: cap.key, captchaCode: cap.code } });
  if (r.ok && r.data && r.data.userId != null) r.data.userId = String(r.data.userId);
  return r;
}

async function loginAdmin() {
  const cap = await getCaptcha();
  const r = await req('POST', '/auth/admin/login', { body: { username: ADMIN.username, password: ADMIN.password, captchaKey: cap.key, captchaCode: cap.code } });
  if (r.ok && r.data && r.data.adminId != null) r.data.adminId = String(r.data.adminId);
  return r;
}

// ---------- DB 辅助 ----------
function sql(statement) {
  const r = spawnSync(MYSQL, ['-uroot', '-p123456', '--database=parking_system', '-N', '-B', '-e', statement], { encoding: 'utf8' });
  if (r.error) throw r.error;
  return (r.stdout || '').trim();
}

async function makeUser(tag, { noPlate = false } = {}) {
  const phone = '1398899' + String(1000 + registeredUsers.length).slice(-4);
  const pwd = 'test123456';
  const r = await register(phone, pwd, 'E2E-' + tag, noPlate ? null : '粤B0000' + registeredUsers.length);
  if (!r.ok) throw new Error('makeUser ' + tag + ' failed: ' + r.msg);
  // 服务器返回的 userId 是未序列化的雪花长整型(超出JS安全整数), 从 /user/info(字符串序列化)取准确值
  let userId = r.data.userId;
  const info = await req('GET', '/user/info', { token: r.data.token });
  if (info.ok && info.data && info.data.id) userId = String(info.data.id);
  registeredUsers.push({ tag, phone, password: pwd, userId, token: r.data.token });
  return registeredUsers[registeredUsers.length - 1];
}

// 空闲车位查询; /spot/list 需要登录态, 使用 spotTok
let spotTok = null;
async function findVacantSpot(used = usedSpotIds) {
  if (!spotTok) throw new Error('spotTok not set');
  const r = await req('GET', '/spot/list?page=1&pageSize=100', { token: spotTok });
  if (!r.ok) throw new Error('spot list failed: ' + r.msg);
  const rec = (r.data.records || []).find((s) => s.status === 0 && !used.has(String(s.id)));
  if (!rec) throw new Error('no vacant spot available');
  usedSpotIds.add(String(rec.id));
  return rec;
}

async function spotStatus(spotId) {
  const r = await req('GET', '/spot/overview');
  if (!r.ok) return null;
  for (const g of r.data.areaGroups || []) {
    const s = (g.spots || []).find((x) => String(x.id) === String(spotId));
    if (s) return s.status;
  }
  return null;
}

function nowPlus(mins) {
  const d = new Date(Date.now() + mins * 60000);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

const sleep = (ms) => new Promise((res) => setTimeout(res, ms));

async function runCleanup(adminToken) {
  section('清理');
  try {
    if (adminToken) {
      for (const u of registeredUsers) {
        const cur = await req('GET', '/vehicle/current', { token: u.token });
        if (cur.ok && cur.data) await req('POST', '/vehicle/exit/' + cur.data.id, { token: u.token });
      }
    }
    const phones = registeredUsers.map((u) => u.phone).filter(Boolean).map((p) => `'${p}'`).join(',');
    const openids = registeredUsers.filter((u) => u.openidLike).map((u) => `'${u.openidLike}'`).join(',');
    if (phones) {
      sql(`DELETE FROM t_vehicle_record WHERE user_id IN (SELECT id FROM t_user WHERE phone IN (${phones}))`);
      sql(`DELETE FROM t_payment_record WHERE user_id IN (SELECT id FROM t_user WHERE phone IN (${phones}))`);
      sql(`DELETE FROM t_recharge_record WHERE user_id IN (SELECT id FROM t_user WHERE phone IN (${phones}))`);
      sql(`DELETE FROM t_reservation WHERE user_id IN (SELECT id FROM t_user WHERE phone IN (${phones}))`);
      sql(`DELETE FROM t_user WHERE phone IN (${phones})`);
    }
    if (openids) {
      sql(`DELETE FROM t_vehicle_record WHERE user_id IN (SELECT id FROM t_user WHERE openid IN (${openids}))`);
      sql(`DELETE FROM t_payment_record WHERE user_id IN (SELECT id FROM t_user WHERE openid IN (${openids}))`);
      sql(`DELETE FROM t_recharge_record WHERE user_id IN (SELECT id FROM t_user WHERE openid IN (${openids}))`);
      sql(`DELETE FROM t_reservation WHERE user_id IN (SELECT id FROM t_user WHERE openid IN (${openids}))`);
      sql(`DELETE FROM t_user WHERE openid IN (${openids})`);
    }
    sql(`DELETE FROM t_parking_spot WHERE spot_code LIKE 'E2E-%'`);
    console.log('API 级清理完成(车位状态/系统配置请另行执行 snapshot 恢复)');
  } catch (e) { console.log('清理异常: ' + e.message); }
}

async function main() {
  let adminToken = null;
  let A = null;
  try {
    // ================= 1. 认证 =================
    section('1. 认证 / 注册 / 登录');
    try {
      const cap = await getCaptcha();
      check('T01 验证码接口返回 key 与图片', !!cap.key && cap.code.length >= 1, `key=${cap.key ? cap.key.slice(0, 8) + '...' : 'null'}`);
    } catch (e) { check('T01 验证码接口', false, e.message); }

    const badCap = await getCaptcha();
    const badLogin = await req('POST', '/auth/login', { body: { phone: '13800138000', password: '123456', captchaKey: badCap.key, captchaCode: 'zzzz' } });
    check('T02 错误验证码被拒绝', !badLogin.ok && /captcha/i.test(badLogin.msg || ''), badLogin.msg);

    try {
      const ar = await loginAdmin();
      adminToken = ar.ok ? ar.data.token : null;
      check('T03 管理员登录成功', ar.ok && !!ar.data.token && ar.data.role === 'super', ar.ok ? `adminId=${ar.data.adminId}` : ar.msg);
    } catch (e) { check('T03 管理员登录成功', false, e.message); }

    const dl = await loginPhone('13800138000', '123456');
    check('T04 用户登录成功', dl.ok && !!dl.data.token, dl.ok ? `userId=${dl.data.userId} balance=${dl.data.balance}` : dl.msg);

    const wlBad = await loginPhone('13800138000', 'wrong-pass');
    check('T05 错误密码登录被拒', !wlBad.ok && /password/i.test(wlBad.msg || ''), wlBad.msg);

    // 供 /spot/list 等需要登录的查询使用
    spotTok = adminToken || (dl.ok ? dl.data.token : null);

    // ================= 2. 用户 A 主流程 =================
    section('2. 用户注册 / 资料 / 充值');
    const phoneA = '1398899' + String(2000).slice(-4); // 13988992000
    try {
      const badReg = await register('12345', '123456', 'x', '');
      check('T06 非法手机号注册被拒', !badReg.ok && badReg.httpStatus === 400 && /phone/i.test(badReg.msg || ''), `http=${badReg.httpStatus} msg=${badReg.msg}`);

      const dup1 = await register(phoneA, 'test123456', 'E2E-A', '粤BAA001');
      check('T07 正常注册成功', dup1.ok && dup1.data.token, dup1.msg || '');
      check('T07b 注册返回userId为字符串(避免JS精度丢失)', dup1.ok && typeof dup1.data.userId === 'string' && /^\d{15,19}$/.test(dup1.data.userId),
        dup1.ok ? `type=${typeof dup1.data.userId} value=${dup1.data.userId}` : dup1.msg);
      if (dup1.ok) {
        let userId = dup1.data.userId;
        const iA = await req('GET', '/user/info', { token: dup1.data.token });
        if (iA.ok && iA.data && iA.data.id) userId = String(iA.data.id); // 精确id
        registeredUsers.push({ tag: 'A', phone: phoneA, password: 'test123456', userId, token: dup1.data.token });
        A = registeredUsers[registeredUsers.length - 1];
        spotTok = spotTok || A.token;
      }
      const dup2 = await register(phoneA, 'test123456', 'E2E-A', '粤BAA001');
      check('T08 重复手机号注册被拒', !dup2.ok && /already registered/i.test(dup2.msg || ''), dup2.msg);
    } catch (e) { check('T06-T08 注册流程', false, e.message); }

    if (A) {
      const up = await req('PUT', '/user/info', { token: A.token, body: { nickname: 'E2E用户A', plateNumber: '粤BAA001' } });
      check('T09 修改昵称/车牌成功', up.ok, up.msg);
      const info = await req('GET', '/user/info', { token: A.token });
      check('T10 资料已更新', info.ok && info.data.nickname === 'E2E用户A' && info.data.plateNumber === '粤BAA001',
        info.ok ? `nick=${info.data.nickname} plate=${info.data.plateNumber} balance=${info.data.balance}` : info.msg);

      const rc = await req('POST', '/user/recharge', { token: A.token, body: { amount: 100, payMethod: 'wechat' } });
      check('T11 充值100元成功', rc.ok, rc.ok ? `orderNo=${rc.data.orderNo}` : rc.msg);
      const info2 = await req('GET', '/user/info', { token: A.token });
      check('T12 余额增加为100.00', info2.ok && Number(info2.data.balance) === 100, info2.ok ? 'balance=' + info2.data.balance : info2.msg);

      const rc0 = await req('POST', '/user/recharge', { token: A.token, body: { amount: 0 } });
      check('T13 充值0元被拒', !rc0.ok, rc0.msg);

      const rr = await req('GET', '/user/recharge/records', { token: A.token });
      check('T14 充值记录列表', rr.ok && rr.data.records.length >= 1 && Number(rr.data.records[0].amount) === 100, rr.ok ? `total=${rr.data.total}` : rr.msg);
    }

    // ================= 3. 微信登录 =================
    section('3. 微信登录 mock');
    try {
      const wc1 = await req('POST', '/auth/wechat-login', { body: { code: 'x1', deviceId: 'e2e-dev-1', nickname: 'WX-E2E' } });
      if (!wc1.ok) {
        check('T15 微信登录(新设备)成功', false, `msg=${wc1.msg} code=${wc1.code} http=${wc1.httpStatus}`);
      } else {
        const wc2 = await req('POST', '/auth/wechat-login', { body: { code: 'x2', deviceId: 'e2e-dev-1', nickname: 'WX-E2E' } });
        const same = wc2.ok && String(wc1.data.userId) === String(wc2.data.userId);
        check('T15 同一设备二次登录返回同一用户', same, wc2.ok ? `userId=${wc1.data.userId}` : ('第二次登录失败: ' + wc2.msg));
        check('T15b 微信登录userId为字符串', wc1.ok && typeof wc1.data.userId === 'string' && /^\d{15,19}$/.test(wc1.data.userId),
          wc1.ok ? `type=${typeof wc1.data.userId} value=${wc1.data.userId}` : wc1.msg);
        if (wc1.ok) registeredUsers.push({ tag: 'WX', phone: wc1.data.phone || '', password: '', userId: String(wc1.data.userId), token: wc1.data.token, openidLike: 'wx_e2e-dev-1' });
      }
    } catch (e) { check('T15 微信登录', false, e.message); }

    // ================= 4. 车位 =================
    section('4. 车位列表 / 公共概览');
    const spotList = await req('GET', '/spot/list?page=1&pageSize=20', { token: spotTok });
    check('T16 车位列表返回记录', spotList.ok && spotList.data.records.length > 0, spotList.ok ? `total=${spotList.data.total}` : spotList.msg);
    const ov = await req('GET', '/spot/overview');
    check('T17 公共概览结构完整', ov.ok && Array.isArray(ov.data.areaGroups) && Array.isArray(ov.data.parkedVehicles) && Array.isArray(ov.data.activeReservations),
      ov.ok ? `areaGroups=${ov.data.areaGroups.length} parked=${ov.data.parkedVehicles.length} reserv=${ov.data.activeReservations.length}` : ov.msg);

    // ================= 5. 预约主流程 =================
    section('5. 预约流程');
    if (A) {
      try {
        const s = await findVacantSpot();
        const cr = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(120), endTime: nowPlus(240) } });
        check('T18 创建预约成功(2小时)', cr.ok && cr.data.status === 1 && Number(cr.data.durationHours) === 2 && Number(cr.data.totalAmount) === Number(s.pricePerHour) * 2,
          cr.ok ? `id=${cr.data.id} amount=${cr.data.totalAmount}` : cr.msg);
        const st2 = await spotStatus(s.id);
        check('T19 预约后车位状态=预约(2)', st2 === 2, `status=${st2}`);

        const cr2 = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(300), endTime: nowPlus(400) } });
        check('T20 重复预约同一车位被拒', !cr2.ok, cr2.msg);

        const crPast = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(-10), endTime: nowPlus(60) } });
        check('T21 过去时间预约被拒', !crPast.ok && /past/i.test(crPast.msg || ''), crPast.msg);

        const crShort = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(60), endTime: nowPlus(90) } });
        check('T22 不足1小时预约被拒', !crShort.ok && /1 hour/i.test(crShort.msg || ''), crShort.msg);

        const crLong = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(60), endTime: nowPlus(60 + 31 * 24 * 60) } });
        check('T23 超30天预约被拒', !crLong.ok, crLong.msg);

        const cx = await req('POST', '/reservation/cancel/' + cr.data.id, { token: A.token });
        check('T24 取消预约成功', cx.ok, cx.msg);
        const st3 = await spotStatus(s.id);
        check('T25 取消后车位释放为空闲(0)', st3 === 0, `status=${st3}`);
      } catch (e) { check('T18-T25 预约流程', false, e.message); }
    }

    // ================= 6. 预约入场->出场结算 =================
    section('6. 预约入场->出场自动扣费');
    if (A) {
      try {
        const s = await findVacantSpot();
        const cr = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(60), endTime: nowPlus(180) } });
        if (!cr.ok) throw new Error('create reservation: ' + cr.msg);
        const expectedResvAmount = Number(cr.data.totalAmount);

        const en = await req('POST', '/vehicle/enter', { token: A.token, body: { spotId: String(s.id), reservationId: String(cr.data.id), plateNumber: '粤BAA001' } });
        check('T26 凭预约入场成功', en.ok && String(en.data.reservationId) === String(cr.data.id) && en.data.status === 0, en.ok ? `recordId=${en.data.id}` : en.msg);

        const cur = await req('GET', '/vehicle/current', { token: A.token });
        check('T27 当前在场车辆查询', cur.ok && cur.data && String(cur.data.id) === String(en.data.id), cur.ok ? `plate=${cur.data.plateNumber}` : (cur.msg || 'no current'));

        const rd = await req('GET', '/reservation/' + cr.data.id, { token: A.token });
        check('T28 入场后预约状态=已完成(2)', rd.ok && rd.data.status === 2, rd.ok ? 'status=' + rd.data.status : rd.msg);

        const balBefore = Number((await req('GET', '/user/info', { token: A.token })).data.balance);
        await sleep(1500);
        const ex = await req('POST', '/vehicle/exit/' + en.data.id, { token: A.token });
        const balAfter = Number((await req('GET', '/user/info', { token: A.token })).data.balance);
        const fee = ex.ok ? Number(ex.data.totalFee) : -1;
        check('T29 预约出场按预约金额结算(max)', ex.ok && fee === expectedResvAmount, ex.ok ? `fee=${fee} expected=${expectedResvAmount} minutes=${ex.data.durationMinutes}` : ex.msg);
        check('T30 出场扣费正确(余额-费用)', ex.ok && balBefore - balAfter === fee, `before=${balBefore} after=${balAfter} fee=${fee}`);
        const stf = await spotStatus(s.id);
        check('T31 出场后车位释放为空闲(0)', stf === 0, `status=${stf}`);
        const ex2 = await req('POST', '/vehicle/exit/' + en.data.id, { token: A.token });
        check('T32 重复出场被拒', !ex2.ok, ex2.msg);

        const pr = await req('GET', '/user/payment/records', { token: A.token });
        check('T33 扣费记录生成', pr.ok && pr.data.records.length >= 1 && Number(pr.data.records[0].amount) === fee,
          pr.ok ? `type=${pr.data.records[0].type} amount=${pr.data.records[0].amount}` : pr.msg);
      } catch (e) { check('T26-T33 预约入场出场', false, e.message); }
    }

    // ================= 7. 直接入场/出场 =================
    section('7. 无预约直接停车');
    if (A) {
      try {
        const s = await findVacantSpot();
        const rate = Number(s.pricePerHour);
        const en = await req('POST', '/vehicle/enter', { token: A.token, body: { spotId: String(s.id), plateNumber: '粤BAA001' } });
        check('T34 直接入场成功', en.ok && en.data.status === 0, en.ok ? `recordId=${en.data.id}` : en.msg);

        const s2 = await findVacantSpot();
        const en2 = await req('POST', '/vehicle/enter', { token: A.token, body: { spotId: String(s2.id), plateNumber: '粤BAA001' } });
        check('T35 在场状态下再次入场被拒', !en2.ok && /already have a vehicle/i.test(en2.msg || ''), en2.msg);

        const balB = Number((await req('GET', '/user/info', { token: A.token })).data.balance);
        await sleep(1500);
        const ex = await req('POST', '/vehicle/exit/' + en.data.id, { token: A.token });
        const balA2 = Number((await req('GET', '/user/info', { token: A.token })).data.balance);
        const fee = ex.ok ? Number(ex.data.totalFee) : -1;
        check('T36 短停出场按1小时起收', ex.ok && fee === rate && ex.data.paymentStatus === 1, ex.ok ? `fee=${fee} rate=${rate} minutes=${ex.data.durationMinutes}` : ex.msg);
        check('T37 出场扣费正确', ex.ok && balB - balA2 === fee, `before=${balB} after=${balA2}`);
        const st = await spotStatus(s.id);
        check('T38 直接出场后车位释放', st === 0, `status=${st}`);
      } catch (e) { check('T34-T38 直接停车', false, e.message); }
    }

    // ================= 7b. 计费边界(整小时起步) =================
    section('7b. 计费边界回归(1分钟/90分钟 -> 分别按1h/2h整小时计费)');
    if (A) {
      try {
        await req('POST', '/user/recharge', { token: A.token, body: { amount: 50 } });
        const s = await findVacantSpot();
        const rate = Number(s.pricePerHour);
        const en = await req('POST', '/vehicle/enter', { token: A.token, body: { spotId: String(s.id), plateNumber: '粤BAA001' } });
        if (!en.ok) throw new Error('enter: ' + en.msg);
        sql(`UPDATE t_vehicle_record SET entry_time = DATE_SUB(NOW(), INTERVAL 65 SECOND) WHERE id = ${en.data.id}`);
        const ex1 = await req('POST', '/vehicle/exit/' + en.data.id, { token: A.token });
        check('T85 停1分钟按1小时计费(不再按0.02小时)', ex1.ok && ex1.data.durationMinutes === 1 && Number(ex1.data.totalFee) === rate,
          ex1.ok ? `fee=${ex1.data.totalFee} minutes=${ex1.data.durationMinutes} rate=${rate}` : ex1.msg);

        const s2 = await findVacantSpot();
        const en2 = await req('POST', '/vehicle/enter', { token: A.token, body: { spotId: String(s2.id), plateNumber: '粤BAA001' } });
        if (!en2.ok) throw new Error('enter2: ' + en2.msg);
        sql(`UPDATE t_vehicle_record SET entry_time = DATE_SUB(NOW(), INTERVAL 90 MINUTE) WHERE id = ${en2.data.id}`);
        const ex2 = await req('POST', '/vehicle/exit/' + en2.data.id, { token: A.token });
        check('T86 停90分钟按2小时计费', ex2.ok && ex2.data.durationMinutes === 90 && Number(ex2.data.totalFee) === rate * 2,
          ex2.ok ? `fee=${ex2.data.totalFee} minutes=${ex2.data.durationMinutes}` : ex2.msg);
      } catch (e) { check('T85-T86 计费边界', false, e.message); }
    }

    // ================= 8. 无车牌入场回滚 =================
    section('8. 未设置车牌入场');
    {
      const B = await makeUser('B', { noPlate: true }); // 注册时不设置车牌
      try {
        const s = await findVacantSpot();
        const en = await req('POST', '/vehicle/enter', { token: B.token, body: { spotId: String(s.id) } });
        check('T39 无车牌入场被拒且事务回滚', !en.ok && /plate/i.test(en.msg || ''), en.msg);
        const st = await spotStatus(s.id);
        check('T40 失败后车位未被占用(回滚)', st === 0, `status=${st}`);

        await req('PUT', '/user/info', { token: B.token, body: { plateNumber: '粤BBB002' } });
        const en2 = await req('POST', '/vehicle/enter', { token: B.token, body: { spotId: String(s.id) } });
        check('T41 设置车牌后入场成功', en2.ok, en2.ok ? '' : en2.msg);
        if (en2.ok) {
          await sleep(1200);
          const ex = await req('POST', '/vehicle/exit/' + en2.data.id, { token: B.token });
          check('T42 B 出场处理(扣费成功或余额不足被拒)', ex.ok || /Insufficient/i.test(ex.msg || ''), ex.msg || 'ok');
        }
      } catch (e) { check('T39-T42 无车牌场景', false, e.message); }
    }

    // ================= 9. 余额不足出场 =================
    section('9. 余额不足结算');
    {
      const C = await makeUser('C');
      try {
        await req('PUT', '/user/info', { token: C.token, body: { plateNumber: '粤CCC003' } });
        const rc = await req('POST', '/user/recharge', { token: C.token, body: { amount: 3 } });
        check('T43 C充值3元', rc.ok, rc.msg);
        const s = await findVacantSpot();
        const rate = Number(s.pricePerHour);
        const en = await req('POST', '/vehicle/enter', { token: C.token, body: { spotId: String(s.id), plateNumber: '粤CCC003' } });
        check('T44 C入场成功', en.ok, en.ok ? '' : en.msg);
        if (en.ok) {
          await sleep(1500);
          const balB = Number((await req('GET', '/user/info', { token: C.token })).data.balance);
          const ex = await req('POST', '/vehicle/exit/' + en.data.id, { token: C.token });
          const balA = Number((await req('GET', '/user/info', { token: C.token })).data.balance);
          check('T45 余额不足出场被拒且不扣费', !ex.ok && /Insufficient/i.test(ex.msg || '') && balA === balB, ex.msg + ` balance=${balA}`);
          const cur = await req('GET', '/vehicle/current', { token: C.token });
          check('T46 失败后车辆仍在场', cur.ok && cur.data && String(cur.data.id) === String(en.data.id), cur.ok ? '' : (cur.msg || 'no current'));
          const st = await spotStatus(s.id);
          check('T47 失败后车位仍被占用', st === 1, `status=${st}`);

          await req('POST', '/user/recharge', { token: C.token, body: { amount: 10 } });
          const ex2 = await req('POST', '/vehicle/exit/' + en.data.id, { token: C.token });
          check('T48 充值后出场成功并扣费', ex2.ok && Number(ex2.data.totalFee) === rate && ex2.data.paymentStatus === 1,
            ex2.ok ? `fee=${ex2.data.totalFee}` : ex2.msg);
          const st2 = await spotStatus(s.id);
          check('T49 充值出场后车位释放', st2 === 0, `status=${st2}`);
        }
      } catch (e) { check('T43-T49 余额不足场景', false, e.message); }
    }

    // ================= 10. 越权占用他人已预约车位 =================
    section('10. 未预约用户进入"已预约"车位(越权探测)');
    {
      const D = await makeUser('D');
      const E = await makeUser('E');
      try {
        const s = await findVacantSpot();
        const cr = await req('POST', '/reservation/create', { token: D.token, body: { spotId: String(s.id), startTime: nowPlus(60), endTime: nowPlus(180) } });
        check('T50 D预约车位成功', cr.ok, cr.msg);
        const stResv = await spotStatus(s.id);
        check('T51 车位状态变为预约(2)', stResv === 2, `status=${stResv}`);

        await req('PUT', '/user/info', { token: E.token, body: { plateNumber: '粤EEE005' } });
        const en = await req('POST', '/vehicle/enter', { token: E.token, body: { spotId: String(s.id), plateNumber: '粤EEE005' } });
        if (en.ok) {
          check('T52 未预约者进入他人已预约车位应被拒', false, `越权成功! E占用了D预约的车位 record=${en.data.id}`);
        } else {
          check('T52 未预约者进入已预约车位被拒', /occupied|not available|reserv/i.test(en.msg || ''), en.msg);
        }
      } catch (e) { check('T50-T53 越权占用探测', false, e.message); }
    }

    // ================= 11. 并发竞态 =================
    section('11. 并发竞态');
    {
      const F = await makeUser('F');
      const G = await makeUser('G');
      const H = await makeUser('H');
      try {
        // 11.1 两用户并发预约同一空闲车位
        const s1 = await findVacantSpot();
        const [rF, rG] = await Promise.all([
          req('POST', '/reservation/create', { token: F.token, body: { spotId: String(s1.id), startTime: nowPlus(60), endTime: nowPlus(180) } }),
          req('POST', '/reservation/create', { token: G.token, body: { spotId: String(s1.id), startTime: nowPlus(60), endTime: nowPlus(180) } }),
        ]);
        const okCount = [rF, rG].filter((r) => r.ok).length;
        check('T54 并发双预约仅一个成功(CAS)', okCount === 1, `ok=${okCount}  F=${rF.msg || 'ok'} G=${rG.msg || 'ok'}`);
        const win = (rF.ok && rF) || (rG.ok && rG) || null;
        if (win) await req('POST', '/reservation/cancel/' + win.data.id, { token: win === rF ? F.token : G.token });

        // 11.2 两用户并发进入同一空闲车位
        const s2 = await findVacantSpot();
        await req('PUT', '/user/info', { token: F.token, body: { plateNumber: '粤FFF006' } });
        await req('PUT', '/user/info', { token: G.token, body: { plateNumber: '粤GGG007' } });
        const [eF, eG] = await Promise.all([
          req('POST', '/vehicle/enter', { token: F.token, body: { spotId: String(s2.id), plateNumber: '粤FFF006' } }),
          req('POST', '/vehicle/enter', { token: G.token, body: { spotId: String(s2.id), plateNumber: '粤GGG007' } }),
        ]);
        const enOk = [eF, eG].filter((r) => r.ok).length;
        check('T55 并发同车位双入场仅一个成功(CAS)', enOk === 1, `ok=${enOk}  F=${eF.msg || 'ok'} G=${eG.msg || 'ok'}`);

        // 11.3 同一用户并发入场到两个不同车位(TOCTOU 探测)
        const s3 = await findVacantSpot();
        const s4 = await findVacantSpot();
        const [eH1, eH2] = await Promise.all([
          req('POST', '/vehicle/enter', { token: H.token, body: { spotId: String(s3.id), plateNumber: '粤HHH008' } }),
          req('POST', '/vehicle/enter', { token: H.token, body: { spotId: String(s4.id), plateNumber: '粤HHH008' } }),
        ]);
        const hOk = [eH1, eH2].filter((r) => r.ok).length;
        if (hOk === 2) {
          check('T56 同一用户并发双车位入场仅一个成功', false, `两个车位同时入场成功 s3=${s3.spotCode} s4=${s4.spotCode} -> 可同时停两辆车`);
        } else {
          check('T56 同一用户并发双车位入场仅一个成功', hOk === 1, `ok=${hOk} ${eH1.msg || 'ok'} / ${eH2.msg || 'ok'}`);
        }
        for (const r of [eH1, eH2]) if (r.ok) await req('POST', '/vehicle/exit/' + r.data.id, { token: H.token });
        for (const r of [eF, eG]) if (r.ok) await req('POST', '/vehicle/exit/' + r.data.id, { token: r === eF ? F.token : G.token });

        // 11.4 同一用户并发双出场(有足够余额)
        const s5 = await findVacantSpot();
        await req('POST', '/user/recharge', { token: F.token, body: { amount: 20 } });
        const e5 = await req('POST', '/vehicle/enter', { token: F.token, body: { spotId: String(s5.id), plateNumber: '粤FFF006' } });
        if (e5.ok) {
          await sleep(1200);
          const [x1, x2] = await Promise.all([
            req('POST', '/vehicle/exit/' + e5.data.id, { token: F.token }),
            req('POST', '/vehicle/exit/' + e5.data.id, { token: F.token }),
          ]);
          const exOk = [x1, x2].filter((r) => r.ok).length;
          check('T57 并发双出场仅一个成功(CAS)', exOk === 1, `ok=${exOk}  ${x1.msg || 'ok'} / ${x2.msg || 'ok'}`);
        }
      } catch (e) { check('T54-T57 并发竞态', false, e.message); }
    }

    // ================= 12. 管理员后台 =================
    section('12. 管理员后台接口');
    if (adminToken) {
      const noToken = await req('GET', '/admin/dashboard');
      check('T58 无token访问管理接口被拒', !noToken.ok && noToken.code === 401, `code=${noToken.code} msg=${noToken.msg}`);
      const badToken = await req('GET', '/admin/dashboard', { token: 'garbage.token.here' });
      check('T59 非法token被拒', !badToken.ok, `code=${badToken.code}`);
      if (A) {
        const uTok = await req('GET', '/admin/dashboard', { token: A.token });
        check('T60 用户token访问管理接口被拒(403)', !uTok.ok && uTok.code === 403, `code=${uTok.code} msg=${uTok.msg}`);
      }

      const dash = await req('GET', '/admin/dashboard', { token: adminToken });
      check('T61 工作台统计接口', dash.ok && dash.data.totalUsers > 0 && dash.data.totalSpots === 50 && dash.data.availableSpots > 0,
        dash.ok ? `users=${dash.data.totalUsers} spots=${dash.data.totalSpots} avail=${dash.data.availableSpots} rev=${dash.data.todayRevenue}` : dash.msg);

      if (A) {
        try {
          const ul = await req('GET', '/admin/users?page=1&pageSize=10&keyword=' + encodeURIComponent(phoneA), { token: adminToken });
          check('T62 用户搜索(keyword=手机号)', ul.ok && ul.data.records.length === 1 && ul.data.records[0].phone === phoneA, ul.ok ? `found=${ul.data.records.length}` : ul.msg);
          const us = await req('GET', '/admin/users?page=1&pageSize=10&status=0', { token: adminToken });
          check('T63 按禁用状态筛选', us.ok && us.data.records.every((u) => u.status === 0), us.ok ? `total=${us.data.total}` : us.msg);

          const dis = await req('PUT', '/admin/user/' + A.userId + '/status?status=0', { token: adminToken });
          check('T64 管理员禁用用户A', dis.ok, dis.msg);
          const infoDis = await req('GET', '/user/info', { token: A.token });
          check('T65 禁用后用户token失效(401)', !infoDis.ok && infoDis.code === 401, `code=${infoDis.code} msg=${infoDis.msg}`);
          const rel = await req('PUT', '/admin/user/' + A.userId + '/status?status=1', { token: adminToken });
          check('T66 重新启用用户A', rel.ok, rel.msg);
          const infoRel = await req('GET', '/user/info', { token: A.token });
          check('T67 启用后用户恢复正常', infoRel.ok, infoRel.ok ? '' : infoRel.msg);
        } catch (e) { check('T62-T67 用户管理', false, e.message); }
      }

      // 预约管理
      if (A) {
        try {
          const s = await findVacantSpot();
          const cr = await req('POST', '/reservation/create', { token: A.token, body: { spotId: String(s.id), startTime: nowPlus(60), endTime: nowPlus(180) } });
          if (cr.ok) {
            const rl = await req('GET', '/admin/reservations?page=1&pageSize=10&status=1', { token: adminToken });
            check('T68 管理员预约列表(状态=1)', rl.ok && rl.data.records.some((r) => String(r.id) === String(cr.data.id)), rl.ok ? `total=${rl.data.total}` : rl.msg);
            const ac = await req('PUT', '/admin/reservation/' + cr.data.id + '/status?status=3', { token: adminToken });
            check('T69 管理员取消预约成功', ac.ok, ac.msg);
            const st = await spotStatus(s.id);
            check('T70 管理员取消后车位释放', st === 0, `status=${st}`);
          } else { check('T68-T70 预约管理前置', false, cr.msg); }
        } catch (e) { check('T68-T70 预约管理', false, e.message); }
      }

      // 车辆记录 + 强制离场
      {
        const J = await makeUser('J');
        try {
          await req('PUT', '/user/info', { token: J.token, body: { plateNumber: '粤JJJ009' } });
          const s = await findVacantSpot();
          const en = await req('POST', '/vehicle/enter', { token: J.token, body: { spotId: String(s.id), plateNumber: '粤JJJ009' } });
          if (en.ok) {
            const vr = await req('GET', '/admin/vehicle-records?page=1&pageSize=10&status=0', { token: adminToken });
            check('T71 管理员在场车辆列表', vr.ok && vr.data.records.some((r) => String(r.id) === String(en.data.id)), vr.ok ? `total=${vr.data.total}` : vr.msg);
            const fe = await req('POST', '/admin/vehicle-record/' + en.data.id + '/force-exit', { token: adminToken });
            check('T72 管理员强制离场(无余额)', fe.ok && fe.data.status === 1 && fe.data.paymentStatus === 0, fe.ok ? `fee=${fe.data.totalFee} pay=${fe.data.paymentStatus}` : fe.msg);
            const st = await spotStatus(s.id);
            check('T73 强制离场后车位释放', st === 0, `status=${st}`);
            const fe2 = await req('POST', '/admin/vehicle-record/' + en.data.id + '/force-exit', { token: adminToken });
            check('T74 重复强制离场被拒', !fe2.ok, fe2.msg);
          } else { check('T71-T74 强制离场前置', false, en.msg); }
        } catch (e) { check('T71-T74 强制离场', false, e.message); }
      }

      // 车位 CRUD + 地图(按 spotCode 断言, 避免 spot-map 数字ID精度问题)
      try {
        const code = 'E2E-' + Date.now().toString().slice(-6);
        const addSpot = await req('POST', '/admin/spot', { token: adminToken, body: { spotCode: code, area: 'E区', floor: 'B1', pricePerHour: 6.5, remark: 'e2e temp' } });
        check('T75 管理员新增车位', addSpot.ok, addSpot.ok ? `code=${code}` : addSpot.msg);
        if (addSpot.ok) {
          const sid = addSpot.data.id;
          const upd = await req('PUT', '/admin/spot/' + sid, { token: adminToken, body: { pricePerHour: 8 } });
          check('T76 修改车位单价', upd.ok && Number(upd.data.pricePerHour) === 8, upd.ok ? 'price=' + upd.data.pricePerHour : upd.msg);
          const mt = await req('PUT', '/admin/spot/' + sid + '/status?status=3', { token: adminToken });
          check('T77 设置维护状态', mt.ok, mt.msg);
          const sm = await req('GET', '/admin/spot-map', { token: adminToken });
          const mine = sm.ok ? sm.data.spots.find((x) => String(x.spotCode) === code) : null;
          check('T78 维护车位在地图标记且计数正确', sm.ok && mine && mine.displayStatus === 4 && sm.data.maintenanceCount >= 1,
            sm.ok ? `display=${mine && mine.displayStatus} maintCount=${sm.data.maintenanceCount}` : (sm.msg || 'map failed'));
          const rt = await req('PUT', '/admin/spot/' + sid + '/status?status=0', { token: adminToken });
          check('T79 恢复车位为空闲', rt.ok, rt.msg);
          const sl = await req('GET', '/admin/spots?page=1&pageSize=60', { token: adminToken });
          check('T80 管理员车位列表包含新增车位', sl.ok && sl.data.records.some((x) => String(x.spotCode) === code), sl.ok ? `total=${sl.data.total}` : sl.msg);
          sql('DELETE FROM t_parking_spot WHERE id = ' + sid);
        }
      } catch (e) { check('T75-T80 车位管理', false, e.message); }

      // 配置读写
      try {
        const cfg = await req('GET', '/admin/config', { token: adminToken });
        check('T81 读取系统配置', cfg.ok && cfg.data && Object.keys(cfg.data).length >= 4, cfg.ok ? JSON.stringify(cfg.data) : cfg.msg);
        if (cfg.ok) {
          const original = cfg.data.grace_period_minutes;
          const uc = await req('PUT', '/admin/config', { token: adminToken, body: { grace_period_minutes: '99' } });
          const cfg2 = await req('GET', '/admin/config', { token: adminToken });
          check('T82 修改配置生效', uc.ok && cfg2.ok && cfg2.data.grace_period_minutes === '99', cfg2.ok ? 'grace=' + cfg2.data.grace_period_minutes : cfg2.msg);
          await req('PUT', '/admin/config', { token: adminToken, body: { grace_period_minutes: original } });
        }
      } catch (e) { check('T81-T82 配置读写', false, e.message); }
    }

    // ================= 13. 预约过期调度 =================
    section('13. 预约过期自动释放(调度器)');
    {
      const K = await makeUser('K');
      try {
        const s = await findVacantSpot();
        const cr = await req('POST', '/reservation/create', { token: K.token, body: { spotId: String(s.id), startTime: nowPlus(1), endTime: nowPlus(120) } });
        if (!cr.ok) throw new Error('create: ' + cr.msg);
        const rid = cr.data.id;
        // 把预约时间段移入过去且超过15分钟宽限期(调度器每60s扫描一次, 超过宽限才过期)
        sql(`UPDATE t_reservation SET start_time = NOW() - INTERVAL 30 MINUTE, end_time = NOW() - INTERVAL 20 MINUTE WHERE id = ${rid}`);
        let expired = false, status = null;
        for (let i = 0; i < 18; i++) {
          await sleep(5000);
          status = sql(`SELECT status FROM t_reservation WHERE id = ${rid}`).split('\n')[0];
          if (status === '4') { expired = true; break; }
        }
        check('T83 过期预约被调度器标记过期(4)', expired === true, `status=${status}`);
        const st = await spotStatus(s.id);
        check('T84 过期后车位自动释放为空闲', st === 0, `status=${st}`);
      } catch (e) { check('T83-T84 过期调度', false, e.message); }
    }
  } catch (e) {
    console.error('用例执行异常: ' + e.message);
    check('脚本自身健壮性(未中断)', false, e.message);
  } finally {
    section('汇总');
    console.log(`通过 ${passCount} / 失败 ${failCount} / 共 ${passCount + failCount}`);
    if (!KEEP) await runCleanup(adminToken);
    const fs = require('fs');
    fs.writeFileSync(path.join(__dirname, 'e2e-results.json'), JSON.stringify({ passCount, failCount, results }, null, 2));
    console.log('结果已写入 e2e-results.json');
  }
  process.exit(failCount > 0 ? 1 : 0);
}

main().catch((e) => { console.error('FATAL:', e); process.exit(2); });
