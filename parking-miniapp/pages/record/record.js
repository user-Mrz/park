const app = getApp()

Page({
  data: {
    tab: 0,
    vehicleRecords: [],
    rechargeRecords: [],
    paymentRecords: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    if (this.data.tab === 0) this.loadVehicleRecords()
    else if (this.data.tab === 1) this.loadRechargeRecords()
    else this.loadPaymentRecords()
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab) })
    this.loadData()
  },

  loadVehicleRecords() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/vehicle-records?page=1&pageSize=50',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) this.setData({ vehicleRecords: (res.data.data && res.data.data.records) || [] })
      }
    })
  },

  loadRechargeRecords() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/recharge/records?page=1&pageSize=50',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) this.setData({ rechargeRecords: (res.data.data && res.data.data.records) || [] })
      }
    })
  },

  loadPaymentRecords() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/payment/records?page=1&pageSize=50',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) this.setData({ paymentRecords: (res.data.data && res.data.data.records) || [] })
      }
    })
  }
})
