const app = getApp()
import { formatDuration, createTicker, clearTicker } from '../../utils/duration.js'
import { createPoller } from '../../utils/poller.js'
import request from '../../utils/request.js'
import spotSocket from '../../utils/spotSocket.js'
import { cacheGet, cacheSet, cacheClear } from '../../utils/pageCache.js'

/**
 * 潮汐车位入口文案：有开放中的批次时优先提示"可入场"，
 * 否则按最近一个批次的开放倒计时提示"现在可预约"
 */
function buildTidalText(batches) {
  if (!batches || batches.length === 0) return ''
  const openList = batches.filter(function(b) { return b.phase === 'open' || b.canEnter })
  if (openList.length > 0) {
    return '潮汐车位开放中：' + openList.length + ' 个临时停车区可入场'
  }
  const days = batches
    .map(function(b) { return b.daysUntilOpen })
    .filter(function(d) { return typeof d === 'number' })
  const minDays = days.length ? Math.min.apply(null, days) : null
  if (minDays === null || minDays <= 0) {
    return '潮汐车位：' + batches.length + ' 个临时停车区可预约'
  }
  return '潮汐车位：' + batches.length + ' 个临时停车区 ' + minDays + ' 天后开放，现在可预约'
}

/**
 * 拼装「当前身份 + 可见区域」提示文案
 * 例：当前身份：访客 · 仅展示你身份可停放的区域：访客与家长临时停车区、潮汐临时停车区
 */
function buildPermText(data) {
  if (!data || !data.identityLabel) return ''
  const zones = (data.zoneNames || []).join('、')
  if (!zones) return '当前身份：' + data.identityLabel
  return '当前身份：' + data.identityLabel + ' · 仅展示你身份可停放的区域：' + zones
}

/**
 * 校园余位环（§4.1）：从 areaGroups 汇总空闲车位（status === 0 与 spots 页口径一致）
 * 返回 { vacant, total }；无数据时 total 为 0
 */
function countVacantInGroups(areaGroups) {
  let vacant = 0
  let total = 0
  ;(areaGroups || []).forEach(function(g) {
    ;((g && g.spots) || []).forEach(function(s) {
      total += 1
      if (s && String(s.status) === '0') vacant += 1
    })
  })
  return { vacant: vacant, total: total }
}

/** 余位比例配色：>50% 绿 / 20-50% 橙 / <20% 红（与设计令牌一致） */
function ringColorOf(percent) {
  if (percent > 50) return '#34B37E'
  if (percent >= 20) return '#E6A23C'
  return '#F56C6C'
}

