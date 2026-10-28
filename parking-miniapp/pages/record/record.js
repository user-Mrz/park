import request from '../../utils/request.js'
import { createPoller } from '../../utils/poller.js'

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
    this.startSync()
  },

  onHide() {
    this.stopSync()
  },

  onUnload() {
    this.stopSync()
  },

  /** 每 10 秒同步一次：管理员结算后新的停车/扣费记录自动出现 */
  startSync() {
    this.stopSync()
    this.recordPoller = createPoller(() => this.loadData(true), 10000)
    this.recordPoller.start()
  },

  stopSync() {
    if (this.recordPoller) {
      this.recordPoller.stop()
      this.recordPoller = null
    }
  },

  loadData(silent) {
    if (this.data.tab === 0) return this.loadVehicleRecords(silent)
    if (this.data.tab === 1) return this.loadRechargeRecords(silent)
    return this.loadPaymentRecords(silent)
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab) })
    this.loadData()
  },

  loadVehicleRecords(silent) {
    if (!wx.getStorageSync('token')) return Promise.resolve()
    return request.get('/user/vehicle-records?page=1&pageSize=50', {}, silent).then((res) => {
      this.setData({ vehicleRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  },

  loadRechargeRecords(silent) {
    if (!wx.getStorageSync('token')) return Promise.resolve()
    return request.get('/user/recharge/records?page=1&pageSize=50', {}, silent).then((res) => {
      this.setData({ rechargeRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  },

  loadPaymentRecords(silent) {
    if (!wx.getStorageSync('token')) return Promise.resolve()
    return request.get('/user/payment/records?page=1&pageSize=50', {}, silent).then((res) => {
      this.setData({ paymentRecords: (res.data && res.data.records) || [] })
    }).catch(() => {})
  }
})
