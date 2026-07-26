const app = getApp()

Page({
  data: {
    userInfo: null,
    amount: 0,
    customAmount: '',
    presetAmounts: [10, 20, 50, 100, 200, 500],
    submitting: false
  },

  onShow() {
    this.loadUser()
  },

  loadUser() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) this.setData({ userInfo: res.data.data })
      }
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) this.setData({ [field]: e.detail.value })
  },

  selectAmount(e) {
    const val = Number(e.currentTarget.dataset.amount)
    this.setData({ amount: val, customAmount: '' })
  },

  handleRecharge() {
    const finalAmount = this.data.customAmount ? Number(this.data.customAmount) : this.data.amount
    if (finalAmount <= 0) { wx.showToast({ title: '请选择或输入金额', icon: 'none' }); return }
    this.setData({ submitting: true })
    const token = wx.getStorageSync('token')
    wx.request({
      url: app.globalData.baseUrl + '/user/recharge',
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      data: { amount: finalAmount, payMethod: 'wechat' },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '充值成功', icon: 'success' })
          this.loadUser()
          setTimeout(() => wx.navigateBack(), 1500)
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
