// 生成压测用户: 输出 SQL 脚本(供 mysql 导入) + Node 本地签发 JWT (HS256, 与后端同 secret) + users.json
// 用法: node gen-users.js <数量>  (默认 30)  -> 然后: mysql ... < users.sql
'use strict';
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const N = parseInt(process.argv[2] || '30', 10);
const SECRET = 'parking-system-secret-key-2024-parking-lot-management-jwt-token';

function signJwt(userId, phone) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: String(userId),
    phone,
    role: 'user',
    iat: now,
    exp: now + 86400, // 24h
  };
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const data = `${b64(header)}.${b64(payload)}`;
  const sig = crypto.createHmac('sha256', Buffer.from(SECRET, 'utf8')).update(data).digest('base64url');
  return `${data}.${sig}`;
}

async function main() {
  const sql = [];
  // 删除旧压测用户及其关联数据
  sql.push(`DELETE FROM t_vehicle_record WHERE user_id IN (SELECT id FROM t_user WHERE openid LIKE 'wx_stress-%');`);
  sql.push(`DELETE FROM t_payment_record WHERE user_id IN (SELECT id FROM t_user WHERE openid LIKE 'wx_stress-%');`);
  sql.push(`DELETE FROM t_recharge_record WHERE user_id IN (SELECT id FROM t_user WHERE openid LIKE 'wx_stress-%');`);
  sql.push(`DELETE FROM t_reservation WHERE user_id IN (SELECT id FROM t_user WHERE openid LIKE 'wx_stress-%');`);
  sql.push(`DELETE FROM t_user WHERE openid LIKE 'wx_stress-%';`);

  const users = [];
  const values = [];
  for (let i = 0; i < N; i++) {
    const id = BigInt(2090000000000000000n + BigInt(i * 3)); // 雪花风格 ID
    const phone = `139${String(10000000 + i * 977).padStart(8, '0')}`;
    const openid = `wx_stress-${i}`;
    const plate = `粤B${String(88000 + i).padStart(5, '0')}`;
    const nickname = `压测用户${i}`;
    values.push(`(${id}, '${openid}', '${nickname}', '${phone}', '', '${plate}', 200.00, 1, NOW(), NOW())`);
    const token = signJwt(id, phone);
    users.push({ i, userId: id.toString(), token, plate, deviceId: `stress-${i}`, phone, nickname });
  }
  sql.push(`INSERT INTO t_user (id, openid, nickname, phone, password, plate_number, balance, status, created_at, updated_at) VALUES ${values.join(',\n')};`);

  fs.writeFileSync(path.join(__dirname, 'users.sql'), sql.join('\n'));
  fs.writeFileSync(path.join(__dirname, 'users.json'), JSON.stringify(users, null, 2));
  console.log(`完成: ${users.length} 个压测用户, 已生成 users.sql (待导入) 与 users.json`);
  console.log('示例:', JSON.stringify(users[0]).slice(0, 200));
}

main().catch(e => { console.error(e); process.exit(1); });
