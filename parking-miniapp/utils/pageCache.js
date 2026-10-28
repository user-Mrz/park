/**
 * 页面级内存短缓存（tab 切换加速）。
 *
 * 小程序 tab 页每次切回都会触发 onShow，如果每次都重新请求网络并显示 loading，
 * 用户会明显感觉「切换慢」。本缓存让页面在 onShow 时先用最近一次数据立即渲染，
 * 再在后台静默刷新，切换体验变为瞬开。
 *
 * 注意：换账号登录、退出登录时必须调用 cacheClear()，
 * 避免把上一个账号的个人数据（在场车辆/预约/余额等）渲染出来。
 */
const store = {}

/**
 * 读缓存：命中且未超过 maxAgeMs（默认 30 秒）时返回缓存值，否则返回 null
 */
export function cacheGet(key, maxAgeMs) {
  const entry = store[key]
  const ttl = maxAgeMs == null ? 30000 : maxAgeMs
  if (entry && Date.now() - entry.ts <= ttl) {
    return entry.value
  }
  return null
}

/** 写缓存 */
export function cacheSet(key, value) {
  store[key] = { value, ts: Date.now() }
  return value
}

/** 清空全部缓存（登出/登入/401 时调用） */
export function cacheClear() {
  Object.keys(store).forEach(function(k) { delete store[k] })
}