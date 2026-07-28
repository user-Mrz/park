const BASE_URL = 'http://192.168.2.247:8080/api'

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
          wx.removeStorageSync('token')
          wx.showToast({ title: '请登录', icon: 'none' })
          wx.reLaunch({ url: '/pages/login/login' })
          reject(res.data)
        } else {
          wx.showToast({ title: res.data.message || '错误', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function get(url, data) { return request({ url, method: 'GET', data }) }
export function post(url, data) { return request({ url, method: 'POST', data }) }
export function put(url, data) { return request({ url, method: 'PUT', data }) }
export default { get, post, put }
