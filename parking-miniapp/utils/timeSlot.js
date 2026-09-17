/**
 * 预约时段计算工具
 *
 * 约束：不允许查看/选择/预约当前时间之前的时段。
 * 规则：时段按 SLOT_MINUTES 粒度生成；选择「今天」时，早于「当前时间 + LEAD_MINUTES」的时段
 * 不会出现在候选列表里（因此用户既看不到也选不到）。
 */

export const LEAD_MINUTES = 10
export const SLOT_MINUTES = 30

export const pad = (n) => String(n).padStart(2, '0')

/** 格式化为 YYYY-MM-DD */
export function fmtDate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 生成未来 days 天的日期列表（从今天开始） */
export function buildDateList(days, now = new Date()) {
  const list = []
  for (let i = 0; i < days; i++) {
    const d = new Date(now)
    d.setDate(d.getDate() + i)
    list.push(fmtDate(d))
  }
  return list
}

/**
 * 生成某日期可选的开始时间列表
 * @param {string} dateStr YYYY-MM-DD
 * @param {Date} now 当前时间
 * @returns {string[]} ["09:30", "10:00", ...]
 */
export function buildTimeSlots(dateStr, now = new Date()) {
  const isToday = dateStr === fmtDate(now)
  const earliest = now.getTime() + LEAD_MINUTES * 60000
  const slots = []
  for (let m = 0; m < 24 * 60; m += SLOT_MINUTES) {
    const hour = Math.floor(m / 60)
    const minute = m % 60
    if (isToday) {
      const slot = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute, 0).getTime()
      if (slot < earliest) continue
    }
    slots.push(`${pad(hour)}:${pad(minute)}`)
  }
  return slots
}

/** 开始时间转时间戳（用构造函数，避免 iOS 对 "YYYY-MM-DD HH:mm" 解析不一致） */
export function startTimestamp(dateStr, timeStr) {
  if (!dateStr || !timeStr) return 0
  const [y, m, d] = dateStr.split('-').map(Number)
  const [hh, mm] = timeStr.split(':').map(Number)
  return new Date(y, m - 1, d, hh, mm, 0).getTime()
}
