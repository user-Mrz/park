import request from '../../utils/request.js'
import { buildTimeSlots, buildDateList, fmtDate, startTimestamp as toTimestamp, pad } from '../../utils/timeSlot.js'
import { buildZoneList, spotsOfZone, zoneNameMap } from '../../utils/zones.js'

/** 完整的预约时长候选；潮汐车位会按开放时间段再过滤一次 */
const ALL_DURATIONS = [
  { label: '1小时', value: 1 }, { label: '2小时', value: 2 }, { label: '3小时', value: 3 },
  { label: '4小时', value: 4 }, { label: '6小时', value: 6 }, { label: '12小时', value: 12 },
  { label: '1天', value: 24 }, { label: '3天', value: 72 }, { label: '1周', value: 168 },
  { label: '1个月', value: 720 }
]

/** 潮汐车位按批次单独成区，zoneCode 形如 tidal:<batchId> */
const TIDAL_ZONE_PREFIX = 'tidal:'

/** "yyyy-MM-dd HH:mm:ss" 或 ISO 串 -> 时间戳（iOS 对 "-" 解析不一致，统一交给 timeSlot 处理） */
const strToTs = (s) => {
  if (!s) return 0
  const t = String(s).replace('T', ' ')
  return toTimestamp(t.slice(0, 10), t.slice(11, 16))
}

