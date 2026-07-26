const app = getApp()

Page({
  data: {
    spots: [],
    selectedSpot: null,
    spotIndex: -1,
    plateNumber: '',
    submitting: false
  },

  onLoad() {
    this.loadSpots()
  },

  loadSpots() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/spot/list?page=1&pageSize=100',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          const spots = (res.data.data.records || []).filter(s => s.status === 0)
          this.setData({ spots })
        }
      }
    })
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200 && res.data.data.plateNumber) {
          this.setData({ plateNumber: res.data.data.plateNumber })
        }
      }
    })
  },

  onSpotChange(e) {
    const idx = e.detail.value
    this.setData({ spotIndex: idx, selectedSpot: this.data.spots[idx] })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) this.setData({ [field]: e.detail.value })
  },

  handleEnter() {
    if (!this.data.selectedSpot) { wx.showToast({ title: '请选择车位', icon: 'none' }); return }
    if (!this.data.plateNumber) { wx.showToast({ title: '请输入车牌号', icon: 'none' }); return }
    this.setData({ submitting: true })
    const token = wx.getStorageSync('token')
    wx.request({
      url: app.globalData.baseUrl + '/vehicle/enter',
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      data: { spotId: this.data.selectedSpot.id, plateNumber: this.data.plateNumber },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '入场成功', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 1500)
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
