import request from '../../utils/request.js'
import { buildTimeSlots, buildDateList, fmtDate, startTimestamp as toTimestamp, pad } from '../../utils/timeSlot.js'

Page({
  data: {
    spots: [],
    spotIndex: -1,
    selectedSpot: null,
    startDate: '',
    startTime: '',
    dateList: [],
    timeList: [],
    timeIndex: 0,
    timeHint: '',
    durationIndex: 0,
    durations: [
      { label: '1小时', value: 1 }, { label: '2小时', value: 2 }, { label: '3小时', value: 3 },
      { label: '4小时', value: 4 }, { label: '6小时', value: 6 }, { label: '12小时', value: 12 },
      { label: '1天', value: 24 }, { label: '3天', value: 72 }, { label: '1周', value: 168 },
      { label: '1个月', value: 720 }
    ],
    endTime: '',
    totalFee: 0,
    submitting: false,
    loading: false,
    errorMsg: '',
    // ---- 校园身份相关 ----
    benefits: null,
    identityLabel: '',
    verified: false,
    remainingQuota: -1,
    identityHint: ''
  },

  onLoad() {
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
   * 根据日期刷新可选时段（过去时段不会出现），并把选中项收敛到合法值
   */
  applyTimeSlots(dateStr, keepSelection) {
    const now = new Date()
    let date = dateStr
    let slots = buildTimeSlots(date, now)
    let hint = ''

    // 今天若已无可用时段，自动切换到下一天
    if (slots.length === 0) {
      const nextDate = this.data.dateList.find((d) => d > date)
      if (nextDate) {
        date = nextDate
        slots = buildTimeSlots(date, now)
        keepSelection = false
        hint = '今天已无可预约时段，已自动切换到下一天'
      }
    }

    const currentTime = this.data.startTime
    let timeIndex = 0
    if (keepSelection && currentTime) {
      const idx = slots.indexOf(currentTime)
      timeIndex = idx >= 0 ? idx : 0
    }
    const startTime = slots[timeIndex] || ''

    if (!hint) {
      hint = date === fmtDate(now)
        ? `仅可选择当前时间（${pad(now.getHours())}:${pad(now.getMinutes())}）之后的时段`
        : '该日期全天时段可选'
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
    this.calcFee()
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
    const zoneNames = {}
    Object.keys(b.zones || {}).forEach((code) => { zoneNames[code] = b.zones[code].name })
    request.get('/spot/list?page=1&pageSize=500').then((res) => {
      const spots = (res.data.records || [])
        .filter(s => s.status === 0)
        // 校园分区规则：只展示当前身份允许停放的区域
        .filter((s) => {
          const allow = String(s.allowIdentities || '').split(',').map(x => x.trim()).filter(Boolean)
          return allow.length === 0 || allow.indexOf(String(identity)) >= 0
        })
        .map(s => {
          const zoneName = zoneNames[s.zoneCode] || s.zoneCode || ''
          return { ...s, zoneName, label: `${s.spotCode} · ${zoneName} · ¥${s.pricePerHour}/h` }
        })
      this.setData({ spots, loading: false })
      if (spots.length === 0) {
        this.setData({ errorMsg: '当前身份暂无可预约车位（不同身份可停区域不同，可在「账号信息」查看校园权益）' })
      }
    }).catch((err) => {
      // 业务错误带 code/message，网络错误使用默认提示
      const msg = (err && err.code && err.message) ? err.message : '网络异常，请检查网络连接'
      this.setData({ loading: false, errorMsg: msg })
    })
  },

  onSpotChange(e) {
    const idx = e.detail.value
    this.setData({ spotIndex: idx, selectedSpot: this.data.spots[idx] })
    this.calcFee()
  },

  onDateChange(e) {
    const date = this.data.dateList[e.detail.value]
    this.applyTimeSlots(date)
  },

  onTimeChange(e) {
    const idx = Number(e.detail.value)
    const startTime = this.data.timeList[idx] || ''
    this.setData({ timeIndex: idx, startTime })
    this.calcFee()
  },

  onDurationChange(e) {
    this.setData({ durationIndex: e.detail.value })
    this.calcFee()
  },

  calcFee() {
    const { selectedSpot, startDate, startTime, durations, durationIndex } = this.data
    if (!selectedSpot || !startDate || !startTime) return
    const dur = durations[durationIndex]
    const start = toTimestamp(startDate, startTime)
    if (!start) return
    const end = new Date(start + dur.value * 3600000)
    const endStr = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:00`
    this.setData({ endTime: endStr, totalFee: (selectedSpot.pricePerHour * dur.value).toFixed(2) })
  },

  handleReserve() {
    const { selectedSpot, startDate, startTime, endTime, durations, durationIndex } = this.data
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
