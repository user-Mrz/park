import request from '../../utils/request.js'

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    submitting: false
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (!field) return
    this.setData({ [field]: e.detail.value })
  },

  async submit() {
    if (this.data.submitting) return
    const oldPassword = this.data.oldPassword
    const newPassword = this.data.newPassword
    const confirmPassword = this.data.confirmPassword

    if (!oldPassword) return wx.showToast({ title: '请输入原密码', icon: 'none' })
    if (newPassword.length < 6 || newPassword.length > 20) {
      return wx.showToast({ title: '新密码长度需为 6-20 位', icon: 'none' })
    }
    if (newPassword === oldPassword) {
      return wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
    }
    if (newPassword !== confirmPassword) {
      return wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    }

    this.setData({ submitting: true })
    try {
      await request.put('/user/password', { oldPassword, newPassword })
      this.setData({ submitting: false })
      wx.showModal({
        title: '修改成功',
        content: '密码已更新，请使用新密码重新登录',
        showCancel: false,
        success: () => {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          wx.reLaunch({ url: '/pages/login/login' })
        }
      })
    } catch (e) {
      this.setData({ submitting: false })
    }
  }
})
