const app = getApp()

// iOS 兼容日期解析，兼容空格和 T 分隔符
function parseDateTime(str) {
  if (!str) return null
  // 将 T 替换为空格，统一处理
  str = str.replace('T', ' ')
  var parts = str.split(' ')[0].split('-')
  var times = (str.split(' ')[1] || '00:00:00').split(':')
  return new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0)
}

function formatDuration(entryTime) {
  if (!entryTime) return '--'
  var start = parseDateTime(entryTime).getTime()
  const now = Date.now()
  const diff = Math.floor((now - start) / 1000 / 60)
  if (diff < 60) return diff + '分钟'
  return Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟'
}

Page({
  data: {
    userInfo: null,
    parkedVehicles: [],
    activeReservations: [],
    areaGroups: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    var self = this
    var token = wx.getStorageSync('token')
    // 用户信息（需登录）
    if (token) {
      wx.request({
        url: app.globalData.baseUrl + '/user/info',
        header: { 'Authorization': 'Bearer ' + token },
        success: function(res) {
          if (res.data.code === 200) {
            self.setData({ userInfo: res.data.data })
          } else if (res.data.code === 401) {
            // 账号被禁用或 token 失效
            wx.removeStorageSync('token')
            wx.removeStorageSync('userInfo')
            self.setData({ userInfo: null })
            wx.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
    // 公开概览，失败则回退用 /spot/list
    wx.request({
      url: app.globalData.baseUrl + '/spot/overview',
      header: token ? { 'Authorization': 'Bearer ' + token } : {},
      success: function(res) {
        if (res.data.code === 200) {
          var data = res.data.data
          var vehicles = (data.parkedVehicles || []).map(function(pv) {
            pv.durationText = formatDuration(pv.entryTime)
            return pv
          })
          self.setData({
            parkedVehicles: vehicles,
            activeReservations: data.activeReservations || [],
            areaGroups: data.areaGroups || []
          })
        } else {
          self.loadSpotsFallback()
        }
      },
      fail: function() { self.loadSpotsFallback() }
    })
  },

  goReserve() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/reserve/reserve' })
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

  // 概览接口失败时的回退方案
  loadSpotsFallback() {
    var self = this
    wx.request({
      url: app.globalData.baseUrl + '/spot/list?page=1&pageSize=500',
      header: {},
      success: function(res) {
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
          self.setData({ areaGroups: areaGroups })
        }
      }
    })
  }
})
