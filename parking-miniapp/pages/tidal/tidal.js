import request from '../../utils/request.js'
import spotSocket from '../../utils/spotSocket.js'

// 服务端时间可能是 "2026-10-15 08:00:00"，也可能是 "2026-10-15T08:00:00"，
// 统一 replace('T',' ') 后截取到分钟（项目内约定，见 utils/duration.js）
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

const toNumber = (v, dft) => {
  const n = Number(v)
  return isNaN(n) ? dft : n
}

const fmtPrice = (v) => {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

/** 阶段：upcoming 开放前·仅可预约 / open 开放中·可入场 / ended 已结束（兜底） */
const PHASE_META = {
  upcoming: { text: '开放前·仅可预约', cls: 'upcoming' },
  open: { text: '开放中·可入场', cls: 'open' },
  ended: { text: '已结束', cls: 'ended' }
}

/** 车位状态：0 空闲、1 已占用、2 已预约、3 维护中 */
const SPOT_STATUS = {
  0: { text: '空闲', cls: 'free' },
  1: { text: '已占用', cls: 'occupied' },
  2: { text: '已预约', cls: 'reserved' },
  3: { text: '维护中', cls: 'locked' }
}

Page({
  data: {
    loading: true,
    isLoggedIn: false,
    errorMsg: '',
    advanceDays: 7,   // 兜底值，实际以后端返回的 advanceDays 为准
    batches: []
  },

  onShow() {
    this.loadData()
    this.startSpotSocket()
  },

  onHide() {
    this.stopSpotSocket()
  },

  onUnload() {
    this.stopSpotSocket()
  },

  // ===================== 车位状态实时推送 =====================

  startSpotSocket() {
    if (!wx.getStorageSync('token')) return
    this.stopSpotSocket()
    this.spotSocketHandler = (msg) => this.onSpotMessage(msg)
    spotSocket.subscribe(this.spotSocketHandler)
  },

  stopSpotSocket() {
    if (this.spotSocketHandler) {
      spotSocket.unsubscribe(this.spotSocketHandler)
      this.spotSocketHandler = null
    }
  },

  /** 只更新批次内已知车位的状态并重算批次计数；批次增删仍由 loadData 负责 */
  onSpotMessage(msg) {
    if (!msg) return
    let items = null
    if (msg.type === 'snapshot') {
      items = (msg.spots || []).map(function(s) { return { id: s.id, status: s.status } })
    } else if (msg.type === 'patch') {
      items = msg.changes || []
    }
    if (!items || items.length === 0) return

    const statusById = {}
    items.forEach(function(c) { statusById[String(c.id)] = c.status })
    let changed = false

    const batches = (this.data.batches || []).map(function(b) {
      let vacant = 0
      let occupied = 0
      let reserved = 0
      const spots = (b.spots || []).map(function(s) {
        const next = statusById[String(s.id)]
        const status = next === undefined ? s.status : next
        if (status === 1) occupied += 1
        else if (status === 2) reserved += 1
        else if (status === 0) vacant += 1
        if (next !== undefined && next !== s.status) changed = true
        const meta = SPOT_STATUS[status] || { text: '未知', cls: 'locked' }
        return Object.assign({}, s, { status: status, statusText: meta.text, statusCls: meta.cls })
      })
      return Object.assign({}, b, {
        spots: spots,
        vacantCount: vacant,
        occupiedCount: occupied,
        reservedCount: reserved
      })
    })

    if (changed) this.setData({ batches: batches })
  },

  /** 下拉刷新：数据回来后再收起刷新动画 */
  onPullDownRefresh() {
    this.loadData(true)
      .then(() => wx.stopPullDownRefresh())
      .catch(() => wx.stopPullDownRefresh())
  },

  /**
   * 拉取潮汐车位批次
   * 统一用 silent=true，错误由页面内的错误块提示，避免下拉刷新时反复弹 toast
   */
  loadData(silent) {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, loading: false, errorMsg: '', batches: [] })
      return Promise.resolve()
    }
    if (!silent) this.setData({ isLoggedIn: true, loading: true, errorMsg: '' })
    return request.get('/spot/tidal', {}, true).then((res) => {
      const data = res.data || {}
      const rawBatches = data.batches || []
      // 提前预约天数优先取响应体顶层的 advanceDays；后端 VO 也可能放在批次上，这里兜底
      let advanceDays = typeof data.advanceDays === 'number' ? data.advanceDays : null
      if (advanceDays === null) {
        const withDays = rawBatches.find((b) => typeof b.advanceDays === 'number')
        advanceDays = withDays ? withDays.advanceDays : this.data.advanceDays
      }
      const batches = rawBatches.map((b) => this.decorateBatch(b, advanceDays))
      this.setData({ advanceDays, batches, loading: false, errorMsg: '' })
    }).catch(() => {
      // 保留已有数据（下拉刷新失败时不清空页面），只标记错误
      this.setData({ loading: false, errorMsg: '潮汐车位加载失败，请稍后下拉重试' })
    })
  },

  onRetry() {
    this.loadData()
  },

  /** 把接口返回的批次加工成页面可直接渲染的结构 */
  decorateBatch(b, advanceDays) {
    const phase = PHASE_META[b.phase] ? b.phase : 'ended'
    const meta = PHASE_META[phase]
    const days = typeof b.daysUntilOpen === 'number' ? b.daysUntilOpen : null
    const canReserve = !!b.canReserve
    const canEnter = !!b.canEnter
    const freeMinutes = toNumber(b.freeMinutes, 0)
    const advance = typeof advanceDays === 'number' ? advanceDays : 7

    let statusText = ''
    if (phase === 'open') {
      statusText = '开放中，可直接入场或预约'
    } else if (phase === 'upcoming') {
      const daysText = days === null ? '即将开放' : (days <= 0 ? '今日开放' : `距开放还有 ${days} 天`)
      statusText = `${daysText} · 开放前仅可预约，开放后凭预约入场`
    } else {
      statusText = '本批次已结束，不再开放'
    }

    // canReserve=false 时按钮置灰，并在此给出原因
    let reserveTip = ''
    if (!canReserve) {
      if (phase === 'ended') reserveTip = '该批次已结束'
      else if (phase === 'open' && canEnter) reserveTip = '当前开放中，可直接入场'
      else reserveTip = `开放前 ${advance} 天才可预约`
    }

    const spots = (b.spots || []).map((s) => {
      const status = toNumber(s.status, 3)
      const st = SPOT_STATUS[status] || { text: '未知', cls: 'locked' }
      return { id: s.id, spotCode: s.spotCode, status, statusText: st.text, statusCls: st.cls }
    })

    return {
      id: b.id,
      batchName: b.batchName || '潮汐车位',
      area: b.area || '',
      floor: b.floor || '',
      zoneIdentityNames: b.zoneIdentityNames || '',
      remark: b.remark || '',
      priceText: fmtPrice(b.pricePerHour),
      freeText: freeMinutes > 0 ? `免费 ${freeMinutes} 分钟` : '',
      openTime: b.openTime,     // 原样保留：拼预约页 URL 时使用
      closeTime: b.closeTime,
      openTimeText: fmtTime(b.openTime),
      closeTimeText: fmtTime(b.closeTime),
      phase,
      phaseText: b.phaseLabel || meta.text,
      phaseCls: meta.cls,
      canReserve,
      canEnter,
      statusText,
      reserveTip,
      spotCount: toNumber(b.spotCount, spots.length),
      vacantCount: toNumber(b.vacantCount, 0),
      occupiedCount: toNumber(b.occupiedCount, 0),
      reservedCount: toNumber(b.reservedCount, 0),
      spots
    }
  },

  findBatch(id) {
    return (this.data.batches || []).find((b) => String(b.id) === String(id)) || null
  },

  /** 批次卡片底部「去预约」 */
  goReserve(e) {
    const batch = this.findBatch(e.currentTarget.dataset.id)
    if (!batch) return
    if (!batch.canReserve) {
      wx.showToast({ title: batch.reserveTip || '暂不可预约', icon: 'none' })
      return
    }
    this.navigateToReserve(batch)
  },

  /** 点击车位小格子：仅空闲车位可预约 */
  goReserveSpot(e) {
    const d = e.currentTarget.dataset
    if (toNumber(d.status, -1) !== 0) {
      wx.showToast({ title: '该车位当前不可预约', icon: 'none' })
      return
    }
    const batch = this.findBatch(d.batchId)
    if (!batch) return
    if (!batch.canReserve) {
      wx.showToast({ title: batch.reserveTip || '暂不可预约', icon: 'none' })
      return
    }
    this.navigateToReserve(batch, d.spotId)
  },

  /** 跳转预约页：按约定拼 tidalBatchId / openTime / closeTime（点具体车位时附加 spotId） */
  navigateToReserve(batch, spotId) {
    let url = '/pages/reserve/reserve?tidalBatchId=' + batch.id
      + '&openTime=' + encodeURIComponent(batch.openTime)
      + '&closeTime=' + encodeURIComponent(batch.closeTime)
    if (spotId) url += '&spotId=' + spotId
    wx.navigateTo({ url })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
