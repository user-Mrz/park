const app = getApp()

Page({
  data: {
    currentRecord: null,
    userInfo: null,
    durationText: '--',
    estimatedFee: 0,
    submitting: false
  },

  onLoad() {
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
          this.setData({ currentRecord: r })
          if (r.entryTime) {
            const start = new Date(r.entryTime.replace(/-/g, '/')).getTime()
            const diff = Math.floor((Date.now() - start) / 1000 / 60)
            this.setData({
              durationText: diff < 60 ? diff + '分钟' : Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟',
              estimatedFee: (Math.ceil(diff / 60) * 5).toFixed(2)
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
          wx.showToast({ title: '离场成功，已自动扣费', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 1500)
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
