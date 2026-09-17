import request from '../../utils/request.js'

// iOS 兼容的日期解析，兼容空格和 T 分隔符
function parseDateTime(str) {
  if (!str) return new Date()
  var s = str.replace('T', ' ')
  var parts = s.split(' ')[0].split('-')
  var times = (s.split(' ')[1] || '00:00:00').split(':')
  return new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0)
}

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
    this.loadData()
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
        if (r.entryTime) {
          const start = parseDateTime(r.entryTime).getTime()
          const diff = Math.floor((Date.now() - start) / 1000 / 60)
          const hourlyRate = r.spotPricePerHour || 5
          // 实际停车时长费用 (不足1小时按1小时)
          const actualFee = Math.max(1, Math.ceil(diff / 60)) * hourlyRate
          // 若关联预约: 提前离场按预约金额结算 (费用 = max(实际停车费, 预约金额))
          const reservationAmount = r.reservationAmount ? Number(r.reservationAmount) : 0
          const finalFee = reservationAmount > actualFee ? reservationAmount : actualFee
          this.setData({
            durationText: diff < 60 ? diff + '分钟' : Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟',
            estimatedFee: finalFee.toFixed(2),
            settlementNote: reservationAmount > actualFee ? '预约时段锁定，提前离场按预约金额结算' : ''
          })
        }
      }
    }).catch(() => {})
  },

  handleExit() {
    if (!this.data.currentRecord) { wx.showToast({ title: '没有停车记录', icon: 'none' }); return }
    this.setData({ submitting: true })
    // silent: 余额不足时由本页自行弹窗引导充值，避免默认错误提示重复
    request.post('/vehicle/exit/' + this.data.currentRecord.id, {}, true).then(() => {
      request.get('/user/info').then((infoRes) => {
        wx.setStorageSync('userInfo', JSON.stringify(infoRes.data))
      }).catch(() => {})
      wx.showToast({ title: '离场成功，已自动扣费', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 1500)
    }).catch((err) => {
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
