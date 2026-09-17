// 准备压测数据：批量创建测试用户（wechat-login）、设置车牌、充值
// 用法: node prepare.js <数量>  (默认 30)
'use strict';
const { BASE, http } = require('./lib');

const N = parseInt(process.argv[2] || '30', 10);

async function main() {
  console.log(`准备 ${N} 个压测用户...`);
  const users = [];
  for (let i = 0; i < N; i++) {
    const deviceId = `stress-dev-${i}`;
    const r = await http('POST', '/auth/wechat-login', {
      body: { code: 'stress-' + i, deviceId, nickname: `压测用户${i}` },
    });
    if (!r.ok) throw new Error(`wechat-login failed: ${JSON.stringify(r)}`);
    const token = r.data.token;
    const userId = r.data.userId;

    // 设置车牌
    const plate = `粤B${String(88000 + i).padStart(5, '0')}`;
    const up = await http('PUT', '/user/info', { token, body: { nickname: `压测用户${i}`, plateNumber: plate } });
    if (!up.ok) throw new Error(`update info failed: ${JSON.stringify(up)}`);

    // 充值 200 元
    const rc = await http('POST', '/user/recharge', { token, body: { amount: 200, payMethod: 'wechat' } });
    if (!rc.ok) throw new Error(`recharge failed: ${JSON.stringify(rc)}`);

    users.push({ i, userId, token, plate, deviceId });
    if ((i + 1) % 10 === 0) console.log(`  已准备 ${i + 1}/${N}`);
  }
  // 保存到文件供场景脚本使用
  const fs = require('fs');
  const path = require('path');
  fs.writeFileSync(path.join(__dirname, 'users.json'), JSON.stringify(users, null, 2));
  console.log(`完成: ${users.length} 个用户已保存到 stress-test/users.json`);
  console.log('示例:', JSON.stringify(users[0]));
}

main().catch(e => { console.error(e); process.exit(1); });
