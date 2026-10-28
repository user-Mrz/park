/**
 * 校园停车分区工具：预约 / 入场两个页面共用
 * 后端分区定义见 CampusRuleServiceImpl（teaching/library/dorm/gym/visitor）
 */

/** 分区展示顺序，未在列表中的分区排到最后 */
const ZONE_ORDER = ['teaching', 'library', 'dorm', 'gym', 'visitor']

/** 车位没有 zoneCode 时的兜底分组 */
export const OTHER_ZONE = 'other'

/**
 * 按分区聚合车位，生成"区域选择器"用的列表
 * @param {Array} spots 车位列表（已按身份过滤）
 * @param {Object} zoneNames 分区名称映射 { teaching: '教学楼区', ... }，可省略
 * @returns {Array} [{ code, name, count, priceText, label }]
 */
export function buildZoneList(spots, zoneNames) {
  const map = {}
  ;(spots || []).forEach((s) => {
    const code = s.zoneCode || OTHER_ZONE
    if (!map[code]) {
      map[code] = {
        code,
        name: (zoneNames && zoneNames[code]) || s.zoneName || s.area || '其他区域',
        spots: []
      }
    }
    map[code].spots.push(s)
  })

  return Object.keys(map).map((code) => {
    const z = map[code]
    const prices = z.spots.map((s) => Number(s.pricePerHour) || 0)
    const min = Math.min.apply(null, prices)
    const max = Math.max.apply(null, prices)
    const priceText = min === max ? `¥${min}/h` : `¥${min}~${max}/h`
    return {
      code: z.code,
      name: z.name,
      count: z.spots.length,
      priceText,
      label: `${z.name}（空闲 ${z.spots.length} 个 · ${priceText}）`
    }
  }).sort((a, b) => {
    const ia = ZONE_ORDER.indexOf(a.code)
    const ib = ZONE_ORDER.indexOf(b.code)
    return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib)
  })
}

/** 取某个分区下的车位 */
export function spotsOfZone(spots, zoneCode) {
  return (spots || []).filter((s) => (s.zoneCode || OTHER_ZONE) === zoneCode)
}

/** 取分区名称映射：来自 /user/benefits 的 zones 字段 */
export function zoneNameMap(benefits) {
  const map = {}
  const zones = (benefits && benefits.zones) || {}
  Object.keys(zones).forEach((code) => {
    map[code] = zones[code] && zones[code].name
  })
  return map
}
