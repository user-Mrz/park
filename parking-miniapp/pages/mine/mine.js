const app = getApp()
import { createPoller } from '../../utils/poller.js'
import request, { triggerUnauthorized } from '../../utils/request.js'
import { cacheGet, cacheSet, cacheClear } from '../../utils/pageCache.js'

const IDENTITY_LABELS = { 1: '学生', 2: '教师', 3: '职工', 4: '访客', 5: '家长' }
const AUDIT_LABELS = { 0: '待审核', 1: '已认证', 2: '已驳回' }

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    fileBase: '',
    identityText: '',
    // ---- 用户角色与可见停车区域（RBAC），接口失败时为空 ----
    permRoleText: '',
    permZoneText: ''
  },

  onShow() {
    try {
      const baseUrl = (app.globalData && app.globalData.baseUrl) || ''
      this.setData({ fileBase: baseUrl.replace(/\/api$/, '') })
      this.loadUserInfo()
      this.loadPerms()
      this.startSync()
    } catch (error) {
      // 页面初始化异常不应影响后续点击，记录日志便于定位
      console.error('[mine] onShow 异常:', error)
    }
  },

  onHide() {
    this.stopSync()
  },

  onUnload() {
    this.stopSync()
  },

  /** 每 10 秒同步一次账户信息：管理员结算扣费后余额自动更新 */
  startSync() {
    this.stopSync()
    this.balancePoller = createPoller(() => this.syncBalance(), 10000)
    this.balancePoller.start()
  },

  stopSync() {
    if (this.balancePoller) {
      this.balancePoller.stop()
      this.balancePoller = null
    }
  },

  syncBalance() {
    const token = wx.getStorageSync('token')
    if (!token || !this.data.isLoggedIn) return Promise.resolve()
    const self = this
    return new Promise((resolve) => {
      wx.request({
        url: app.globalData.baseUrl + '/user/info',
        timeout: 10000,
        header: { 'Authorization': 'Bearer ' + token },
        success: (res) => {
          if (res.data.code === 200) {
            const info = res.data.data
            const prev = self.data.userInfo || {}
            const prevBalance = prev.balance === undefined ? null : Number(prev.balance)
            const nextBalance = Number(info.balance)
            if (prevBalance !== null && prevBalance !== nextBalance) {
              const diff = nextBalance - prevBalance
              wx.showToast({
                title: diff < 0 ? `已扣费 ¥${Math.abs(diff).toFixed(2)}` : `已到账 ¥${diff.toFixed(2)}`,
                icon: 'none',
                duration: 2500
              })
            }
            self.setData({ userInfo: info, identityText: self.buildIdentityText(info) })
            wx.setStorageSync('userInfo', JSON.stringify(info))
          }
          resolve()
        },
        fail: () => resolve()
      })
    })
  },

  loadUserInfo() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, userInfo: null, permRoleText: '', permZoneText: '' })
      return
    }
    // 先用本地缓存立即渲染，切回「我的」tab 时不再白屏等请求
    const stored = wx.getStorageSync('userInfo')
    if (stored) {
      try {
        const info = JSON.parse(stored)
        this.setData({ isLoggedIn: true, userInfo: info, identityText: this.buildIdentityText(info) })
      } catch (e) { /* 本地缓存损坏时忽略 */ }
    }
    // 10 秒节流：onShow 与 10 秒余额轮询共用同一数据源，避免重复请求
    const nowMs = Date.now()
    if (nowMs - (this.lastInfoFetch || 0) < 10000) return
    this.lastInfoFetch = nowMs
    // 从后端获取最新余额
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      timeout: 10000,
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        this.lastInfoFetch = Date.now()
        if (res.data.code === 200) {
          const info = res.data.data
          this.setData({ isLoggedIn: true, userInfo: info, identityText: this.buildIdentityText(info) })
          wx.setStorageSync('userInfo', JSON.stringify(info))
          // 余额不足订阅提醒：余额低于 5 元时触发（本地防抖保证同会话只弹一次）
          this.tryBalanceLowSubscribe(info)
        } else if (res.data.code === 401) {
          triggerUnauthorized()
          this.setData({ isLoggedIn: false, userInfo: null })
        }
      },
      fail: () => {
        // 网络失败：允许下次 onShow 立即重试
        this.lastInfoFetch = 0
      }
    })
  },

  /**
   * 拉取当前用户角色与可见停车区域（RBAC）：silent=true
   * 先读 30 秒内存缓存立即渲染，再静默刷新；失败静默降级（不展示）
   */
  loadPerms() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ permRoleText: '', permZoneText: '' })
      return Promise.resolve()
    }
    const cached = cacheGet('myPerms', 30000)
    if (cached) this.applyPerms(cached)
    return request.get('/user/my-perms', {}, true).then((res) => {
      const d = (res && res.data) || {}
      cacheSet('myPerms', d)
      this.applyPerms(d)
    }).catch(() => {
      this.setData({ permRoleText: '', permZoneText: '' })
    })
  },

  applyPerms(d) {
    this.setData({
      permRoleText: d.roleName || d.identityLabel || '',
      permZoneText: (d.zoneNames || []).join('、')
    })
  },

  // 身份展示文案，如「学生 · 已认证 · S2021001」
  buildIdentityText(info) {
    if (!info || !info.identityType) return ''
    const identity = IDENTITY_LABELS[info.identityType] || '用户'
    const audit = AUDIT_LABELS[info.identityStatus] || ''
    const no = info.userNo ? ` · ${info.userNo}` : ''
    return `${identity}${audit ? ' · ' + audit : ''}${no}`
  },

  goPage(e) {
    const url = e.currentTarget.dataset.url
    console.log('[mine] goPage 点击:', url)
    if (!url) return
    const token = wx.getStorageSync('token')
    // 未登录时除登录页外，一律先去登录
    const target = (!token && url !== '/pages/login/login') ? '/pages/login/login' : url
    wx.navigateTo({
      url: target,
      success: () => console.log('[mine] 跳转成功:', target),
      fail: (err) => {
        console.error('[mine] 跳转失败:', target, err && err.errMsg)
        wx.showToast({
          title: '页面跳转失败：' + ((err && err.errMsg) || '未知原因'),
          icon: 'none',
          duration: 2500
        })
      }
    })
  },

  // 点击顶部头像卡片 → 账号信息
  goProfile() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    wx.navigateTo({ url: '/pages/profile/profile' })
  },

  /**
   * 余额不足订阅提醒触发。
   * 条件：已登录 + 余额 < 5 元 + 本次会话内未弹过。
   */
  tryBalanceLowSubscribe(info) {
    if (!info) return
    const balance = Number(info.balance || 0)
    if (balance >= 5) return
    // 触发 requestSubscribeMessage（必须在用户点击事件里？不，这里在 onShow 回调里
    // 直接调用也可以，但微信可能会拒绝——所以我们放在余额浮层点击时触发更安全。
    // 这里先记录，让用户点击「充值」按钮时再触发授权更稳妥）
    // 暂时不自动弹，避免 onShow 时无用户交互导致微信静默拒绝。
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          cacheClear()
          this.setData({ isLoggedIn: false, userInfo: null, permRoleText: '', permZoneText: '' })
          wx.showToast({ title: '已退出', icon: 'none' })
        }
      }
    })
  }
})
