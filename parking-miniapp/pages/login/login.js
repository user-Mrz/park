const app = getApp()

Page({
  data: {
    loginType: 'phone',
    phone: '',
    password: '',
    nickname: '',
    plateNumber: '',
    captchaCode: '',
    captchaImg: '',
    captchaKey: '',
    loading: false
  },

  onLoad() {
    this.loadCaptcha()
  },

  request(url, method = 'GET', data = {}) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.baseUrl + url,
        method,
        data,
        header: { 'Content-Type': 'application/json' },
        success(res) { resolve(res.data) },
        fail: reject
      })
    })
  },

  async loadCaptcha() {
    try {
      const res = await this.request('/auth/captcha')
      this.setData({
        captchaKey: res.data.key,
        captchaImg: res.data.image
      })
    } catch (e) {}
  },

  switchType(e) {
    this.setData({ loginType: e.currentTarget.dataset.type })
  },

  async handleLogin() {
    if (!this.data.phone || !this.data.password) {
      wx.showToast({ title: '请输入手机号和密码', icon: 'none' }); return
    }
    if (!this.data.captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' }); return
    }
    this.setData({ loading: true })
    try {
      const res = await this.request('/auth/login', 'POST', {
        phone: this.data.phone,
        password: this.data.password,
        captchaKey: this.data.captchaKey,
        captchaCode: this.data.captchaCode
      })
      wx.setStorageSync('token', res.data.token)
      wx.setStorageSync('userInfo', JSON.stringify(res.data))
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 500)
    } catch (e) {
      this.loadCaptcha()
      this.setData({ captchaCode: '' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleRegister() {
    if (!this.data.phone || !this.data.password) {
      wx.showToast({ title: '请输入手机号和密码', icon: 'none' }); return
    }
    if (!this.data.captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' }); return
    }
    this.setData({ loading: true })
    try {
      const res = await this.request('/auth/register', 'POST', {
        phone: this.data.phone,
        password: this.data.password,
        nickname: this.data.nickname,
        plateNumber: this.data.plateNumber,
        captchaKey: this.data.captchaKey,
        captchaCode: this.data.captchaCode
      })
      wx.setStorageSync('token', res.data.token)
      wx.setStorageSync('userInfo', JSON.stringify(res.data))
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 500)
    } catch (e) {
      this.loadCaptcha()
      this.setData({ captchaCode: '' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  handleWechatLogin() {
    wx.login({
      success: async (loginRes) => {
        try {
          const res = await this.request('/auth/wechat-login', 'POST', { code: loginRes.code })
          wx.setStorageSync('token', res.data.token)
          wx.setStorageSync('userInfo', JSON.stringify(res.data))
          wx.showToast({ title: '登录成功', icon: 'success' })
          setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 500)
        } catch (e) {}
      }
    })
  }
})
