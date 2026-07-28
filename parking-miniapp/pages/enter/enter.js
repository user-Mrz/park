const app = getApp()

Page({
  data: {
    spots: [],
    selectedSpot: null,
    spotIndex: -1,
    plateNumber: '',
    reservationId: null,  // 预约ID，如果有活动预约则自动关联
    activeReservation: null,
    submitting: false
  },

  onLoad() {
    this.loadSpots()
    this.checkReservation()
  },

  loadSpots() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/spot/list?page=1&pageSize=100',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          // Show vacant (0) and reserved (2) spots; both are valid for entry
          const spots = (res.data.data.records || []).filter(s => s.status === 0 || s.status === 2)
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

  // Check if user has an active reservation to auto-link
  checkReservation() {
    const token = wx.getStorageSync('token')
    if (!token) return
    wx.request({
      url: app.globalData.baseUrl + '/user/reservations?page=1&pageSize=5',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          const reservations = res.data.data.records || []
          // Find the most recent active (status=1) reservation
          const active = reservations.find(r => r.status === 1)
          if (active) {
            this.setData({
              reservationId: active.id,
              activeReservation: active
            })
            // Auto-select the reserved spot if it exists in the spot list
            const spotIdx = this.data.spots.findIndex(s => s.id === active.spotId)
            if (spotIdx >= 0) {
              this.setData({
                spotIndex: spotIdx,
                selectedSpot: this.data.spots[spotIdx]
              })
            }
          }
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
    const reqData = {
      spotId: this.data.selectedSpot.id,
      plateNumber: this.data.plateNumber
    }
    // Attach reservationId if user has an active reservation for this spot
    if (this.data.reservationId && this.data.selectedSpot.id === this.data.activeReservation?.spotId) {
      reqData.reservationId = this.data.reservationId
    }
    wx.request({
      url: app.globalData.baseUrl + '/vehicle/enter',
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      data: reqData,
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
