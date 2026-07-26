Page({
  data: {
    isLoggedIn: false,
    userInfo: null
  },

  onShow() {
    const token = wx.getStorageSync('token')
    const stored = wx.getStorageSync('userInfo')
    if (token && stored) {
      this.setData({ isLoggedIn: true, userInfo: JSON.parse(stored) })
    } else {
      this.setData({ isLoggedIn: false, userInfo: null })
    }
  },

  goPage(url) {
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
