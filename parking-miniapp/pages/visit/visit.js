import request from '../../utils/request.js'
import { buildDateList, buildTimeSlots, startTimestamp, fmtDate } from '../../utils/timeSlot.js'

const PLATE_RE = /^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/
const STATUS_META = {
  0: { text: '待审核', cls: 'pending' },
  1: { text: '已通过·待入场', cls: 'ok' },
  2: { text: '已驳回', cls: 'error' },
  3: { text: '已入场', cls: 'ok' },
  4: { text: '已完成', cls: 'done' },
  5: { text: '已过期', cls: 'done' },
  6: { text: '已撤销', cls: 'done' }
}

const pad = (n) => String(n).padStart(2, '0')

Page({
  data: {
    visitTypes: ['访客来访', '家长接送'],
    visitTypeIndex: 1,
    visitorName: '',
    plateNumber: '',
    purpose: '',
    dateList: [],
    dateIndex: 0,
    startDate: '',
    timeList: [],
    timeIndex: 0,
    startTime: '',
    durations: [
      { label: '30 分钟', value: 0.5 },
      { label: '1 小时', value: 1 },
      { label: '2 小时', value: 2 },
      { label: '4 小时', value: 4 },
      { label: '8 小时', value: 8 }
    ],
    durationIndex: 1,
    endText: '',
    reports: [],
    loadingList: false,
    submitting: false,
    parentFreeMinutes: 30,
    isLoggedIn: false,
    canReport: false
  },

  onLoad() {
    const now = new Date()
    const dates = buildDateList(7, now)
    this.setData({ dateList: dates })
    this.applyDate(0)
    this.loadMeta()
    this.loadReports()
  },

  onShow() {
    this.loadReports()
  },

  applyDate(index) {
    const date = this.data.dateList[index]
    const slots = buildTimeSlots(date, new Date())
    this.setData({
      dateIndex: index,
      startDate: date,
      timeList: slots,
      timeIndex: 0,
      startTime: slots[0] || ''
    })
    this.calcEnd()
  },

  loadMeta() {
    const token = wx.getStorageSync('token')
    this.setData({ isLoggedIn: !!token })
    if (!token) return
    request.get('/user/benefits', {}, true).then((res) => {
      const b = res.data || {}
      this.setData({
        // 报备对身份验证状态放宽：只要登录就可以提交，审核由管理员进行
        canReport: true,
        parentFreeMinutes: Number(b.freeMinutes) || 30
      })
    }).catch(() => {
      // 接口失败时也允许报备（不阻塞）
      this.setData({ canReport: true, parentFreeMinutes: 30 })
    })
  },

  loadReports() {
    const token = wx.getStorageSync('token')
    if (!token) return
    this.setData({ loadingList: true })
    request.get('/user/visit-reports?page=1&pageSize=20', {}, true).then((res) => {
      const rows = ((res.data && res.data.records) || []).map((r) => ({
        ...r,
        typeLabel: r.visitType === 2 ? '家长接送' : '访客',
        statusText: (STATUS_META[r.status] || { text: '未知' }).text,
        statusCls: (STATUS_META[r.status] || { cls: 'done' }).cls,
        rangeText: `${String(r.expectedStart || '').replace('T', ' ').slice(5, 16)} ~ ${String(r.expectedEnd || '').replace('T', ' ').slice(5, 16)}`,
        canCancel: r.status === 0 || r.status === 1
      }))
      this.setData({ reports: rows, loadingList: false })
    }).catch(() => this.setData({ loadingList: false }))
  },

  onTypeChange(e) { this.setData({ visitTypeIndex: Number(e.detail.value) }) },
  onNameInput(e) { this.setData({ visitorName: e.detail.value }) },
  onPlateInput(e) { this.setData({ plateNumber: (e.detail.value || '').toUpperCase() }) },
  onPurposeInput(e) { this.setData({ purpose: e.detail.value }) },
  onDateChange(e) { this.applyDate(Number(e.detail.value)) },
  onTimeChange(e) {
    this.setData({ timeIndex: Number(e.detail.value), startTime: this.data.timeList[Number(e.detail.value)] || '' })
    this.calcEnd()
  },
  onDurationChange(e) {
    this.setData({ durationIndex: Number(e.detail.value) })
    this.calcEnd()
  },

  calcEnd() {
    const { startDate, startTime, durations, durationIndex } = this.data
    const ts = startTimestamp(startDate, startTime)
    if (!ts) return this.setData({ endText: '' })
    const end = new Date(ts + durations[durationIndex].value * 3600000)
    this.setData({
      endText: `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}`
    })
  },

  submit() {
    if (this.data.submitting) return
    const { visitTypeIndex, visitorName, plateNumber, purpose, startDate, startTime, endText } = this.data
    if (!plateNumber || !PLATE_RE.test(plateNumber)) {
      return wx.showToast({ title: '请输入正确车牌号，如 粤B12345', icon: 'none' })
    }
    if (!startTime || !endText) return wx.showToast({ title: '请选择到校时间与时长', icon: 'none' })
    const startTs = startTimestamp(startDate, startTime)
    if (startTs <= Date.now()) {
      wx.showToast({ title: '到校时间不能早于当前时间', icon: 'none' })
      return this.applyDate(this.data.dateIndex)
    }

    this.setData({ submitting: true })
    request.post('/user/visit-report', {
      visitType: visitTypeIndex + 1,
      visitorName,
      plateNumber,
      purpose,
      expectedStart: `${startDate} ${startTime}:00`,
      expectedEnd: `${endText}:00`
    }).then(() => {
      this.setData({ submitting: false, visitorName: '', plateNumber: '', purpose: '' })
      wx.showToast({ title: '已提交，等待审核', icon: 'success' })
      this.loadReports()
    }).catch(() => this.setData({ submitting: false }))
  },

  cancelReport(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '撤销报备',
      content: '确定撤销该车辆报备？',
      success: (r) => {
        if (!r.confirm) return
        request.post(`/user/visit-report/${id}/cancel`, {}).then(() => {
          wx.showToast({ title: '已撤销', icon: 'none' })
          this.loadReports()
        }).catch(() => {})
      }
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
