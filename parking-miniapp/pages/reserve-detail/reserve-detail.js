import request from '../../utils/request.js'
import { createPoller } from '../../utils/poller.js'
import { requestAndReport, SubscribeScenes } from '../../utils/subscribe.js'

Page({
  data: {
    reservation: null,
    loading: true,
    entering: false,
    cancelling: false,
    statusLabel: '',
    statusIcon: ''
  },

  statusMap: {
    text: { 0: '待确认', 1: '进行中', 2: '已完成', 3: '已取消', 4: '已过期' },
    icon: { 0: '⏳', 1: '✅', 2: '🏁', 3: '❌', 4: '⏰' }
  },

  computeStatus(s) {
    return {
      statusLabel: (this.statusMap.text[s]) || '未知',
      statusIcon: (this.statusMap.icon[s]) || '❓'
    }
  },

  onLoad(options) {
    const id = options.id
    this.reservationId = id
    if (id) this.loadDetail(id)
    else this.setData({ loading: false })
  },

  onShow() {
    // 管理员取消/车位变动后，本页自动同步（10 秒一次）
    this.startSync()
  },

  onHide() {
    this.stopSync()
  },

  onUnload() {
    this.stopSync()
  },

  startSync() {
    this.stopSync()
    if (!this.reservationId) return
    this.detailPoller = createPoller(() => this.loadDetail(this.reservationId, true), 10000)
    this.detailPoller.start()
  },

  stopSync() {
    if (this.detailPoller) {
      this.detailPoller.stop()
      this.detailPoller = null
    }
  },

  async loadDetail(id, silent) {
    if (!wx.getStorageSync('token')) { this.setData({ loading: false }); return }
    if (!silent) this.setData({ loading: true })
    request.get('/reservation/' + id, {}, silent).then((res) => {
      const r = res.data
      this.setData(Object.assign({ reservation: r, loading: false }, this.computeStatus(r.status)))
      // 预约前 15 分钟提醒：状态活跃 + 开始时间在 0~30 分钟内
      if (r && Number(r.status) === 1 && r.startTime) {
        this.tryReserveRemindSubscribe(r)
      }
    }).catch(() => {
      if (!silent) this.setData({ loading: false })
    })
  },

  /** 判断预约是否在 0~30 分钟内开始，触发订阅提醒（尽力而为） */
  tryReserveRemindSubscribe(r) {
    try {
      const startMs = new Date(r.startTime.replace(/-/g, '/')).getTime()
      const now = Date.now()
      const diffMin = (startMs - now) / 60000
      if (diffMin > 0 && diffMin <= 30) {
        requestAndReport(SubscribeScenes.SCENE_RESERVE_REMIND, r.reservationNo || String(r.id))
      }
    } catch (e) { /* 时间解析失败忽略 */ }
  },

  goBack() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  handleCancel() {
    const r = this.data.reservation
    if (!r) return
    wx.showModal({
      title: '确认取消',
      content: '确定要取消这个预约吗？',
      confirmColor: '#F56C6C',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        this.doCancel(r.id)
      }
    })
  },

  doCancel(id) {
    this.setData({ cancelling: true })
    request.post('/reservation/cancel/' + id).then(() => {
      wx.showToast({ title: '预约已取消', icon: 'success' })
      const r = this.data.reservation
      r.status = 3
      this.setData(Object.assign({ reservation: r }, this.computeStatus(3)))
    }).catch(() => {}).finally(() => this.setData({ cancelling: false }))
  },

  handleEnter() {
    const r = this.data.reservation
    if (!r) return
    if (!wx.getStorageSync('token')) return

    // 先获取用户信息拿车牌号
    request.get('/user/info').then((infoRes) => {
      const user = infoRes.data
      const plateNumber = user.plateNumber || ''
      if (!plateNumber) {
        wx.showModal({
          title: '提示',
          content: '请先在个人中心设置车牌号',
          confirmText: '去设置',
          success(modalRes) {
            if (modalRes.confirm) wx.switchTab({ url: '/pages/mine/mine' })
          }
        })
        return
      }
      this.doEnter(r, plateNumber)
    }).catch(() => {})
  },

  doEnter(r, plateNumber) {
    this.setData({ entering: true })
    request.post('/vehicle/enter', {
      spotId: r.spotId,
      reservationId: r.id,
      plateNumber: plateNumber
    }).then(() => {
      r.status = 2
      this.setData(Object.assign({ reservation: r }, this.computeStatus(2)))
      wx.showToast({ title: '入场成功，预约已完成', icon: 'success' })
    }).catch(() => {}).finally(() => this.setData({ entering: false }))
  }
})
