const app = getApp()

const IDENTITY_LABELS = { 1: '学生', 2: '教师', 3: '职工', 4: '访客', 5: '家长' }
const AUDIT_LABELS = { 0: '待审核', 1: '已认证', 2: '已驳回' }

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    fileBase: '',
    identityText: ''
  },

  onShow() {
    const baseUrl = (app.globalData && app.globalData.baseUrl) || ''
    this.setData({ fileBase: baseUrl.replace(/\/api$/, '') })
    this.loadUserInfo()
  },

  loadUserInfo() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, userInfo: null })
      return
    }
    // 从后端获取最新余额，不用 localStorage 缓存
    wx.request({
      url: app.globalData.baseUrl + '/user/info',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        if (res.data.code === 200) {
          const info = res.data.data
          this.setData({ isLoggedIn: true, userInfo: info, identityText: this.buildIdentityText(info) })
          wx.setStorageSync('userInfo', JSON.stringify(info))
        } else if (res.data.code === 401) {
          // 账号被禁用或 token 失效，强制登出
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          this.setData({ isLoggedIn: false, userInfo: null })
          wx.reLaunch({ url: '/pages/login/login' })
        }
      },
      fail: () => {
        // 网络失败时降级用缓存
        const stored = wx.getStorageSync('userInfo')
        if (stored) {
          const info = JSON.parse(stored)
          this.setData({ isLoggedIn: true, userInfo: info, identityText: this.buildIdentityText(info) })
        }
      }
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
    if (!url) return
    const token = wx.getStorageSync('token')
    if (!token && url !== '/pages/login/login') {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    wx.navigateTo({ url })
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

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          this.setData({ isLoggedIn: false, userInfo: null })
          wx.showToast({ title: '已退出', icon: 'none' })
        }
      }
    })
  }
})
