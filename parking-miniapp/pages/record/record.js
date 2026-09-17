import request from '../../utils/request.js'

Page({
  data: {
    tab: 0,
    vehicleRecords: [],
    rechargeRecords: [],
    paymentRecords: []
  },

  onLoad(options) {
    if (options && options.tab) {
      var t = parseInt(options.tab)
      if (t >= 0 && t <= 2) this.setData({ tab: t })
    }
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
    if (!wx.getStorageSync('token')) return
    request.get('/user/vehicle-records?page=1&pageSize=50').then((res) => {
      this.setData({ vehicleRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  },

  loadRechargeRecords() {
    if (!wx.getStorageSync('token')) return
    request.get('/user/recharge/records?page=1&pageSize=50').then((res) => {
      this.setData({ rechargeRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  },

  loadPaymentRecords() {
    if (!wx.getStorageSync('token')) return
    request.get('/user/payment/records?page=1&pageSize=50').then((res) => {
      this.setData({ paymentRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  }
})
