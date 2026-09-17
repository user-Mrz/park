import request from '../../utils/request.js'

const TYPE_LABEL = {
  overtime: '超时占用',
  illegal_zone: '违规区域',
  occupied_others: '占用他人车位'
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
  },

  loadList() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, loading: false })
      return
    }
    this.setData({ isLoggedIn: true, loading: true })
    request.get('/vehicle/violations?page=1&pageSize=30', {}, true).then((res) => {
      const rows = ((res.data && res.data.records) || []).map((v) => ({
        ...v,
        typeLabel: TYPE_LABEL[v.type] || v.type,
        handledText: v.status === 1 ? '已处理' : '待处理',
        handledCls: v.status === 1 ? 'done' : 'pending',
        timeText: fmtTime(v.createdAt)
      }))
      this.setData({ list: rows, loading: false })
    }).catch(() => this.setData({ loading: false }))
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
