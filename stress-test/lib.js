// 压测公共库：并发执行器 + 统计
'use strict';

const BASE = 'http://localhost:8080/api';

// 并发执行器：同时启动 concurrency 个 worker，每个 worker 循环执行 task，直到达到 total 次
async function runConcurrent(total, concurrency, task, { label = '', timeoutMs = 30000 } = {}) {
  const results = [];
  let idx = 0;
  let active = 0;
  let started = 0;
  const t0 = Date.now();
  const errors = [];

  async function worker() {
    while (true) {
      const i = idx++;
      if (i >= total) return;
      const t1 = Date.now();
      let ok = false;
      let status = 0;
      let code = null;
      let msg = '';
      let err = null;
      try {
        const r = await task(i, { timeoutMs });
        ok = r.ok;
        status = r.status;
        code = r.code;
        msg = r.msg;
      } catch (e) {
        err = e;
      }
      const elapsed = Date.now() - t1;
      results.push({ ok, status, code, msg, elapsed, err });
      started++;
      active--;
    }
  }

  // 启动 concurrency 个 worker
  const workers = [];
  for (let w = 0; w < concurrency; w++) {
    active++;
    workers.push(worker());
  }
  await Promise.all(workers);

  const totalMs = Date.now() - t0;
  const latencies = results.map(r => r.elapsed).sort((a, b) => a - b);
  const okCount = results.filter(r => r.ok).length;
  const failCount = results.length - okCount;
  const p = (q) => latencies.length ? latencies[Math.min(latencies.length - 1, Math.floor(q * latencies.length))] : 0;

  // 错误信息 top5
  const msgCount = {};
  for (const r of results) {
    if (!r.ok) {
      const key = `${r.status} ${r.msg || (r.err ? r.err.message : '')}`.slice(0, 80);
      msgCount[key] = (msgCount[key] || 0) + 1;
    }
  }
  const topErrors = Object.entries(msgCount).sort((a, b) => b[1] - a[1]).slice(0, 5);

  return {
    label,
    total: results.length,
    ok: okCount,
    fail: failCount,
    qps: totalMs > 0 ? Math.round((results.length / totalMs) * 1000) : 0,
    totalMs,
    latency: {
      avg: latencies.length ? Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length) : 0,
      p50: p(0.5),
      p90: p(0.9),
      p95: p(0.95),
      p99: p(0.99),
      max: latencies.length ? latencies[latencies.length - 1] : 0,
    },
    topErrors,
  };
}

function fmt(r) {
  const lines = [
    `[${r.label}]`,
    `  总数=${r.total} 成功=${r.ok} 失败=${r.fail}  QPS=${r.qps}  总耗时=${r.totalMs}ms`,
    `  延迟(ms): avg=${r.latency.avg} p50=${r.latency.p50} p90=${r.latency.p90} p95=${r.latency.p95} p99=${r.latency.p99} max=${r.latency.max}`,
  ];
  if (r.topErrors.length) {
    for (const [k, v] of r.topErrors) lines.push(`  错误: ${v}x  ${k}`);
  }
  return lines.join('\n');
}

// 简易 HTTP 封装
async function http(method, path, { token, body, timeoutMs = 30000 } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(BASE + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: ctrl.signal,
    });
    let data = null;
    try { data = await res.json(); } catch { /* non-json */ }
    const ok = res.ok && data && data.code === 200;
    // data 解包: 返回 R.data 本体; raw 保留完整响应
    return { ok, status: res.status, code: data ? data.code : null, msg: data ? (data.message || '') : '', data: (ok && data) ? data.data : null, raw: data };
  } finally {
    clearTimeout(timer);
  }
}

module.exports = { BASE, runConcurrent, fmt, http };
