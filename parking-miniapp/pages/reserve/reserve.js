const app = getApp()

Page({
  data: {
    spots: [],
    spotIndex: -1,
    selectedSpot: null,
    startDate: '',
    startTime: '08:00',
    dateList: [],
    durationIndex: 0,
    durations: [
      { label: '1小时', value: 1 }, { label: '2小时', value: 2 }, { label: '3小时', value: 3 },
      { label: '4小时', value: 4 }, { label: '6小时', value: 6 }, { label: '12小时', value: 12 },
      { label: '1天', value: 24 }, { label: '3天', value: 72 }, { label: '1周', value: 168 },
      { label: '1个月', value: 720 }
    ],
    endTime: '',
    totalFee: 0,
    submitting: false
  },

  onLoad() {
    const dates = []
    const now = new Date()
    for (let i = 0; i < 14; i++) {
      const d = new Date(now)
      d.setDate(d.getDate() + i)
      dates.push(`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`)
    }
    this.setData({ dateList: dates, startDate: dates[0] })
    this.loadSpots()
  },

  async loadSpots() {
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
  },

  onSpotChange(e) {
    const idx = e.detail.value
    this.setData({ spotIndex: idx, selectedSpot: this.data.spots[idx] })
    this.calcFee()
  },
  onDateChange(e) {
    this.setData({ startDate: this.data.dateList[e.detail.value] })
    this.calcFee()
  },
  onTimeChange(e) {
    this.setData({ startTime: e.detail.value + ':00' })
    this.calcFee()
  },
  onDurationChange(e) {
    this.setData({ durationIndex: e.detail.value })
    this.calcFee()
  },

  calcFee() {
    const { selectedSpot, startDate, startTime, durations, durationIndex } = this.data
    if (!selectedSpot || !startDate) return
    const dur = durations[durationIndex]
    const start = new Date((startDate + 'T' + startTime).replace(/-/g, '/')).getTime()
    const end = new Date(start + dur.value * 3600000)
    const endStr = `${end.getFullYear()}-${String(end.getMonth()+1).padStart(2,'0')}-${String(end.getDate()).padStart(2,'0')} ${String(end.getHours()).padStart(2,'0')}:00`
    this.setData({ endTime: endStr, totalFee: (selectedSpot.pricePerHour * dur.value).toFixed(2) })
  },

  async handleReserve() {
    const { selectedSpot, startDate, startTime, endTime, durations, durationIndex } = this.data
    if (!selectedSpot) { wx.showToast({ title: '请选择车位', icon: 'none' }); return }
    this.setData({ submitting: true })
    const token = wx.getStorageSync('token')
    const dur = durations[durationIndex]
    const startDt = startDate + ' ' + startTime
    wx.request({
      url: app.globalData.baseUrl + '/reservation/create',
      method: 'POST',
      header: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' },
      data: {
        spotId: selectedSpot.id,
        startTime: startDt,
        endTime: endTime
      },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '预约成功', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 1500)
        } else {
          wx.showToast({ title: res.data.message, icon: 'none' })
        }
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
