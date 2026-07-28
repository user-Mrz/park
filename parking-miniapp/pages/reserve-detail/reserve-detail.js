const app = getApp()

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
    if (id) this.loadDetail(id)
    else this.setData({ loading: false })
  },

  async loadDetail(id) {
    const token = wx.getStorageSync('token')
    if (!token) { this.setData({ loading: false }); return }
    this.setData({ loading: true })
    wx.request({
      url: app.globalData.baseUrl + '/reservation/' + id,
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          const r = res.data.data
          this.setData(Object.assign({ reservation: r, loading: false }, this.computeStatus(r.status)))
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
          this.setData({ loading: false })
        }
      },
      fail: () => {
        wx.showToast({ title: '网络异常', icon: 'none' })
        this.setData({ loading: false })
      }
    })
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
    const token = wx.getStorageSync('token')
    this.setData({ cancelling: true })
    wx.request({
      url: app.globalData.baseUrl + '/reservation/cancel/' + id,
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '预约已取消', icon: 'success' })
          const r = this.data.reservation
          r.status = 3
          this.setData(Object.assign({ reservation: r }, this.computeStatus(3)))
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ cancelling: false })
    })
  },

  handleEnter() {
    const r = this.data.reservation
    if (!r) return
    const token = wx.getStorageSync('token')
    if (!token) return

    // 先获取用户信息拿车牌号
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (infoRes) => {
        if (infoRes.data.code !== 200) return
        const user = infoRes.data.data
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
        this.doEnter(r, plateNumber, token)
      }
    })
  },

  doEnter(r, plateNumber, token) {
    this.setData({ entering: true })
    wx.request({
      url: app.globalData.baseUrl + '/vehicle/enter',
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      data: {
        spotId: r.spotId,
        reservationId: r.id,
        plateNumber: plateNumber
      },
      success: (res) => {
        if (res.data.code === 200) {
          r.status = 2
          this.setData(Object.assign({ reservation: r }, this.computeStatus(2)))
          wx.showToast({ title: '入场成功，预约已完成', icon: 'success' })
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ entering: false })
    })
  }
})
