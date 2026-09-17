const app = getApp()
import { getDeviceId, getWxCode } from '../../utils/device.js'

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
    isLoggedIn: false,
    // 校园身份注册字段
    identityLabels: ['学生', '教师', '职工', '访客', '家长'],
    identityIndex: 0,
    userNo: '',
    department: '',
    className: '',
    // 学院/部门下拉（来自管理端「学院管理」维护的启用列表）
    departments: [],
    deptNames: [],
    deptIndex: -1
  },

  onLoad() {
    this.setData({ isLoggedIn: !!wx.getStorageSync('token') })
    this.loadCaptcha()
    this.loadDepartments()
  },

  onShow() {
    this.setData({ isLoggedIn: !!wx.getStorageSync('token') })
  },

  // ===== 输入绑定 =====
  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) {
      this.setData({ [field]: e.detail.value })
    }
  },

  // ===== 学院/部门（公开接口，注册时下拉选择） =====
  loadDepartments() {
    this._request('/departments').then((res) => {
      const list = res.data || []
      this.setData({
        departments: list,
        deptNames: list.map((d) => d.name)
      })
    }).catch(() => { /* 加载失败时可手动输入 */ })
  },

  onDeptChange(e) {
    const idx = Number(e.detail.value)
    this.setData({ deptIndex: idx, department: this.data.deptNames[idx] || '' })
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

  onIdentityChange(e) {
    this.setData({ identityIndex: Number(e.detail.value) })
  },

  // ===== 手机号注册 =====
  handleRegister() {
    const { phone, password, nickname, plateNumber, captchaCode, captchaKey,
            identityIndex, userNo, department, className } = this.data

    if (!phone || !phone.match(/^1[3-9]\d{9}$/)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
    }
    if (!password || password.length < 6) {
      wx.showToast({ title: '密码至少6位', icon: 'none' }); return
    }
    const identityType = identityIndex + 1
    if (identityType <= 3 && !/^[A-Za-z0-9]{4,20}$/.test((userNo || '').trim())) {
      wx.showToast({ title: '请填写 4-20 位学号/工号', icon: 'none' }); return
    }
    if (identityType <= 3 && !(department || '').trim()) {
      wx.showToast({ title: '请选择学院/部门', icon: 'none' }); return
    }
    if (!captchaCode) {
      wx.showToast({ title: '请输入验证码', icon: 'none' }); return
    }
    if (!captchaKey) {
      wx.showToast({ title: '验证码加载失败，点击图片重试', icon: 'none' }); return
    }

    const data = {
      phone, password, captchaKey, captchaCode, identityType,
      userNo: (userNo || '').trim(),
      department: (department || '').trim(),
      className: (className || '').trim()
    }
    if (nickname) data.nickname = nickname
    if (plateNumber) data.plateNumber = plateNumber

    this._doRequest('/auth/register', 'POST', data)
  },

  // ===== 微信登录 / 绑定 =====
  // 未登录：微信一键登录（openid 已注册则登录，否则创建新账号）
  // 已登录：把当前微信绑定到已登录账号（避免又生成一个重复的空账号）
  async handleWechatLogin() {
    const token = wx.getStorageSync('token')
    wx.showLoading({ title: token ? '绑定中...' : '登录中...', mask: true })
    try {
      const code = await getWxCode()
      const deviceId = getDeviceId()

      if (token) {
        const res = await this._request('/user/bind-wechat', 'POST', { code, deviceId })
        wx.hideLoading()
        const info = (res && res.data) || {}
        if (info.id) wx.setStorageSync('userInfo', JSON.stringify(info))
        wx.showToast({ title: '已绑定当前账号', icon: 'success' })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/mine/mine' })
        }, 800)
        return
      }

      const res = await this._request('/auth/wechat-login', 'POST', { code, deviceId })
      wx.hideLoading()
      this._onLoginSuccess(res)
    } catch (e) {
      wx.hideLoading()
    }
  },

  // ===== 通用请求方法 =====
  _request(url, method = 'GET', data = {}) {
    return new Promise((resolve, reject) => {
      const token = wx.getStorageSync('token')
      wx.request({
        url: app.globalData.baseUrl + url,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? 'Bearer ' + token : ''
        },
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
