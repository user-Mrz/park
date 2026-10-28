import request from '../../utils/request.js'
import { requestAndReport, SubscribeScenes } from '../../utils/subscribe.js'

const PASS_TYPES = [
  { key: 1, label: '月卡 30 天', labelShort: '月卡', months: 1 },
  { key: 2, label: '季卡 90 天', labelShort: '季卡', months: 3 },
  { key: 3, label: '年卡 365 天', labelShort: '年卡', months: 12 }
]

function formatDate(d) {
  if (!d) return '-'
  return String(d).slice(0, 10)
}

/** 距到期日还有多少天（按自然日计算，今天为 0） */
function daysUntil(dateStr) {
  if (!dateStr) return null
  const parts = String(dateStr).slice(0, 10).split('-').map(Number)
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null
  const target = new Date(parts[0], parts[1] - 1, parts[2])
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target.getTime() - today.getTime()) / 86400000)
}

Page({
  data: {
    loading: true,
    prices: { monthly: '199.00', quarterly: '499.00', yearly: '1599.00' },
    passTypes: PASS_TYPES,
    selectedType: 1,
    currentPass: null,
    history: [],
    balance: '0.00',
    openPassing: false,
    plateNumber: ''
  },

  onShow() {
    this.loadAll()
  },

  async loadAll() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ loading: true })
    try {
      const [prices, current, history, userInfo] = await Promise.all([
        request.get('/user/monthly-pass/prices', {}, true).then((r) => r.data || {}).catch(() => ({})),
        request.get('/user/monthly-pass/current', {}, true).then((r) => r.data).catch(() => null),
        request.get('/user/monthly-pass/list', {}, true).then((r) => (r.data && r.data.records) || []).catch(() => []),
        request.get('/user/info', {}, true).then((r) => r.data || {}).catch(() => ({}))
      ])
      this.setData({
        prices, currentPass: current,
        history: (history || []).map((h) => ({
          ...h,
          startDate: formatDate(h.startDate),
          expireDate: formatDate(h.expireDate)
        })),
        plateNumber: userInfo.plateNumber || '',
        balance: Number(userInfo.balance || 0).toFixed(2),
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onTypeTap(e) {
    const t = Number(e.currentTarget.dataset.type)
    this.setData({ selectedType: t })
  },

  getPrice(type) {
    const p = this.data.prices
    if (type === 1) return p.monthly || '199.00'
    if (type === 2) return p.quarterly || '499.00'
    if (type === 3) return p.yearly || '1599.00'
    return '0.00'
  },

  async handleOpen() {
    if (this.data.openPassing) return
    if (!this.data.plateNumber) {
      wx.showModal({
        title: '请先绑定车牌',
        content: '开通月卡需要指定车牌，请先到账号信息页绑定',
        confirmText: '去绑定',
        success: (r) => { if (r.confirm) wx.navigateTo({ url: '/pages/profile/profile' }) }
      })
      return
    }
    const type = this.data.selectedType
    const price = this.getPrice(type)
    const balance = Number(this.data.balance)
    if (balance < Number(price)) {
      wx.showModal({
        title: '余额不足',
        content: `开通需 ${price} 元，当前余额 ${this.data.balance} 元，请先充值`,
        confirmText: '去充值',
        success: (r) => { if (r.confirm) wx.navigateTo({ url: '/pages/recharge/recharge' }) }
      })
      return
    }
    wx.showModal({
      title: '确认开通',
      content: `${PASS_TYPES.find((p) => p.key === type).label}\n车牌 ${this.data.plateNumber}\n实付 ${price} 元`,
      success: async (r) => {
        if (!r.confirm) return
        this.setData({ openPassing: true })
        try {
          await request.post('/user/monthly-pass/open', {
            passType: type, plateNumber: this.data.plateNumber
          })
          wx.showToast({ title: '开通成功', icon: 'success' })
          this.loadAll()
        } catch (e) { /* request 已处理错误提示 */ }
        this.setData({ openPassing: false })
      }
    })
  },

  /**
   * 开启月卡到期提醒（必须由用户点击触发，微信要求订阅授权在交互回调内发起）。
   * 按当前最近的一个里程碑（7/3/1 天）授权，后端到期前对应天数时发送。
   */
  async subscribeExpireRemind() {
    const pass = this.data.currentPass
    if (!pass) {
      wx.showToast({ title: '当前没有生效月卡', icon: 'none' })
      return
    }
    const daysLeft = daysUntil(pass.expireDate)
    if (daysLeft === null) {
      wx.showToast({ title: '到期日异常，请稍后重试', icon: 'none' })
      return
    }
    if (daysLeft <= 0) {
      wx.showToast({
        title: daysLeft === 0 ? '月卡今天到期，请及时续费' : '月卡已到期，请直接续费',
        icon: 'none'
      })
      return
    }
    const milestone = daysLeft > 3 ? 7 : (daysLeft > 1 ? 3 : 1)
    const accepted = await requestAndReport(
      SubscribeScenes.SCENE_MONTHLY_PASS_EXPIRE,
      `${pass.id}:${milestone}`
    )
    if (accepted) {
      wx.showToast({ title: '已开启到期提醒', icon: 'success' })
    }
  },

  goRecharge() {
    wx.navigateTo({ url: '/pages/recharge/recharge' })
  }
})
