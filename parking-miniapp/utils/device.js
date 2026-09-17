/**
 * 微信登录/绑定相关的公用工具
 */

/** 生成并复用稳定的设备标识：开发模式下后端据此生成稳定的 mock openid */
export function getDeviceId() {
  let id = wx.getStorageSync('device_id')
  if (!id) {
    id = 'dev_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10)
    wx.setStorageSync('device_id', id)
  }
  return id
}

/** 调用 wx.login 获取临时登录 code */
export function getWxCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res && res.code) resolve(res.code)
        else reject(new Error('微信授权失败'))
      },
      fail: () => reject(new Error('微信授权失败'))
    })
  })
}
