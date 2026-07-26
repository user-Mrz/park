const app = getApp()

Page({
  data: {
    userInfo: null,
    currentParking: null,
    reservations: [],
    spots: [],
    durationText: '--'
  },

  onShow() {
    this.loadData()
  },

  async loadData() {
    const token = wx.getStorageSync('token')
    if (!token) return
    try {
      const infoRes = await this.request('/user/info')
      this.setData({ userInfo: infoRes.data })
      const curRes = await this.request('/vehicle/current')
      this.setData({ currentParking: curRes.data })
      if (curRes.data) {
        this.updateDuration(curRes.data.entryTime)
      }
      const resRes = await this.request('/user/reservations?page=1&pageSize=5')
      this.setData({ reservations: (resRes.data && resRes.data.records) || [] })
      const spotRes = await this.request('/spot/list?page=1&pageSize=20')
      const allSpots = (spotRes.data && spotRes.data.records) || []
      this.setData({ spots: allSpots.filter(s => s.status === 0).slice(0, 10) })
    } catch (e) {}
  },

  updateDuration(entryTime) {
    if (!entryTime) return
    const start = new Date(entryTime.replace(/-/g, '/')).getTime()
    const now = Date.now()
    const diff = Math.floor((now - start) / 1000 / 60)
    let text = ''
    if (diff < 60) text = diff + '分钟'
    else text = Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟'
    this.setData({ durationText: text })
  },

  request(url, method = 'GET', data = {}) {
    return new Promise((resolve, reject) => {
      const token = wx.getStorageSync('token')
      wx.request({
        url: app.globalData.baseUrl + url,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? 'Bearer ' + token : ''
        },
        success(res) {
          if (res.data.code === 200) resolve(res.data)
          else reject(res.data)
        },
        fail: reject
      })
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
    if (!this.data.currentParking) {
      wx.showToast({ title: '当前没有停车记录', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/exit/exit?id=' + this.data.currentParking.id })
  },
  goRecharge() {
    const token = wx.getStorageSync('token')
    if (!token) { wx.navigateTo({ url: '/pages/login/login' }); return }
    wx.navigateTo({ url: '/pages/recharge/recharge' })
  }
})
