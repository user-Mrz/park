import request from '../../utils/request.js'
import { elapsedMinutes, formatDuration, createTicker, clearTicker } from '../../utils/duration.js'
import { createPoller } from '../../utils/poller.js'

Page({
  data: {
    currentRecord: null,
    userInfo: null,
    durationText: '--',
    estimatedFee: 0,
    settlementNote: '',
    submitting: false
  },

  onLoad(options) {
    this.recordId = options?.id
  },

  onShow() {
    this.loadData()
    // 已停时长与预计费用会随时间变化，页面停留期间自动走字，无需手动刷新
    this.startLiveTicker()
    // 与后端同步：管理员在后台强制结算后，本页自动感知
    this.startSync()
  },

  onHide() {
    this.stopLiveTicker()
    this.stopSync()
  },

  onUnload() {
    this.stopLiveTicker()
    this.stopSync()
  },

  /** 每 5 秒同步一次当前停车记录 */
  startSync() {
    this.stopSync()
    this.syncPoller = createPoller(() => this.syncCurrent(), 5000)
    this.syncPoller.start()
  },

  stopSync() {
    if (this.syncPoller) {
      this.syncPoller.stop()
      this.syncPoller = null
    }
  },

  /**
   * 轮询当前停车记录：
   * 若记录已消失说明管理员（或其它终端）已完成出场结算，本页立即切换为空状态并提示用户
   */
  syncCurrent() {
    if (!wx.getStorageSync('token')) return Promise.resolve()
    const self = this
    return new Promise((resolve) => {
      request.get('/vehicle/current', {}, true).then((res) => {
        const record = res.data
        const hadRecord = !!self.data.currentRecord
        if (hadRecord && !record) {
          self.stopSync()
          self.stopLiveTicker()
          self.setData({ currentRecord: null, durationText: '--', estimatedFee: 0, settlementNote: '' })
          // 结算会扣余额，同步一次账户信息
          request.get('/user/info', {}, true).then((info) => {
            self.setData({ userInfo: info.data })
          }).catch(() => {})
          if (self.exiting) {
            // 用户自己点的离场，正常流程里已有成功提示，不再重复弹窗
            resolve()
            return
          }
          wx.showModal({
            title: '车辆已出场',
            content: '您的车辆已由管理员完成出场结算，费用已自动从余额扣除，可在「我的 → 停车记录」中查看。',
            showCancel: false,
            confirmText: '知道了',
            success: () => { wx.navigateBack() }
          })
        } else if (record) {
          self.setData({ currentRecord: record })
          self.computeLive()
        }
        resolve()
      }).catch(() => resolve())
    })
  },

  startLiveTicker() {
    this.stopLiveTicker()
    this.liveTimer = createTicker(() => this.computeLive(), 1000)
  },

  stopLiveTicker() {
    clearTicker(this.liveTimer)
    this.liveTimer = null
  },

  loadData() {
    if (!wx.getStorageSync('token')) return
    request.get('/user/info').then((res) => {
      this.setData({ userInfo: res.data })
    }).catch(() => {})
    request.get('/vehicle/current').then((res) => {
      if (res.data) {
        const r = res.data
        if (this.recordId && String(r.id) !== String(this.recordId)) {
          wx.showToast({ title: '停车记录不匹配', icon: 'none' })
          return
        }
        this.setData({ currentRecord: r })
        this.computeLive()
      }
    }).catch(() => {})
  },

  /**
   * 根据当前在场记录重算"已停时长 / 预计费用 / 结算说明"
   * 每秒调用一次，但只有内容变化时才 setData
   */
  computeLive() {
    const r = this.data.currentRecord
    if (!r || !r.entryTime) return
    const diff = elapsedMinutes(r.entryTime)
    const hourlyRate = r.spotPricePerHour || 5
    // 实际停车时长费用 (不足1小时按1小时)
    const actualFee = Math.max(1, Math.ceil(diff / 60)) * hourlyRate
    // 若关联预约: 提前离场按预约金额结算 (费用 = max(实际停车费, 预约金额))
    const reservationAmount = r.reservationAmount ? Number(r.reservationAmount) : 0
    const finalFee = reservationAmount > actualFee ? reservationAmount : actualFee

    const durationText = formatDuration(r.entryTime)
    const estimatedFee = finalFee.toFixed(2)
    const settlementNote = reservationAmount > actualFee ? '预约时段锁定，提前离场按预约金额结算' : ''

    if (durationText !== this.data.durationText
      || estimatedFee !== this.data.estimatedFee
      || settlementNote !== this.data.settlementNote) {
      this.setData({ durationText, estimatedFee, settlementNote })
    }
  },

  handleExit() {
    if (!this.data.currentRecord) { wx.showToast({ title: '没有停车记录', icon: 'none' }); return }
    this.exiting = true
    this.setData({ submitting: true })
    // silent: 余额不足时由本页自行弹窗引导充值，避免默认错误提示重复
    request.post('/vehicle/exit/' + this.data.currentRecord.id, {}, true).then(() => {
      request.get('/user/info').then((infoRes) => {
        wx.setStorageSync('userInfo', JSON.stringify(infoRes.data))
      }).catch(() => {})
      wx.showToast({ title: '离场成功，已自动扣费', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 1500)
    }).catch((err) => {
      // 离场失败（例如余额不足）：恢复轮询提示能力
      this.exiting = false
      if (err && err.message && err.message.includes('Insufficient balance')) {
        wx.showModal({
          title: '余额不足',
          content: err.message + '，是否前往充值？',
          confirmText: '去充值',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              wx.navigateTo({ url: '/pages/recharge/recharge' })
            }
          }
        })
      } else if (err && err.message) {
        wx.showToast({ title: err.message, icon: 'none' })
      }
    }).finally(() => this.setData({ submitting: false }))
  }
})