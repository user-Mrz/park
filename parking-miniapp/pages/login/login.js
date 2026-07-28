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
    captchaKey: ''
  },

  onLoad() {
    this.loadCaptcha()
  },

  // ===== 输入绑定 =====
  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) {
      this.setData({ [field]: e.detail.value })
    }
  },

  // ===== 验证码 =====
  async loadCaptcha() {
    try {
      const res = await this._request('/auth/captcha')
      this.setData({
        captchaKey: res.data.key,
        captchaImg: res.data.image
      })
    } catch (e) {
      // 失败不提示，用户点图片可重试
    }
  },

  // ===== 切换登录/注册 =====
  switchType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      loginType: type,
      captchaCode: ''
    })
    this.loadCaptcha()
  },

  // ===== 手机号登录 =====
  handleLogin() {
    const { phone, password, captchaCode, captchaKey } = this.data

    if (!phone || !phone.match(/^1[3-9]\d{9}$/)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' }); return
    }
    if (!captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' }); return
    }
    if (!captchaKey) {
      wx.showToast({ title: '验证码加载失败，点击图片重试', icon: 'none' }); return
    }

    this._doRequest('/auth/login', 'POST', {
      phone, password, captchaKey, captchaCode
    })
  },

  // ===== 手机号注册 =====
  handleRegister() {
    const { phone, password, nickname, plateNumber, captchaCode, captchaKey } = this.data

    if (!phone || !phone.match(/^1[3-9]\d{9}$/)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
    }
    if (!password || password.length < 6) {
      wx.showToast({ title: '密码至少6位', icon: 'none' }); return
    }
    if (!captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' }); return
    }
    if (!captchaKey) {
      wx.showToast({ title: '验证码加载失败，点击图片重试', icon: 'none' }); return
    }

    const data = {
      phone, password, captchaKey, captchaCode
    }
    if (nickname) data.nickname = nickname
    if (plateNumber) data.plateNumber = plateNumber

    this._doRequest('/auth/register', 'POST', data)
  },

  // ===== 微信登录 =====
  handleWechatLogin() {
    wx.showLoading({ title: '登录中...', mask: true })
    wx.login({
      success: async (loginRes) => {
        if (!loginRes.code) {
          wx.hideLoading()
          wx.showToast({ title: '微信授权失败', icon: 'none' }); return
        }
        try {
          const res = await this._request('/auth/wechat-login', 'POST', { code: loginRes.code })
          wx.hideLoading()
          this._onLoginSuccess(res)
        } catch (e) {
          wx.hideLoading()
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '微信登录失败', icon: 'none' })
      }
    })
  },

  // ===== 通用请求方法 =====
  _request(url, method = 'GET', data = {}) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.baseUrl + url,
        method,
        data,
        header: { 'Content-Type': 'application/json' },
        success(res) {
          if (res.statusCode === 200 && res.data.code === 200) {
            resolve(res.data)
          } else {
            const msg = (res.data && res.data.message) || ('服务器错误 HTTP' + res.statusCode)
            wx.showToast({ title: msg, icon: 'none', duration: 2000 })
            reject(res.data || res)
          }
        },
        fail(err) {
          wx.showToast({ title: '网络异常，请检查服务器是否启动', icon: 'none', duration: 2000 })
          reject(err)
        }
      })
    })
  },

  // ===== 提交登录/注册（防重复点击 + 超时保护）=====
  _doRequest(url, method, data) {
    if (this._submitting) return   // 防重复点击
    this._submitting = true

    wx.showLoading({ title: '请稍候...', mask: true })

    // 超时保护：8秒后强制解除 loading，防止卡死
    const timer = setTimeout(() => {
      wx.hideLoading()
      this._submitting = false
    }, 8000)

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        clearTimeout(timer)
        wx.hideLoading()
        this._submitting = false

        if (res.statusCode === 200 && res.data.code === 200) {
          this._onLoginSuccess(res.data)
        } else {
          const msg = (res.data && res.data.message) || ('服务器错误 HTTP' + res.statusCode)
          wx.showToast({ title: msg, icon: 'none', duration: 2000 })
          this.loadCaptcha()
          this.setData({ captchaCode: '' })
        }
      },
      fail: () => {
        clearTimeout(timer)
        wx.hideLoading()
        this._submitting = false
        wx.showToast({ title: '网络异常，请检查服务器是否启动', icon: 'none', duration: 2000 })
      }
    })
  },

  _onLoginSuccess(result) {
    wx.setStorageSync('token', result.data.token)
    wx.setStorageSync('userInfo', JSON.stringify(result.data))
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      wx.switchTab({ url: '/pages/index/index' })
    }, 800)
  }
})
