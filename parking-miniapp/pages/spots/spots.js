const app = getApp()
import request from '../../utils/request.js'
import spotSocket from '../../utils/spotSocket.js'
import { cacheGet, cacheSet, cacheClear } from '../../utils/pageCache.js'

/**
 * 拼装「当前身份 + 可见区域」提示文案（与首页保持一致）
 * 例：当前身份：访客 · 仅展示你身份可停放的区域：访客与家长临时停车区、潮汐临时停车区
 */
function buildPermText(data) {
  if (!data || !data.identityLabel) return ''
  const zones = (data.zoneNames || []).join('、')
  if (!zones) return '当前身份：' + data.identityLabel
  return '当前身份：' + data.identityLabel + ' · 仅展示你身份可停放的区域：' + zones
}

/**
 * 全部车位页面：从首页抽离
 * - onShow 时拉取 /spot/overview 获取 areaGroups
 * - 计算总车位数 totalSpots、区域数 areaGroups.length、空闲车位 vacantCount
 * - 保留搜索 + 折叠逻辑（与首页原实现一致）
 */
Page({
  data: {
    loading: true,
    errorMsg: '',
    areaGroups: [],
    totalSpots: 0,
    vacantCount: 0,
    // ---- 车位搜索 ----
    spotKeyword: '',
    searchMatched: 0,
    // ---- 区域折叠：默认收起，点击展开；搜索时自动展开 ----
    collapsedAreas: {},
    // ---- 当前身份与可见区域提示（RBAC），接口失败时为空不展示 ----
    permText: ''
  },

  onShow() {
    this.loadSpots()
    this.loadPerms()
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

  onSpotMessage(msg) {
    if (!msg || (msg.type !== 'snapshot' && msg.type !== 'patch')) return
    const oldGroups = this.allAreaGroups || []
    const groups = msg.type === 'snapshot'
      ? this.buildGroupsFromSnapshot(msg.spots || [], oldGroups)
      : this.applySpotPatch(msg.changes || [], oldGroups)
    this.allAreaGroups = groups
    let totalSpots = 0
    let vacantCount = 0
    groups.forEach(function(g) {
      (g.spots || []).forEach(function(s) {
        totalSpots += 1
        if (String(s.status) === '0') vacantCount += 1
      })
    })
    this.setData({ loading: false, errorMsg: '', totalSpots: totalSpots, vacantCount: vacantCount })
    this.applyKeywordFilter()
  },

  /** snapshot：按区域重建分组，区域顺序沿用页面已有顺序；保留本地已有的价格等展示字段 */
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
   * 拉取当前身份与可见停车区域（RBAC）：silent=true，失败静默降级不展示提示
   * 与 loadSpots 并行，互不阻塞
   */
  loadPerms() {
    const token = wx.getStorageSync('token')
    if (!token) { this.setData({ permText: '' }); return Promise.resolve() }
    const cached = cacheGet('myPerms', 30000)
    if (cached) this.setData({ permText: buildPermText(cached) })
    const self = this
    return request.get('/user/my-perms', {}, true).then(function(res) {
      cacheSet('myPerms', res.data)
      self.setData({ permText: buildPermText(res.data) })
    }).catch(function() {
      self.setData({ permText: '' })
    })
  },

  /** 拉取车位概览（必须登录态）；命中首页缓存时立即渲染，再后台静默刷新 */
  loadSpots() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    const cached = cacheGet('spotOverview', 15000)
    if (cached) {
      this.applyOverview(cached)
    } else {
      this.setData({ loading: true, errorMsg: '' })
    }
    const self = this
    wx.request({
      url: app.globalData.baseUrl + '/spot/overview',
      timeout: 10000,
      header: { 'Authorization': 'Bearer ' + token },
      success: function(res) {
        if (res.data.code === 200) {
          cacheSet('spotOverview', res.data.data)
          self.applyOverview(res.data.data)
        } else if (res.data.code === 401) {
          // token 失效：清登录态后回到首页（首页会引导登录）
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          cacheClear()
          wx.reLaunch({ url: '/pages/index/index' })
        } else if (!cached) {
          self.setData({ loading: false, errorMsg: res.data.msg || '加载失败' })
        }
      },
      fail: function() {
        // 已有缓存数据时网络失败不打断浏览
        if (!cached) self.setData({ loading: false, errorMsg: '网络异常，请稍后重试' })
      }
    })
  },

  /** 应用概览数据，计算统计指标 */
  applyOverview(data) {
    const areaGroups = (data && data.areaGroups) || []
    const totalSpots = areaGroups.reduce(function(sum, g) {
      return sum + ((g && g.spots) ? g.spots.length : 0)
    }, 0)
    // 空闲车位 = status === 0 的车位（与首页 spot-status-0 一致）
    let vacantCount = 0
    areaGroups.forEach(function(g) {
      (g.spots || []).forEach(function(s) {
        if (String(s.status) === '0') vacantCount += 1
      })
    })
    this.allAreaGroups = areaGroups
    this.setData({
      loading: false,
      errorMsg: '',
      areaGroups: areaGroups,
      totalSpots: totalSpots,
      vacantCount: vacantCount
    })
    if (this.data.spotKeyword) this.applyKeywordFilter()
  },

  /** 搜索关键字输入：实时过滤下方车位网格 */
  onKeywordInput(e) {
    const kw = e.detail.value
    this.setData({ spotKeyword: kw })
    this.applyKeywordFilter()
  },

  /** 点击搜索按钮：兜底触发一次过滤 */
  onSpotSearch() {
    this.applyKeywordFilter()
  },

  /**
   * 关键字过滤：跨区域按车位编号 / 区域名模糊匹配
   * 关键字为空时恢复原始 areaGroups；非空时只保留有匹配车位的区域，
   * 且每个区域只展示匹配的车位。
   */
  applyKeywordFilter() {
    const kw = String(this.data.spotKeyword || '').trim().toLowerCase()
    const all = this.allAreaGroups || []
    if (!kw) {
      this.setData({ areaGroups: all, searchMatched: 0 })
      return
    }
    let matched = 0
    const filtered = all.map((g) => {
      const areaName = String(g.area || '').toLowerCase()
      const spots = (g.spots || []).filter((s) => {
        const code = String(s.spotCode || '').toLowerCase()
        const area = String(s.area || '').toLowerCase()
        return code.indexOf(kw) >= 0 || area.indexOf(kw) >= 0 || areaName.indexOf(kw) >= 0
      })
      matched += spots.length
      return { area: g.area, spots }
    }).filter((g) => g.spots.length > 0)
    this.setData({ areaGroups: filtered, searchMatched: matched })
  },

  /** 点击区域标题旁的三角图标：切换该区域的展开/收起状态 */
  toggleArea(e) {
    const area = e.currentTarget.dataset.area
    if (!area) return
    const map = Object.assign({}, this.data.collapsedAreas)
    // 三态切换：未设置(收起) → 展开(false)；已展开(false) → 收起(true)；已收起(true) → 展开(false)
    map[area] = map[area] === false ? true : false
    this.setData({ collapsedAreas: map })
  }
})
