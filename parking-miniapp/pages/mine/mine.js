const app = getApp()

Page({
  data: {
    isLoggedIn: false,
    userInfo: null
  },

  onShow() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, userInfo: null })
      return
    }
    // 从后端获取最新余额，不用 localStorage 缓存
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          const info = res.data.data
          this.setData({ isLoggedIn: true, userInfo: info })
          wx.setStorageSync('userInfo', JSON.stringify(info))
        } else if (res.data.code === 401) {
          // 账号被禁用或 token 失效，强制登出
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          this.setData({ isLoggedIn: false, userInfo: null })
          wx.reLaunch({ url: '/pages/login/login' })
        }
      },
      fail: () => {
        // 网络失败时降级用缓存
        const stored = wx.getStorageSync('userInfo')
        if (stored) {
          this.setData({ isLoggedIn: true, userInfo: JSON.parse(stored) })
        }
      }
    })
  },

  goPage(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    const token = wx.getStorageSync('token')
    if (!token && url !== '/pages/login/login') {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    wx.navigateTo({ url })
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          this.setData({ isLoggedIn: false, userInfo: null })
          wx.showToast({ title: '已退出', icon: 'none' })
        }
      }
    })
  }
})
