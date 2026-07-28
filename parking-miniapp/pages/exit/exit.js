const app = getApp()

// iOS 兼容的日期解析，兼容空格和 T 分隔符
function parseDateTime(str) {
  if (!str) return new Date()
  var s = str.replace('T', ' ')
  var parts = s.split(' ')[0].split('-')
  var times = (s.split(' ')[1] || '00:00:00').split(':')
  return new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0)
}

Page({
  data: {
    currentRecord: null,
    userInfo: null,
    durationText: '--',
    estimatedFee: 0,
    submitting: false
  },

  onLoad(options) {
    this.recordId = options?.id
    this.loadData()
  },

  loadData() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) this.setData({ userInfo: res.data.data })
      }
    })
    wx.request({
      url: app.globalData.baseUrl + '/vehicle/current',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200 && res.data.data) {
          const r = res.data.data
          if (this.recordId && String(r.id) !== String(this.recordId)) {
            wx.showToast({ title: '停车记录不匹配', icon: 'none' })
            return
          }
          this.setData({ currentRecord: r })
          if (r.entryTime) {
            const start = parseDateTime(r.entryTime).getTime()
            const diff = Math.floor((Date.now() - start) / 1000 / 60)
            const hourlyRate = r.spotPricePerHour || 5
            this.setData({
              durationText: diff < 60 ? diff + '分钟' : Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟',
              estimatedFee: (Math.ceil(diff / 60) * hourlyRate).toFixed(2)
            })
          }
        }
      }
    })
  },

  handleExit() {
    if (!this.data.currentRecord) { wx.showToast({ title: '没有停车记录', icon: 'none' }); return }
    this.setData({ submitting: true })
    const token = wx.getStorageSync('token')
    wx.request({
      url: app.globalData.baseUrl + '/vehicle/exit/' + this.data.currentRecord.id,
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          wx.request({
            url: app.globalData.baseUrl + '/user/info',
            header: { 'Authorization': 'Bearer ' + token },
            success: (infoRes) => {
              if (infoRes.data.code === 200) {
                wx.setStorageSync('userInfo', JSON.stringify(infoRes.data.data))
              }
            }
          })
          wx.showToast({ title: '离场成功，已自动扣费', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 1500)
        } else {
          if (res.data.message && res.data.message.includes('Insufficient balance')) {
            wx.showModal({
              title: '余额不足',
              content: res.data.message + '，是否前往充值？',
              confirmText: '去充值',
              cancelText: '取消',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  wx.navigateTo({ url: '/pages/recharge/recharge' })
                }
              }
            })
          } else {
            wx.showToast({ title: res.data.message, icon: 'none' })
          }
        }
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
