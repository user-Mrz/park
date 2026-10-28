// 服务器地址统一由 utils/request.js 解析（开发者工具自动用 localhost、真机用局域网 IP），
// 这里直接复用，避免出现两处硬编码地址不一致导致"部分页面能连、部分页面超时"的问题。
import { BASE_URL, SERVER_ROOT } from './utils/request.js'

App({
  onLaunch() {
    console.log('App Launch')
    console.log('[parking] 服务器地址：' + BASE_URL)
    const token = wx.getStorageSync('token')
    // 全局登录拦截：未登录时无论用户从哪个入口打开小程序，都强制跳转到登录页
    // 登录成功后由 login.js 跳转到首页（或分享目标页）
    if (!token) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    // 已登录：把本地 userInfo 同步到 globalData，方便各页面直接读取
    try {
      this.globalData.userInfo = wx.getStorageSync('userInfo') || null
      this.globalData.token = token
    } catch (e) {
      //StorageSync 读取失败不阻塞主流程
    }
  },

  globalData: {
    userInfo: null,
    token: null,
    /** 接口根地址（含 /api） */
    baseUrl: BASE_URL,
    /** 静态文件根地址（不含 /api），用于头像等 */
    fileBase: SERVER_ROOT
  }
})
