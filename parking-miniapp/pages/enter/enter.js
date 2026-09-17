import request from '../../utils/request.js'

Page({
  data: {
    spots: [],
    selectedSpot: null,
    spotIndex: -1,
    plateNumber: '',
    reservationId: null,  // 预约ID，如果有活动预约则自动关联
    activeReservation: null,
    activeSpotCode: '',
    submitting: false
  },

  onLoad() {
    this.loadPageData()
  },

  // 一次性并行加载: 车位列表 + 我的预约 + 用户信息(避免竞态导致未自动选中)
  loadPageData() {
    if (!wx.getStorageSync('token')) return
    Promise.all([
      request.get('/spot/list?page=1&pageSize=500'),
      request.get('/user/reservations?page=1&pageSize=10'),
      request.get('/user/info')
    ]).then(([spotRes, resvRes, infoRes]) => {
      const user = infoRes.data || {}
      const reservations = (resvRes.data && resvRes.data.records) || []
      // 取最近一条活跃预约(status=1)
      const active = reservations.find(r => r.status === 1)

      // 仅展示: 空闲车位(0) + 本人活跃预约占用的车位(2)。
      // 他人已预约的车位不再展示, 避免选中后入场被后端拒绝("reserved by another user")。
      const spots = ((spotRes.data && spotRes.data.records) || []).filter((s) => {
        if (s.status === 0) return true
        return s.status === 2 && active && String(s.id) === String(active.spotId)
      })

      this.setData({
        spots,
        plateNumber: user.plateNumber || '',
        reservationId: active ? active.id : null,
        activeReservation: active || null,
        activeSpotCode: ''
      })

      // 若有本人活跃预约, 自动选中并关联对应车位
      if (active) {
        const idx = spots.findIndex((s) => String(s.id) === String(active.spotId))
        if (idx >= 0) {
          this.setData({
            spotIndex: idx,
            selectedSpot: spots[idx],
            activeSpotCode: spots[idx].spotCode
          })
        }
      }
    }).catch(() => {})
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
    const reqData = {
      spotId: this.data.selectedSpot.id,
      plateNumber: this.data.plateNumber
    }
    // Attach reservationId if user has an active reservation for this spot
    if (this.data.reservationId && this.data.selectedSpot.id === this.data.activeReservation?.spotId) {
      reqData.reservationId = this.data.reservationId
    }
    request.post('/vehicle/enter', reqData).then(() => {
      wx.showToast({ title: '入场成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => {}).finally(() => this.setData({ submitting: false }))
  }
})
