// 后端接口地址：本地开发请改为你电脑的局域网 IP（命令行执行 ipconfig 查看 IPv4 地址）
// 真机预览必须使用局域网 IP；仅在开发者工具中调试可用 http://localhost:8080/api
const BASE_URL = 'http://192.168.2.132:8080/api'

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data)
        } else if (res.data.code === 401) {
          // 账号被禁用或 token 失效，强制登出并回到登录页
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          wx.reLaunch({ url: '/pages/login/login' })
          reject(res.data)
        } else {
          if (!options.silent) {
            wx.showToast({ title: res.data.message || '错误', icon: 'none' })
          }
          reject(res.data)
        }
      },
      fail(err) {
        if (!options.silent) {
          wx.showToast({ title: '网络错误', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

// silent = true 时不弹默认错误提示，由调用方自行处理错误
export function get(url, data, silent) { return request({ url, method: 'GET', data, silent }) }
export function post(url, data, silent) { return request({ url, method: 'POST', data, silent }) }
export function put(url, data, silent) { return request({ url, method: 'PUT', data, silent }) }

/** 静态文件（头像等）根地址：去掉 /api 后缀 */
export const FILE_BASE = BASE_URL.replace(/\/api$/, '')

/**
 * 上传文件（头像等），返回 Promise<响应体>
 * @param {string} url 相对路径，如 /user/avatar
 * @param {string} filePath 本地临时文件路径
 * @param {object} formData 附加表单字段
 */
export function upload(url, filePath, formData) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.uploadFile({
      url: BASE_URL + url,
      filePath,
      name: 'file',
      formData: formData || {},
      header: { 'Authorization': token ? 'Bearer ' + token : '' },
      success(res) {
        let body = res.data
        try { body = JSON.parse(res.data) } catch (e) { /* 非 JSON 响应保持原文 */ }
        if (body && body.code === 200) {
          resolve(body)
        } else if (body && body.code === 401) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          wx.reLaunch({ url: '/pages/login/login' })
          reject(body)
        } else {
          wx.showToast({ title: (body && body.message) || '上传失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default { get, post, put, upload }
