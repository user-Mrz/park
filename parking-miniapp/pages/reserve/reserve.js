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
    submitting: false,
    loading: false,
    errorMsg: ''
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

  loadSpots() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ errorMsg: '请先登录', loading: false })
      wx.showModal({
        title: '提示',
        content: '请先登录后再预约车位',
        confirmText: '去登录',
        success(res) {
          if (res.confirm) wx.navigateTo({ url: '/pages/login/login' })
        }
      })
      return
    }
    this.setData({ loading: true, errorMsg: '' })
    wx.request({
      url: app.globalData.baseUrl + '/spot/list?page=1&pageSize=100',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          // Show vacant (0) and reserved (2) spots; both can be reserved
          const spots = (res.data.data.records || []).filter(s => s.status === 0)
          this.setData({ spots, loading: false })
          if (spots.length === 0) {
            this.setData({ errorMsg: '暂无可预约车位' })
          }
        } else {
          const msg = (res.data && res.data.message) || '加载车位失败'
          this.setData({ errorMsg: msg, loading: false })
          wx.showToast({ title: msg, icon: 'none' })
        }
      },
      fail: (err) => {
        this.setData({ loading: false, errorMsg: '网络异常，请检查网络连接' })
        wx.showToast({ title: '网络异常，请检查网络连接', icon: 'none', duration: 2000 })
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
    this.setData({ startTime: e.detail.value })
    this.calcFee()
  },
  onDurationChange(e) {
    this.setData({ durationIndex: e.detail.value })
    this.calcFee()
  },

  calcFee() {
    const { selectedSpot, startDate, startTime, durations, durationIndex } = this.data
    if (!selectedSpot || !startDate || !startTime) return
    const dur = durations[durationIndex]
    // 用构造函数避免 iOS 日期字符串解析兼容问题
    const dateParts = startDate.split('-')
    const timeParts = startTime.split(':')
    const start = new Date(+dateParts[0], +dateParts[1] - 1, +dateParts[2], +timeParts[0], +timeParts[1], 0).getTime()
    const end = new Date(start + dur.value * 3600000)
    const pad = (n) => String(n).padStart(2, '0')
    const endStr = `${end.getFullYear()}-${pad(end.getMonth()+1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:00`
    this.setData({ endTime: endStr, totalFee: (selectedSpot.pricePerHour * dur.value).toFixed(2) })
  },

  handleReserve() {
    const { selectedSpot, startDate, startTime, endTime, durations, durationIndex } = this.data
    if (!selectedSpot) { wx.showToast({ title: '请选择车位', icon: 'none' }); return }
    if (!endTime) { wx.showToast({ title: '请选择预约时长', icon: 'none' }); return }
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    const dur = durations[durationIndex]
    const startDt = startDate + ' ' + startTime + ':00'
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
      fail: () => {
        wx.showToast({ title: '网络异常，请重试', icon: 'none' })
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
