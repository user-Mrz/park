App({
  onLaunch() {
    console.log('App Launch')
    const token = wx.getStorageSync('token')
    if (!token) {
      // Handle login needed
    }
  },

  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://192.168.2.132:8080/api'
  }
})