/** 时间戳 -> "yyyy-MM-dd HH:mm" */
const tsToText = (ts) => {
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 生成 [fromTs, toTs] 之间的日期列表（不早于今天），用于潮汐车位的开放时间段 */
const buildRangeDateList = (fromTs, toTs) => {
  const list = []
  const start = new Date(Math.max(fromTs, Date.now()))
  start.setHours(0, 0, 0, 0)
  const end = new Date(toTs)
  end.setHours(0, 0, 0, 0)
  const cur = new Date(start)
  let guard = 0
  while (cur.getTime() <= end.getTime() && guard < 400) {
    list.push(fmtDate(cur))
    cur.setDate(cur.getDate() + 1)
    guard++
  }
  return list
}

/** 把 /spot/tidal 的批次展开成可预约的车位列表（仅保留 canReserve 且空闲的车位） */
const collectTidalSpots = (data) => {
  const batches = (data && data.batches) || []
  const out = []
  batches.forEach((b) => {
    if (!b || !b.canReserve) return
    ;(b.spots || []).forEach((s) => {
      if (!s || s.status !== 0) return
      out.push({
        id: s.id,
        spotCode: s.spotCode,
        spotType: 1,
        tidalBatchId: b.id,
        batchName: b.batchName,
        openTime: b.openTime,
        closeTime: b.closeTime,
        pricePerHour: b.pricePerHour,
        freeMinutes: b.freeMinutes,
        allowIdentities: b.allowIdentities,
        area: b.area,
        zoneCode: TIDAL_ZONE_PREFIX + b.id,
        zoneName: b.batchName || b.area || '潮汐临时停车区'
      })
    })
  })
  return out
}

Page({
  data: {
    spots: [],          // 当前身份可停的全部空闲车位（含可预约的潮汐车位）
    zones: [],          // 区域列表（第一步选区域）
    zoneIndex: -1,
    selectedZone: null,
    zoneSpots: [],      // 当前区域下的车位（第二步选车位）
    spotIndex: -1,
    selectedSpot: null,
    startDate: '',
    startTime: '',
    dateList: [],
    timeList: [],
    timeIndex: 0,
    timeHint: '',
    durationIndex: 0,
    durations: ALL_DURATIONS,
    endTime: '',
    totalFee: 0,
    submitting: false,
    loading: false,
    errorMsg: '',
    tidalHint: '',      // 选中潮汐车位时的开放时间段提示
    // ---- 校园身份相关 ----
    benefits: null,
    identityLabel: '',
    verified: false,
    remainingQuota: -1,
    identityHint: '',
    // ---- 车位搜索 ----
    spotKeyword: '',      // 搜索关键字（车位编号/区域/分区名）
    searchMatched: 0      // 当前关键字匹配到的车位数（用于显示提示）
  },

  onLoad(options) {
    // 从「潮汐车位」页跳转过来时携带的参数：批次 / 车位 / 开放时间段
    const opt = options || {}
    this.tidalParam = {
      batchId: opt.tidalBatchId ? String(opt.tidalBatchId) : '',
      spotId: opt.spotId ? String(opt.spotId) : '',
      openTime: opt.openTime ? decodeURIComponent(opt.openTime) : '',
      closeTime: opt.closeTime ? decodeURIComponent(opt.closeTime) : ''
    }

    const now = new Date()
    const dates = buildDateList(14, now)
    this.setData({ dateList: dates })
    // 今天已无可预约时段时，直接定位到明天
    const todaySlots = buildTimeSlots(dates[0], now)
    const initialDate = todaySlots.length ? dates[0] : dates[1]
    this.setData({ startDate: initialDate })
    this.applyTimeSlots(initialDate)
    this.loadBenefits()
  },

  /** 加载校园身份权益：决定可预约区域、优惠与每日上限 */
  loadBenefits() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.loadSpots()
      return
    }
    request.get('/user/benefits', {}, true).then((res) => {
      const b = res.data || {}
      this.setData({
        benefits: b,
        identityLabel: b.identityLabel || '',
        verified: !!b.verified,
        remainingQuota: typeof b.remainingQuota === 'number' ? b.remainingQuota : -1,
        identityHint: `身份：${b.identityLabel || '未设置'}（${b.identityStatusLabel || '待审核'}）· 费率系数 ${b.discount} · 免费 ${b.freeMinutes} 分钟`
      })
      this.loadSpots()
    }).catch(() => this.loadSpots())
  },

  onShow() {
    // 页面停留期间时间会流逝，回到页面时刷新可选时段
    if (this.data.startDate) {
      this.applyTimeSlots(this.data.startDate, true)
    }
  },

  /**
   * 当前生效的开放时间段（仅潮汐车位有）。
   * 已选具体车位时按该车位的批次；只选了区域时按该区域第一个车位。
   * @returns {{open:number, close:number}|null}
   */
  activeWindow() {
    const spot = this.data.selectedSpot
    if (spot && spot.spotType === 1) {
      const open = strToTs(spot.openTime)
      const close = strToTs(spot.closeTime)
      if (open && close) return { open, close }
    }
    const zone = this.data.selectedZone
    if (zone && String(zone.code).indexOf(TIDAL_ZONE_PREFIX) === 0) {
      const first = (this.data.zoneSpots || [])[0]
      if (first) {
        const open = strToTs(first.openTime)
        const close = strToTs(first.closeTime)
        if (open && close) return { open, close }
      }
    }
    return null
  },

  /** 某日期可选的开始时间：先去掉当天已过去的时段，潮汐车位再限制在开放时间段内 */
  allowedSlots(dateStr, now) {
    const slots = buildTimeSlots(dateStr, now)
    const win = this.activeWindow()
    if (!win) return slots
    // 开始时间必须落在开放时间段内，且至少能容纳 1 小时（最短预约时长）
    return slots.filter((t) => {
      const ts = toTimestamp(dateStr, t)
      return ts >= win.open && ts + 3600000 <= win.close
    })
  },

  /**
   * 根据日期刷新可选时段（过去时段不会出现），并把选中项收敛到合法值
   */
  applyTimeSlots(dateStr, keepSelection) {
    const now = new Date()
    let date = dateStr
    let slots = this.allowedSlots(date, now)
    let hint = ''

    // 当天若已无可用时段，自动切换到下一个还有空档的日期
    if (slots.length === 0) {
      const nextDate = (this.data.dateList || []).find((d) => d > date)
      if (nextDate) {
        date = nextDate
        slots = this.allowedSlots(nextDate, now)
        keepSelection = false
        hint = '该日期已无可预约时段，已自动切换到下一天'
      }
    }

    const currentTime = this.data.startTime
    let timeIndex = 0
    if (keepSelection && currentTime) {
      const idx = slots.indexOf(currentTime)
      timeIndex = idx >= 0 ? idx : 0
    }
    const startTime = slots[timeIndex] || ''

    const win = this.activeWindow()
    if (!hint) {
      if (win) {
        hint = `潮汐车位仅可预约开放时间段内的时段（${tsToText(win.open)} ~ ${tsToText(win.close)}）`
      } else {
        hint = date === fmtDate(now)
          ? `仅可选择当前时间（${pad(now.getHours())}:${pad(now.getMinutes())}）之后的时段`
          : '该日期全天时段可选'
      }
    }

    this.setData({
      startDate: date,
      timeList: slots,
      timeIndex,
      startTime,
      timeHint: hint
    })
    if (hint && hint.indexOf('自动切换') >= 0) {
      wx.showToast({ title: hint, icon: 'none', duration: 2500 })
    }
    this.rebuildDurations()
  },

  /** 按当前开始时间过滤预约时长：潮汐车位不能超过结束时间 */
  rebuildDurations() {
    const { startDate, startTime } = this.data
    const win = this.activeWindow()
    let list = ALL_DURATIONS
    if (win && startDate && startTime) {
      const start = toTimestamp(startDate, startTime)
      const maxHours = (win.close - start) / 3600000
      const fitted = ALL_DURATIONS.filter((d) => d.value <= maxHours + 1e-6)
      list = fitted.length ? fitted : [ALL_DURATIONS[0]]
    }
    let idx = this.data.durationIndex
    if (idx >= list.length) idx = 0
    this.setData({ durations: list, durationIndex: idx })
    this.calcFee()
  },

  /**
   * 车位/区域变化后收敛时间选择范围：
   * 潮汐车位把日期列表收窄到开放时间段，普通车位恢复默认 14 天
   */
  applySpotWindow() {
    const win = this.activeWindow()
    const now = new Date()
    if (win) {
      let dates = buildRangeDateList(win.open, win.close)
      if (!dates.length) dates = buildDateList(14, now)
      const cur = this.data.startDate
      const startDate = dates.indexOf(cur) >= 0 ? cur : dates[0]
      this.setData({
        dateList: dates,
        tidalHint: `该批次仅可在开放时间段内预约：${tsToText(win.open)} ~ ${tsToText(win.close)}`
      })
      this.applyTimeSlots(startDate)
    } else {
      const dates = buildDateList(14, now)
      const cur = this.data.startDate
      const startDate = dates.indexOf(cur) >= 0 ? cur : dates[0]
      this.setData({ dateList: dates, tidalHint: '' })
      this.applyTimeSlots(startDate)
    }
  },

  loadSpots() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ errorMsg: '请先登录', loading: false })
      wx.showModal({
        title: '提示',
        content: '请先登录后再预约车位',
        confirmText: '去登录',
        success(res) {
          if (res.confirm) wx.navigateTo({ url: '/pages/login/login' })
        }
      })
      return
    }
    this.setData({ loading: true, errorMsg: '' })
    const b = this.data.benefits || {}
    const identity = Number(b.identityType) || 1
    const names = zoneNameMap(b)
    Promise.all([
      request.get('/spot/list?page=1&pageSize=500'),
      // 潮汐车位：开放前 7 天起可预约；这里静默失败，不能影响普通车位预约
      request.get('/spot/tidal', {}, true).catch(() => ({ data: { batches: [] } }))
    ]).then(([listRes, tidalRes]) => {
      const normal = (listRes.data.records || []).filter((s) => s.status === 0)
      const tidal = collectTidalSpots(tidalRes.data)
      const spots = normal.concat(tidal)
        // 校园分区规则：只展示当前身份允许停放的区域
        .filter((s) => {
          const allow = String(s.allowIdentities || '').split(',').map((x) => x.trim()).filter(Boolean)
          return allow.length === 0 || allow.indexOf(String(identity)) >= 0
        })
        .map((s) => {
          const zoneName = s.zoneName || names[s.zoneCode] || s.area || ''
          const mark = s.spotType === 1 ? ' · 潮汐' : ''
          // 区域已在第一步选中，车位标签里不再重复区域名
          return { ...s, zoneName, label: `${s.spotCode} · ¥${s.pricePerHour}/h${mark}` }
        })
      // 先按区域聚合，用户先选区域再选车位
      const zones = buildZoneList(spots, names)
      // 备份原始数据用于搜索过滤
      this.allSpots = spots
      this.allZones = zones
      this.zoneNames = names
      this.setData({
        spots,
        zones,
        zoneIndex: -1,
        selectedZone: null,
        zoneSpots: [],
        spotIndex: -1,
        selectedSpot: null,
        tidalHint: '',
        loading: false,
        spotKeyword: '',
        searchMatched: 0
      })
      if (spots.length === 0) {
        this.setData({ errorMsg: '当前身份暂无可预约车位（不同身份可停区域不同，可在「账号信息」查看校园权益）' })
      } else {
        this.preselectFromParam()
      }
    }).catch((err) => {
      // 业务错误带 code/message，网络错误使用默认提示
      const msg = (err && err.code && err.message) ? err.message : '网络异常，请检查网络连接'
      this.setData({ loading: false, errorMsg: msg })
    })
  },

  /** 从「潮汐车位」页跳转过来时，自动定位到对应批次（与车位） */
  preselectFromParam() {
    const p = this.tidalParam || {}
    if (!p.batchId) return
    const zoneCode = TIDAL_ZONE_PREFIX + p.batchId
    const zi = this.data.zones.findIndex((z) => z.code === zoneCode)
    if (zi < 0) return
    const zone = this.data.zones[zi]
    const zoneSpots = spotsOfZone(this.data.spots, zoneCode)
    let si = -1
    if (p.spotId) si = zoneSpots.findIndex((s) => String(s.id) === String(p.spotId))
    this.setData({
      zoneIndex: zi,
      selectedZone: zone,
      zoneSpots,
      spotIndex: si,
      selectedSpot: si >= 0 ? zoneSpots[si] : null
    })
    this.applySpotWindow()
  },

  /** 搜索关键字输入：实时过滤车位池和区域列表 */
  onKeywordInput(e) {
    const kw = e.detail.value
    this.setData({ spotKeyword: kw })
    this.applyKeywordFilter()
  },

  /** 点击搜索按钮：触发一次过滤（输入框已实时联动，这里做兜底） */
  onSpotSearch() {
    this.applyKeywordFilter()
  },

  /**
   * 关键字过滤：跨区域按车位编号 / 区域名 / 分区名 模糊匹配
   * 关键字为空时恢复原始 zones/spots；非空时 zones 只显示有匹配车位的区域，
   * 并在保留已选区域（若仍命中）的同时重建 zoneSpots。
   */
  applyKeywordFilter() {
    const kw = String(this.data.spotKeyword || '').trim().toLowerCase()
    const allSpots = this.allSpots || []
    const allZones = this.allZones || []
    const names = this.zoneNames || {}

    if (!kw) {
      // 关键字为空：恢复原区域列表，已选区域的车位也恢复
      const z = this.data.selectedZone
      const zoneSpots = z ? spotsOfZone(allSpots, z.code) : []
      this.setData({ zones: allZones, spots: allSpots, zoneSpots, searchMatched: 0 })
      return
    }

    // 跨字段模糊匹配：车位编号 / 区域名 area / 分区名 zoneName
    const filtered = allSpots.filter((s) => {
      const code = String(s.spotCode || '').toLowerCase()
      const area = String(s.area || '').toLowerCase()
      const zoneName = String(s.zoneName || '').toLowerCase()
      return code.indexOf(kw) >= 0 || area.indexOf(kw) >= 0 || zoneName.indexOf(kw) >= 0
    })

    // 用过滤后的 spots 重新聚合区域（重建 count/priceText）
    const zones = buildZoneList(filtered, names)

    // 若原选中的区域仍命中，保留选中；否则清空车位选择
    const oldCode = this.data.selectedZone ? this.data.selectedZone.code : null
    const newZone = oldCode ? zones.find((z) => z.code === oldCode) : null
    const zoneSpots = newZone ? spotsOfZone(filtered, newZone.code) : []

    this.setData({
      spots: filtered,
      zones,
      zoneSpots,
      zoneIndex: newZone ? zones.findIndex((z) => z.code === newZone.code) : -1,
      selectedZone: newZone,
      // 已选车位若不在过滤结果中则清空
      spotIndex: -1,
      selectedSpot: null,
      endTime: '',
      totalFee: 0,
      searchMatched: filtered.length
    })
  },

  /** 第一步：选择停车区域 */
  onZoneChange(e) {
    const idx = Number(e.detail.value)
    const zone = this.data.zones[idx]
    if (!zone) return
    this.setData({
      zoneIndex: idx,
      selectedZone: zone,
      zoneSpots: spotsOfZone(this.data.spots, zone.code),
      // 换区域后原车位失效，需重新选择
      spotIndex: -1,
      selectedSpot: null,
      endTime: '',
      totalFee: 0
    })
    this.applySpotWindow()
  },

  /** 第二步：在所选区域内选择车位 */
  onSpotChange(e) {
    const idx = Number(e.detail.value)
    this.setData({ spotIndex: idx, selectedSpot: this.data.zoneSpots[idx] })
    this.applySpotWindow()
  },

  onDateChange(e) {
    const date = this.data.dateList[e.detail.value]
    this.applyTimeSlots(date)
  },

  onTimeChange(e) {
    const idx = Number(e.detail.value)
    const startTime = this.data.timeList[idx] || ''
    this.setData({ timeIndex: idx, startTime })
    this.rebuildDurations()
  },

  onDurationChange(e) {
    this.setData({ durationIndex: e.detail.value })
    this.calcFee()
  },

  calcFee() {
    const { selectedSpot, startDate, startTime, durations, durationIndex } = this.data
    if (!selectedSpot || !startDate || !startTime) return
    const dur = durations[durationIndex] || durations[0]
    if (!dur) return
    const start = toTimestamp(startDate, startTime)
    if (!start) return
    const end = new Date(start + dur.value * 3600000)
    const endStr = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:00`
    this.setData({ endTime: endStr, totalFee: (selectedSpot.pricePerHour * dur.value).toFixed(2) })
  },

  handleReserve() {
    const { selectedSpot, selectedZone, startDate, startTime, endTime, durations, durationIndex } = this.data
    if (!selectedZone) { wx.showToast({ title: '请先选择停车区域', icon: 'none' }); return }
    if (!selectedSpot) { wx.showToast({ title: '请选择车位', icon: 'none' }); return }
    if (!startTime) { wx.showToast({ title: '请选择开始时间', icon: 'none' }); return }
    if (!endTime) { wx.showToast({ title: '请选择预约时长', icon: 'none' }); return }
    // 校园规则：身份需认证；每日预约次数上限
    if (!this.data.verified) {
      wx.showModal({
        title: '身份未认证',
        content: '校园预约需先完成身份认证。请到「我的 → 账号信息」填写学号/工号并等待管理员审核。',
        confirmText: '去认证',
        success: (r) => { if (r.confirm) wx.navigateTo({ url: '/pages/profile/profile' }) }
      })
      return
    }
    if (this.data.remainingQuota === 0) {
      wx.showToast({ title: '今日预约次数已达上限，请明天再试', icon: 'none', duration: 2500 })
      return
    }

    // 兜底校验：开始时间必须晚于当前时间（防止页面停留期间时间流逝）
    const start = toTimestamp(startDate, startTime)
    if (start <= Date.now()) {
      wx.showToast({ title: '开始时间不能早于当前时间，请重新选择', icon: 'none', duration: 2500 })
      this.applyTimeSlots(startDate)
      return
    }

    // 潮汐车位兜底校验：预约时段必须完整落在开放时间段内
    const win = this.activeWindow()
    if (win) {
      const dur = durations[durationIndex] || durations[0]
      const endTs = start + (dur ? dur.value : 1) * 3600000
      if (start < win.open) {
        wx.showToast({ title: `开始时间不能早于开放时间 ${tsToText(win.open)}`, icon: 'none', duration: 2500 })
        return
      }
      if (endTs > win.close) {
        wx.showToast({ title: `结束时间不能晚于 ${tsToText(win.close)}，请缩短预约时长`, icon: 'none', duration: 2500 })
        return
      }
    }

    const token = wx.getStorageSync('token')
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    const startDt = startDate + ' ' + startTime + ':00'
    request.post('/reservation/create', {
      spotId: selectedSpot.id,
      startTime: startDt,
      endTime: endTime
    }).then(() => {
      wx.showToast({ title: '预约成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => {}).finally(() => this.setData({ submitting: false }))
  }
})
