/**
 * 时长相关的公共工具（管理端）
 * 与小程序 utils/duration.js 口径保持一致：服务端时间形如 "2026-09-21 10:30:00"
 */

/** 兼容空格与 T 分隔符的日期解析；解析失败返回 null */
export function parseDateTime(v?: string | null): Date | null {
  if (!v) return null
  const s = String(v).replace('T', ' ')
  const parts = s.split(' ')[0].split('-')
  const times = (s.split(' ')[1] || '00:00:00').split(':')
  const d = new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0)
  return isNaN(d.getTime()) ? null : d
}

/** 已停分钟数（向下取整，最小 0） */
export function elapsedMinutes(entryTime?: string | null, now: number = Date.now()): number {
  const start = parseDateTime(entryTime)
  if (!start) return 0
  const diff = Math.floor((now - start.getTime()) / 1000 / 60)
  return diff > 0 ? diff : 0
}

/** 分钟数文案：不足 1 分钟 / N 分钟 / N 小时 M 分钟 / N 天 M 小时 */
export function formatMinutes(minutes?: number | null): string {
  const m = Math.floor(Number(minutes || 0))
  if (!m || m <= 0) return '不足 1 分钟'
  const d = Math.floor(m / 1440)
  const h = Math.floor((m % 1440) / 60)
  const mm = m % 60
  const parts: string[] = []
  if (d) parts.push(`${d} 天`)
  if (h) parts.push(`${h} 小时`)
  if (mm) parts.push(`${mm} 分钟`)
  return parts.join(' ') || '不足 1 分钟'
}

/** 由入场时间实时计算已停时长文案；时间无效时返回 '-' */
export function formatDuration(entryTime?: string | null, now: number = Date.now()): string {
  if (!parseDateTime(entryTime)) return '-'
  return formatMinutes(elapsedMinutes(entryTime, now))
}