/** 生成余位环 SVG data-URI（圆环 stroke-dasharray + 中心百分比白字） */
function buildRingImage(percent) {
  const C = 339.29 // 2 * π * 54
  const clamped = Math.max(0, Math.min(100, percent))
  const dash = (clamped / 100 * C).toFixed(1)
  const svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'>"
    + "<circle cx='60' cy='60' r='54' fill='none' stroke='rgba(255,255,255,0.28)' stroke-width='10'/>"
    + "<circle cx='60' cy='60' r='54' fill='none' stroke='" + ringColorOf(clamped) + "' stroke-width='10' stroke-linecap='round'"
    + " stroke-dasharray='" + dash + " " + C + "' transform='rotate(-90 60 60)'/>"
    + "<text x='60' y='70' text-anchor='middle' font-size='28' font-weight='600' fill='#ffffff'>" + Math.round(clamped) + "%</text>"
    + '</svg>'
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

Page({
  data: {
    userInfo: null,
    needLogin: false,
    greeting: '',         // 问候语（早上好/下午好/晚上好），由 onShow 计算
    totalSpots: 0,        // 全部车位的总数量（首页入口卡片显示）
    vacantTotal: 0,       // 余位环：空闲车位数（areaGroups 汇总）
    ringPercent: -1,      // 余位环：空闲占比；-1 表示无数据/未登录，整块不渲染
    ringImage: '',        // 余位环：SVG data-URI
    parkedVehicles: [],
    activeReservations: [],
    areaGroups: [],
    // 潮汐车位入口：只保留可预约 / 可入场的批次，没有就不显示卡片
    tidalBatches: [],
    tidalCount: 0,
    tidalText: '',
    permText: ''          // 当前身份与可见区域提示文案（RBAC），接口失败时为空不展示
  },

  onShow() {
    this.setGreeting()
    this.loadData()
    this.startTicker()
    this.startSync()
    this.startSpotSocket()
  },

  /** 根据当前小时设置问候语（早 6-11 / 午 11-14 / 下午 14-18 / 晚 18-6） */
  setGreeting() {
    const h = new Date().getHours()
    let g = '晚上好'
    if (h >= 5 && h < 11) g = '早上好'
    else if (h >= 11 && h < 14) g = '中午好'
    else if (h >= 14 && h < 18) g = '下午好'
    if (this.data.greeting !== g) this.setData({ greeting: g })
  },

  onHide() {
    this.stopTicker()
    this.stopSync()
    this.stopSpotSocket()
  },

  onUnload() {
    this.stopTicker()
    this.stopSync()
    this.stopSpotSocket()
  },

  /** 启动"已停时长"实时刷新（每秒检查，文案变化才 setData） */
  startTicker() {
    this.stopTicker()
    this.durationTimer = createTicker(() => this.refreshDurations(), 1000)
  },

  stopTicker() {
    clearTicker(this.durationTimer)
    this.durationTimer = null
  },

  /**
   * 启动与后端的状态同步（5 秒一次）：
   * 管理员在后台强制结算/释放车位后，用户端不需要手动刷新就能看到最新数据
   */
  startSync() {
    this.stopSync()
    this.syncTick = 0
    this.syncPoller = createPoller(() => this.syncFromServer(), 5000)
    this.syncPoller.start()
  },

  stopSync() {
    if (this.syncPoller) {
      this.syncPoller.stop()
      this.syncPoller = null
    }
  },

  syncFromServer() {
    if (this.data.needLogin || !wx.getStorageSync('token')) return Promise.resolve()
    this.syncTick = (this.syncTick || 0) + 1
    // 概览每轮都拉：WebSocket 只推车位状态，在场车辆/预约/余额等个人数据仍需 HTTP 更新
    // （后端 /spot/overview 带 3 秒短缓存，5 秒一轮不会造成压力）
    const jobs = [this.loadOverview(true)]
    // 余额变化相对慢，每 3 轮（15 秒）同步一次即可
    if (this.syncTick % 3 === 1) jobs.push(this.loadUserInfo(true))
    return Promise.all(jobs)
  },

  // ===================== 车位状态实时推送 =====================

  startSpotSocket() {
    if (this.data.needLogin || !wx.getStorageSync('token')) return
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

  onSpotMessage(msg) {
    if (!msg || (msg.type !== 'snapshot' && msg.type !== 'patch')) return
    const oldGroups = this.data.areaGroups || []
    let groups
    if (msg.type === 'snapshot') {
      groups = this.buildGroupsFromSnapshot(msg.spots || [], oldGroups)
    } else {
      groups = this.applySpotPatch(msg.changes || [], oldGroups)
    }
    const totalSpots = groups.reduce(function(sum, g) {
      return sum + ((g && g.spots) ? g.spots.length : 0)
    }, 0)
    const stat = countVacantInGroups(groups)
    const ringPercent = stat.total > 0 ? (stat.vacant / stat.total * 100) : -1
    this.setData({
      areaGroups: groups,
      totalSpots: totalSpots,
      vacantTotal: stat.vacant,
      ringPercent: ringPercent,
      ringImage: ringPercent >= 0 ? buildRingImage(ringPercent) : ''
    })
  },

  /** snapshot：按区域重建车位分组，区域顺序沿用页面已有顺序；保留本地已有字段 */
  buildGroupsFromSnapshot(spots, oldGroups) {
    const grouped = {}
    const areas = []
    const oldById = {}
    oldGroups.forEach(function(g) {
      if (g && g.area && !grouped[g.area]) { grouped[g.area] = []; areas.push(g.area) }
      ;(g.spots || []).forEach(function(s) { oldById[String(s.id)] = s })
    })
    spots.forEach(function(s) {
      const area = s.area || '未分区'
      if (!grouped[area]) { grouped[area] = []; areas.push(area) }
      grouped[area].push(Object.assign({}, oldById[String(s.id)] || {}, {
        id: s.id,
        spotCode: s.spotCode,
        area: area,
        zoneCode: s.zoneCode,
        allowIdentities: s.allowIdentities,
        status: s.status
      }))
    })
    return areas
      .map(function(area) { return { area: area, spots: grouped[area] } })
      .filter(function(g) { return g.spots.length > 0 })
  },

  /** patch：只更新状态；维护中的车位对用户不可见，直接从列表移除 */
  applySpotPatch(changes, oldGroups) {
    if (!changes.length) return oldGroups
    const statusById = {}
    changes.forEach(function(c) { statusById[String(c.id)] = c.status })
    return oldGroups.map(function(g) {
      const spots = (g.spots || []).map(function(s) {
        const st = statusById[String(s.id)]
        if (st === undefined) return s
        if (st === 3) return null
        return Object.assign({}, s, { status: st })
      }).filter(Boolean)
      return { area: g.area, spots: spots }
    }).filter(function(g) { return g.spots.length > 0 })
  },

  /**
   * 重算在场车辆的已停时长：页面停留期间时间会走，不需要用户手动刷新
   * 只有文案真的变化（跨过整分钟）才 setData，避免每秒无谓渲染
   */
  refreshDurations() {
    const list = this.data.parkedVehicles
    if (!list || list.length === 0) return
    let changed = false
    const next = list.map((item) => {
      const text = formatDuration(item.entryTime)
      if (text === item.durationText) return item
      changed = true
      return Object.assign({}, item, { durationText: text })
    })
    if (changed) this.setData({ parkedVehicles: next })
  },

  loadData() {
    var token = wx.getStorageSync('token')

    // 未登录：不向服务端请求任何停车数据，页面只显示登录引导
    // （后端 /spot/overview 已改为必须登录，未登录会直接返回 401）
    if (!token) {
      this.setData({
        needLogin: true,
        userInfo: null,
        totalSpots: 0,
        vacantTotal: 0,
        ringPercent: -1,
        ringImage: '',
        parkedVehicles: [],
        activeReservations: [],
        areaGroups: [],
        tidalBatches: [],
        tidalCount: 0,
        tidalText: '',
        permText: ''
      })
      return
    }
    this.setData({ needLogin: false })

    // ---- 用缓存立即渲染，切回 tab 时不再白屏等网络 ----
    const cachedOverview = cacheGet('spotOverview', 15000)
    if (cachedOverview) this.applyOverview(cachedOverview)
    try {
      const storedUser = wx.getStorageSync('userInfo')
      if (storedUser) this.setData({ userInfo: JSON.parse(storedUser) })
    } catch (e) { /* 本地缓存损坏时忽略 */ }
    const cachedPerms = cacheGet('myPerms', 30000)
    if (cachedPerms) this.setData({ permText: buildPermText(cachedPerms) })
    const cachedTidal = cacheGet('tidalBatches', 30000)
    if (cachedTidal) {
      this.setData({
        tidalBatches: cachedTidal.batches,
        tidalCount: cachedTidal.count,
        tidalText: cachedTidal.text
      })
    }

    // ---- 后台静默刷新最新数据 ----
    this.loadUserInfo()
    this.loadOverview()
    this.loadTidal()
    this.loadPerms()
  },

  /** 拉取账号信息（含余额）；silent=true 用于自动同步，不打扰用户 */
  loadUserInfo(silent) {
    const token = wx.getStorageSync('token')
    if (!token) return Promise.resolve()
    const self = this
    return new Promise(function(resolve) {
      wx.request({
        url: app.globalData.baseUrl + '/user/info',
        timeout: 10000,
        header: { 'Authorization': 'Bearer ' + token },
        success: function(res) {
          if (res.data.code === 200) {
            self.setData({ userInfo: res.data.data })
            wx.setStorageSync('userInfo', JSON.stringify(res.data.data))
          } else if (res.data.code === 401) {
            self.handleUnauthorized()
          }
          resolve()
        },
        fail: function() { resolve() }
      })
    })
  },

  /**
   * 拉取本人的停车概览（在场车辆 / 活跃预约只返回当前账号的数据）
   * @param {boolean} silent 自动同步时为 true：对比变化并给出提示
   */
  loadOverview(silent) {
    const token = wx.getStorageSync('token')
    if (!token) return Promise.resolve()
    const self = this
    return new Promise(function(resolve) {
      wx.request({
        url: app.globalData.baseUrl + '/spot/overview',
        timeout: 10000,
        header: { 'Authorization': 'Bearer ' + token },
        success: function(res) {
          if (res.data.code === 200) {
            cacheSet('spotOverview', res.data.data)
            self.applyOverview(res.data.data, !!silent)
          } else if (res.data.code === 401) {
            self.handleUnauthorized()
          } else if (!silent) {
            self.loadSpotsFallback(token)
          }
          resolve()
        },
        fail: function() {
          if (!silent) self.loadSpotsFallback(token)
          resolve()
        }
      })
    })
  },

  /**
   * 应用概览数据；isSync=true 时对比上一次数据：
   * 车辆消失（管理员结算出场 / 已在别处出场）→ 提示并立刻同步余额
   */
  applyOverview(data, isSync) {
    const prevIds = (this.data.parkedVehicles || []).map(function(v) { return String(v.recordId) })
    const vehicles = (data.parkedVehicles || []).map(function(pv) {
      pv.durationText = formatDuration(pv.entryTime)
      return pv
    })
    const nextIds = vehicles.map(function(v) { return String(v.recordId) })

    if (isSync) {
      const gone = prevIds.filter(function(id) { return nextIds.indexOf(id) < 0 })
      const added = nextIds.filter(function(id) { return prevIds.indexOf(id) < 0 })
      if (gone.length > 0 && added.length === 0) {
        wx.showToast({ title: '车辆已出场，费用已结算', icon: 'none', duration: 3000 })
        this.loadUserInfo(true) // 结算会扣余额，立即同步一次
      } else if (added.length > 0 && gone.length === 0) {
        wx.showToast({ title: '检测到车辆已入场', icon: 'none', duration: 2500 })
      }
    }

    // 计算全部车位总数（首页"全部车位"标题右侧统计）
    const totalSpots = (data.areaGroups || []).reduce(function(sum, g) {
      return sum + ((g && g.spots) ? g.spots.length : 0)
    }, 0)

    // 余位环：总数为 0 时视为无数据，不渲染（符合静默降级约定）
    const stat = countVacantInGroups(data.areaGroups)
    const ringPercent = stat.total > 0 ? (stat.vacant / stat.total * 100) : -1

    this.setData({
      parkedVehicles: vehicles,
      activeReservations: data.activeReservations || [],
      areaGroups: data.areaGroups || [],
      totalSpots: totalSpots,
      vacantTotal: stat.vacant,
      ringPercent: ringPercent,
      ringImage: ringPercent >= 0 ? buildRingImage(ringPercent) : ''
    })
  },

  /** 点击"全部车位"入口卡片：跳转到独立的车位页面 */
  goSpots() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/spots/spots' })
  },

  /**
   * 顺带拉取潮汐车位（新功能）：silent=true，任何失败都不弹错、不影响首页其它内容
   * 只保留 canReserve / canEnter 的批次；一个都没有时不显示入口卡片
   */
  loadTidal() {
    const token = wx.getStorageSync('token')
    if (!token) return Promise.resolve()
    const self = this
    return request.get('/spot/tidal', {}, true).then(function(res) {
      const all = (res.data && res.data.batches) || []
      const usable = all.filter(function(b) { return b.canReserve || b.canEnter })
      cacheSet('tidalBatches', {
        batches: usable,
        count: usable.length,
        text: buildTidalText(usable)
      })
      self.setData({
        tidalBatches: usable,
        tidalCount: usable.length,
        tidalText: buildTidalText(usable)
      })
    }).catch(function() {
      // 潮汐车位接口失败（未上线 / 网络异常）时静默降级：不显示入口，不影响首页其它模块
      self.setData({ tidalBatches: [], tidalCount: 0, tidalText: '' })
    })
  },

  /**
   * 拉取当前身份与可见停车区域（RBAC）：silent=true
   * 任何失败都静默降级（不弹错、不阻塞首页其它数据加载），失败时清空提示不展示
   */
  loadPerms() {
    const token = wx.getStorageSync('token')
    if (!token) { this.setData({ permText: '' }); return Promise.resolve() }
    const self = this
    return request.get('/user/my-perms', {}, true).then(function(res) {
      cacheSet('myPerms', res.data)
      self.setData({ permText: buildPermText(res.data) })
    }).catch(function() {
      self.setData({ permText: '' })
    })
  },

  // 账号被禁用或 token 失效：清掉本地登录态，回到"未登录"视图
  handleUnauthorized() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    cacheClear()
    this.setData({
      needLogin: true,
      userInfo: null,
      totalSpots: 0,
      vacantTotal: 0,
      ringPercent: -1,
      ringImage: '',
      parkedVehicles: [],
      activeReservations: [],
      areaGroups: [],
      tidalBatches: [],
      tidalCount: 0,
      tidalText: '',
      permText: ''
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goReserve() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/reserve/reserve' })
  },
  goTidal() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/tidal/tidal' })
  },
  goEnter() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/enter/enter' })
  },
  goExit() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/exit/exit' })
  },
  goRecharge() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/recharge/recharge' })
  },
  goReserveDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/reserve-detail/reserve-detail?id=' + id })
  },

  // 概览接口失败时的回退方案（同样需要登录态，未登录不再请求）
  loadSpotsFallback(token) {
    if (!token) { this.setData({ needLogin: true }); return }
    var self = this
    wx.request({
      url: app.globalData.baseUrl + '/spot/list?page=1&pageSize=500',
      timeout: 10000,
      header: { 'Authorization': 'Bearer ' + token },
      success: function(res) {
        if (res.data.code === 401) { self.handleUnauthorized(); return }
        if (res.data.code === 200) {
          var allSpots = (res.data.data && res.data.data.records) || []
          var groups = {}
          allSpots.forEach(function(s) {
            var area = s.area || '未分区'
            if (!groups[area]) groups[area] = []
            groups[area].push(s)
          })
          var areaGroups = Object.keys(groups).sort().map(function(area) {
            return { area: area, spots: groups[area] }
          })
          var total = areaGroups.reduce(function(sum, g) { return sum + g.spots.length }, 0)
          var stat = countVacantInGroups(areaGroups)
          var ringPercent = stat.total > 0 ? (stat.vacant / stat.total * 100) : -1
          self.setData({
            areaGroups: areaGroups,
            totalSpots: total,
            vacantTotal: stat.vacant,
            ringPercent: ringPercent,
            ringImage: ringPercent >= 0 ? buildRingImage(ringPercent) : ''
          })
        }
      }
    })
  }
})
