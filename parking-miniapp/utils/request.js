const BASE_URL = 'http://localhost:8080/api'

function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
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
          uni.removeStorageSync('token')
          uni.showToast({ title: 'Please login', icon: 'none' })
          uni.reLaunch({ url: '/pages/login/login' })
          reject(res.data)
        } else {
          uni.showToast({ title: res.data.message || 'Error', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        uni.showToast({ title: 'Network error', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function get(url, data) { return request({ url, method: 'GET', data }) }
export function post(url, data) { return request({ url, method: 'POST', data }) }
export function put(url, data) { return request({ url, method: 'PUT', data }) }
export default { get, post, put }
