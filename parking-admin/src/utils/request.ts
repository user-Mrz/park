import axios from 'axios'
import { message } from 'antd'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || 'Request failed')
      if (res.code === 401) {
        localStorage.removeItem('admin_token')
        router.navigate('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    message.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default request
