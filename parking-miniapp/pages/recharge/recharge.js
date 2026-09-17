import request from '../../utils/request.js'

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
    if (!wx.getStorageSync('token')) return
    request.get('/user/info').then((res) => {
      const info = res.data
      this.setData({ userInfo: info })
      wx.setStorageSync('userInfo', JSON.stringify(info))
    }).catch(() => {})
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
    request.post('/user/recharge', { amount: finalAmount, payMethod: 'wechat' }).then(() => {
      wx.showToast({ title: '充值成功', icon: 'success' })
      this.loadUser()
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => {}).finally(() => this.setData({ submitting: false }))
  }
})
