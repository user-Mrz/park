/**
 * 停车时长相关的公共工具（首页在场车辆、离场页共用）
 * 说明：服务端返回的时间形如 "2026-09-21 10:30:00"，
 * 部分机型（iOS）用 new Date(str) 解析会失败，这里统一手动解析。
 */

/** 兼容空格与 T 分隔符的日期解析；解析失败返回 null */
export function parseDateTime(str) {
  if (!str) return null
  const s = String(str).replace('T', ' ')
  const parts = s.split(' ')[0].split('-')
  const times = (s.split(' ')[1] || '00:00:00').split(':')
  const d = new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0)
  return isNaN(d.getTime()) ? null : d
}

/**
 * 已停分钟数（向下取整，最小 0）
 * @param {string} entryTime 入场时间
 * @param {number} now 当前时间戳，默认取本机当前时间
 */
export function elapsedMinutes(entryTime, now) {
  const start = parseDateTime(entryTime)
  if (!start) return 0
  const diff = Math.floor(((now || Date.now()) - start.getTime()) / 1000 / 60)
  return diff > 0 ? diff : 0
}

/**
 * 已停时长文案：不足1分钟 / N分钟 / X小时Y分钟
 * 时间无法解析时返回 '--'，避免显示出错误的时长
 */
export function formatDuration(entryTime, now) {
  if (!entryTime) return '--'
  const start = parseDateTime(entryTime)
  if (!start) return '--'
  const diff = elapsedMinutes(entryTime, now)
  if (diff < 1) return '不足1分钟'
  if (diff < 60) return diff + '分钟'
  return Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟'
}

/**
 * 创建秒级刷新器：用于"已停时长"这类需要自己走字的时间显示。
 * 回调内部请自行比对文案是否变化，只有变化时才 setData。
 * @param {Function} fn 每次触发执行的函数
 * @param {number} intervalMs 检查间隔，默认 1000ms（整分钟时文案变化一次）
 */
export function createTicker(fn, intervalMs) {
  return setInterval(fn, intervalMs || 1000)
}

/** 停止刷新器（页面 onHide/onUnload 必须调用，避免后台空转） */
export function clearTicker(timer) {
  if (timer) clearInterval(timer)
}
