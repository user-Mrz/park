/**
 * 后端接口地址配置
 *
 * 微信开发者工具里的请求是由电脑代理发出的，所以**在开发者工具中直接用 localhost 最稳**，
 * 电脑 IP 变了也不用改；而**真机预览 / 体验版**必须使用电脑的局域网 IP
 * （命令行执行 ipconfig，看 WLAN 适配器的 IPv4 地址），且手机与电脑要在同一局域网。
 *
 * 换网络后如果真机连不上，只需要改下面的 LAN_HOST 一处（本地改成自己的局域网 IP，
 * 不要把这个改动提交到仓库）；也可以在开发者工具 Console 里一次性覆盖（优先级更高）：
 *   wx.setStorageSync('serverBase', 'http://<你自己的局域网IP>:8080')   // 不要带 /api
 * 清除覆盖：wx.removeStorageSync('serverBase')
 */
// 401 强制登出时清空页面级内存缓存，防止下一个账号渲染到上一个账号的个人数据
import { cacheClear } from './pageCache.js'

const PORT = 8080
/**
 * 真机预览用的电脑局域网 IP。
 * 仓库里保留占位值 localhost，真机调试时在本地改成自己的局域网 IP（如 192.168.1.10），
 * 或者按上面说明用 wx.setStorageSync('serverBase', ...) 临时覆盖，避免把本机网络信息提交上去。
 */
const LAN_HOST = 'localhost'

/** 判断运行环境：开发者工具返回 'devtools'，真机返回 android / ios / windows / mac 等 */
function detectPlatform() {
  try {
    if (typeof wx.getDeviceInfo === 'function') {
      const info = wx.getDeviceInfo()
      if (info && info.platform) return info.platform
    }
  } catch (e) {
    // 低版本基础库没有该 API，走下面的兜底
  }
  try {
    const info = wx.getSystemInfoSync()
    return (info && info.platform) || ''
  } catch (e) {
    return ''
  }
}

/** 组装根地址：优先本地存储覆盖 -> 开发者工具用 localhost -> 其余（真机）用局域网 IP */
function resolveBaseUrl() {
  let root = ''
  try {
    const saved = wx.getStorageSync('serverBase')
    if (saved) {
      root = String(saved).trim().replace(/\/+$/, '').replace(/\/api$/, '')
    }
  } catch (e) {
    // 存储不可用时忽略，用默认地址
  }
  if (!root) {
    root = detectPlatform() === 'devtools'
      ? 'http://localhost:' + PORT
      : 'http://' + LAN_HOST + ':' + PORT
  }
  return root + '/api'
}

const BASE_URL = resolveBaseUrl()

/** 401 防抖：保证一次会话内只触发一次强制登出 */
let _unauthorizedDispatching = false
function triggerUnauthorized() {
  if (_unauthorizedDispatching) return
  _unauthorizedDispatching = true
  try {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    cacheClear()
  } catch (e) {}
  wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
  setTimeout(() => {
    wx.reLaunch({ url: '/pages/login/login' })
    setTimeout(() => { _unauthorizedDispatching = false }, 3000)
  }, 300)
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: options.timeout || 10000,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data)
        } else if (res.data.code === 401) {
          triggerUnauthorized()
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

/** 接口根地址（含 /api），供 app.js 复用到 globalData.baseUrl，避免出现第二处硬编码 */
export { BASE_URL }

/** 服务器根地址（不含 /api），用于拼接 /uploads/** 静态资源与附件下载地址 */
export const SERVER_ROOT = BASE_URL.replace(/\/api$/, '')

/** 兼容旧命名：与 SERVER_ROOT 相同 */
export const FILE_BASE = SERVER_ROOT

/** 统一的 401 防抖登出逻辑，供其它直接用 wx.request 的页面复用 */
export { triggerUnauthorized }

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
          triggerUnauthorized()
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
