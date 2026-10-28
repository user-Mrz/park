import request from '../../utils/request.js'
import { createPoller } from '../../utils/poller.js'

const STATUS_META = {
  0: { text: '待支付', cls: 'pending' },
  1: { text: '进行中', cls: 'ok' },
  2: { text: '已完成', cls: 'done' },
  3: { text: '已取消', cls: 'done' },
  4: { text: '已过期', cls: 'error' }
}

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

Page({
  data: {
    loading: true,
    isLoggedIn: false,
    list: []
  },

  onShow() {
    this.loadList()
    this.startSync()
  },

  onHide() {
    this.stopSync()
  },

  onUnload() {
    this.stopSync()
  },

  /** 每 10 秒同步一次：管理员取消/过期预约后本页自动更新 */
  startSync() {
    this.stopSync()
    this.listPoller = createPoller(() => this.loadList(true), 10000)
    this.listPoller.start()
  },

  stopSync() {
    if (this.listPoller) {
      this.listPoller.stop()
      this.listPoller = null
    }
  },

  loadList(silent) {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, loading: false })
      return Promise.resolve()
    }
    if (!silent) this.setData({ isLoggedIn: true, loading: true })
    return request.get('/user/reservations?page=1&pageSize=30', {}, true).then((res) => {
      const rows = ((res.data && res.data.records) || []).map((r) => {
        const meta = STATUS_META[r.status] || { text: '未知', cls: 'done' }
        return {
          ...r,
          statusText: meta.text,
          statusCls: meta.cls,
          rangeText: `${fmtTime(r.startTime)} ~ ${fmtTime(r.endTime)}`,
          canCancel: r.status === 1
        }
      })
      this.setData({ list: rows, loading: false })
    }).catch(() => this.setData({ loading: false }))
  },

  cancelReserve(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消预约',
      content: '确定取消该预约？取消后车位将被释放。',
      success: (r) => {
        if (!r.confirm) return
        request.post(`/reservation/cancel/${id}`, {}).then(() => {
          wx.showToast({ title: '已取消预约', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/reserve-detail/reserve-detail?id=${id}` })
  },

  goEnter() {
    wx.navigateTo({ url: '/pages/enter/enter' })
  },

  goReserve() {
    wx.navigateTo({ url: '/pages/reserve/reserve' })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
