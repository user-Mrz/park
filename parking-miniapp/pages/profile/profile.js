import request, { FILE_BASE, upload } from '../../utils/request.js'
import { getDeviceId, getWxCode } from '../../utils/device.js'
import { requestAndReport, SubscribeScenes } from '../../utils/subscribe.js'

const PHONE_RE = /^1[3-9]\d{9}$/
const PLATE_RE = /^[\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}$/
const USER_NO_RE = /^[A-Za-z0-9]{4,20}$/
/** 校园身份类型（与后端 1~5 对应） */
const IDENTITY_LABELS = ['学生', '教师', '职工', '访客', '家长']

function maskId(id) {
  const s = String(id || '')
  if (!s) return '-'
  return s.length <= 8 ? s : s.slice(0, 4) + '****' + s.slice(-4)
}

function maskOpenid(openid) {
  const s = String(openid || '')
  if (!s) return '未绑定微信'
  return s.length <= 10 ? s : s.slice(0, 6) + '****' + s.slice(-4)
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    fileBase: FILE_BASE,
    loading: true,
    saving: false,
    uploading: false,
    isLoggedIn: false,
    nickname: '',
    phone: '',
    plateNumber: '',
    avatar: '',
    userId: '-',
    openidText: '',
    bound: false,
    binding: false,
    createdAt: '-',
    statusText: '正常',
    balance: '0.00',
    original: { nickname: '', phone: '', plateNumber: '' },
    // ---- 校园身份 ----
    identityLabels: IDENTITY_LABELS,
    identityIndex: 0,
    identityStatusText: '待审核',
    identityVerified: false,
    identityRemark: '',
    userNo: '',
    department: '',
    className: '',
    savingIdentity: false,
    benefits: null,
    // 学院/部门下拉（来自管理端「学院管理」维护的启用列表）
    deptNames: [],
    deptIndex: -1
  },

  onShow() {
    this.loadProfile()
    this.loadDepartments()
  },

  /** 学院下拉数据（公开接口） */
  loadDepartments() {
    request.get('/departments', {}, true).then((res) => {
      const list = res.data || []
      const names = list.map((d) => d.name)
      const current = (this.data.department || '').trim()
      const idx = names.indexOf(current)
      this.setData({ deptNames: names, deptIndex: idx })
    }).catch(() => { /* 加载失败时保留手动输入 */ })
  },

  onDeptChange(e) {
    const idx = Number(e.detail.value)
    this.setData({ deptIndex: idx, department: this.data.deptNames[idx] || '' })
  },

  async loadProfile() {
    const token = wx.getStorageSync('token')
    if (!token) {
      this.setData({ isLoggedIn: false, loading: false })
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    try {
      const res = await request.get('/user/info', {}, true)
      const u = res.data || {}
      this.setData({
        loading: false,
        isLoggedIn: true,
        nickname: u.nickname || '',
        phone: u.phone || '',
        plateNumber: u.plateNumber || '',
        avatar: u.avatar || '',
        userId: maskId(u.id),
        openidText: maskOpenid(u.openid),
        bound: !!u.openid,
        createdAt: formatTime(u.createdAt),
        statusText: Number(u.status) === 1 ? '正常' : '已禁用',
        balance: Number(u.balance || 0).toFixed(2),
        original: {
          nickname: u.nickname || '',
          phone: u.phone || '',
          plateNumber: u.plateNumber || ''
        },
        userNo: u.userNo || '',
        department: u.department || '',
        className: u.className || '',
        identityIndex: Math.max(0, (Number(u.identityType) || 1) - 1)
      })
      wx.setStorageSync('userInfo', JSON.stringify(u))
      this.loadBenefits()
    } catch (e) {
      this.setData({ loading: false })
      const stored = wx.getStorageSync('userInfo')
      if (stored) {
        const u = JSON.parse(stored)
        this.setData({
          isLoggedIn: true,
          nickname: u.nickname || '',
          phone: u.phone || '',
          plateNumber: u.plateNumber || '',
          avatar: u.avatar || '',
          bound: !!u.openid,
          balance: Number(u.balance || 0).toFixed(2)
        })
      }
    }
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onPlateInput(e) {
    this.setData({ plateNumber: (e.detail.value || '').toUpperCase() })
  },

  pickImage() {
    return new Promise((resolve) => {
      if (wx.chooseMedia) {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sizeType: ['compressed'],
          success: (res) => {
            const f = res.tempFiles && res.tempFiles[0]
            resolve(f ? f.tempFilePath : '')
          },
          fail: () => resolve('')
        })
      } else {
        wx.chooseImage({
          count: 1,
          sizeType: ['compressed'],
          success: (res) => resolve((res.tempFilePaths && res.tempFilePaths[0]) || ''),
          fail: () => resolve('')
        })
      }
    })
  },

  async chooseAvatar() {
    if (this.data.uploading) return
    const path = await this.pickImage()
    if (!path) return
    this.setData({ uploading: true })
    try {
      const res = await upload('/user/avatar', path)
      const url = (res.data && res.data.url) || ''
      this.setData({ avatar: url, uploading: false })
      const stored = wx.getStorageSync('userInfo')
      if (stored) {
        const info = JSON.parse(stored)
        info.avatar = url
        wx.setStorageSync('userInfo', JSON.stringify(info))
      }
      wx.showToast({ title: '头像已更新', icon: 'success' })
    } catch (e) {
      this.setData({ uploading: false })
    }
  },

  removeAvatar() {
    wx.showModal({
      title: '移除头像',
      content: '确定移除当前头像？',
      success: async (r) => {
        if (!r.confirm) return
        try {
          const res = await request.put('/user/info', { avatar: '' })
          const u = res.data || {}
          this.setData({ avatar: u.avatar || '' })
          wx.setStorageSync('userInfo', JSON.stringify(u))
          wx.showToast({ title: '已移除', icon: 'none' })
        } catch (e) { /* 错误提示由 request 统一处理 */ }
      }
    })
  },

  confirm(title, content) {
    return new Promise((resolve) => {
      wx.showModal({
        title,
        content,
        success: (r) => resolve(!!r.confirm),
        fail: () => resolve(false)
      })
    })
  },

  async saveProfile() {
    if (this.data.saving) return
    const nickname = (this.data.nickname || '').trim()
    const phone = (this.data.phone || '').trim()
    const plateNumber = (this.data.plateNumber || '').trim().toUpperCase()

    if (!nickname) return wx.showToast({ title: '昵称不能为空', icon: 'none' })
    if (nickname.length > 20) return wx.showToast({ title: '昵称不能超过 20 个字', icon: 'none' })
    if (phone && !PHONE_RE.test(phone)) return wx.showToast({ title: '手机号格式不正确', icon: 'none' })
    if (plateNumber && !PLATE_RE.test(plateNumber)) {
      return wx.showToast({ title: '车牌号格式不正确，如 粤B12345', icon: 'none' })
    }
    if (phone !== this.data.original.phone) {
      const ok = await this.confirm(
        '更换手机号',
        phone ? '手机号是登录账号，更换后请使用新手机号 + 密码登录，确定继续？' : '清空手机号后将无法使用手机号登录，确定继续？'
      )
      if (!ok) return
    }

    this.setData({ saving: true })
    try {
      const res = await request.put('/user/info', { nickname, phone, plateNumber })
      const u = res.data || {}
      wx.setStorageSync('userInfo', JSON.stringify(u))
      this.setData({
        saving: false,
        nickname: u.nickname || nickname,
        phone: u.phone || '',
        plateNumber: u.plateNumber || '',
        original: {
          nickname: u.nickname || '',
          phone: u.phone || '',
          plateNumber: u.plateNumber || ''
        }
      })
      wx.showToast({ title: '保存成功', icon: 'success' })
    } catch (e) {
      this.setData({ saving: false })
    }
  },

  goPassword() {
    wx.navigateTo({ url: '/pages/password/password' })
  },

  // ---- 校园身份与权益 ----
  async loadBenefits() {
    try {
      const res = await request.get('/user/benefits', {}, true)
      const b = res.data || {}
      this.setData({
        benefits: b,
        identityIndex: Math.max(0, (Number(b.identityType) || 1) - 1),
        identityStatusText: b.identityStatusLabel || '待审核',
        identityVerified: !!b.verified,
        identityRemark: b.identityRemark || ''
      })
    } catch (e) {
      /* 权益加载失败不影响资料展示 */
    }
  },

  onIdentityChange(e) {
    this.setData({ identityIndex: Number(e.detail.value) })
  },
  onUserNoInput(e) { this.setData({ userNo: e.detail.value }) },
  onDepartmentInput(e) { this.setData({ department: e.detail.value }) },
  onClassNameInput(e) { this.setData({ className: e.detail.value }) },

  async saveIdentity() {
    if (this.data.savingIdentity) return
    const identityType = this.data.identityIndex + 1
    const userNo = (this.data.userNo || '').trim()
    const department = (this.data.department || '').trim()
    const className = (this.data.className || '').trim()
    if (identityType <= 3 && !USER_NO_RE.test(userNo)) {
      return wx.showToast({ title: '请填写 4-20 位学号/工号', icon: 'none' })
    }
    const ok = await this.confirm(
      '提交校园身份',
      identityType <= 3
        ? '学生/教师/职工身份将先由校园管理系统自动核验，核验通过立即生效，未查询到时转管理员审核，确定提交？'
        : '访客/家长身份无需审核，将按访客费率计费，确定提交？'
    )
    if (!ok) return
    this.setData({ savingIdentity: true })
    try {
      const res = await request.put('/user/info', { userNo, identityType, department, className })
      const u = res.data || {}
      wx.setStorageSync('userInfo', JSON.stringify(u))
      this.setData({
        savingIdentity: false,
        identityIndex: Math.max(0, (Number(u.identityType) || 1) - 1),
        userNo: u.userNo || '',
        department: u.department || '',
        className: u.className || ''
      })
      const pending = identityType <= 3 && Number(u.identityStatus) === 0
      wx.showToast({
        title: identityType <= 3 ? (pending ? '已提交，等待审核' : '身份已自动认证') : '身份已更新',
        icon: 'success'
      })
      // 仅待人工审核时需要订阅审核结果通知（自动认证无需等待）
      if (pending) {
        requestAndReport(SubscribeScenes.SCENE_IDENTITY_RESULT, String(u.id || ''))
      }
      this.loadBenefits()
    } catch (e) {
      this.setData({ savingIdentity: false })
    }
  },

  // 绑定微信到当前账号（已登录状态，不会新建账号）
  async bindWechat() {
    if (this.data.binding) return
    this.setData({ binding: true })
    try {
      const code = await getWxCode()
      const res = await request.post('/user/bind-wechat', { code, deviceId: getDeviceId() })
      const u = res.data || {}
      this.setData({ binding: false, bound: !!u.openid, openidText: maskOpenid(u.openid) })
      wx.setStorageSync('userInfo', JSON.stringify(u))
      wx.showToast({ title: '绑定成功', icon: 'success' })
    } catch (e) {
      this.setData({ binding: false })
    }
  },

  // 解除微信绑定
  unbindWechat() {
    wx.showModal({
      title: '解除微信绑定',
      content: '解绑后将无法使用微信一键登录，仍可用手机号+密码登录。确定解绑？',
      success: async (r) => {
        if (!r.confirm) return
        try {
          const res = await request.post('/user/unbind-wechat')
          const u = res.data || {}
          this.setData({ bound: !!u.openid, openidText: maskOpenid(u.openid) })
          wx.setStorageSync('userInfo', JSON.stringify(u))
          wx.showToast({ title: '已解除绑定', icon: 'none' })
        } catch (e) { /* 错误提示由 request 统一处理 */ }
      }
    })
  }
})
