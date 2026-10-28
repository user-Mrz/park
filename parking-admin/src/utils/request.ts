import axios from 'axios'
import { ElMessage } from 'element-plus'
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
      // 401 未登录：清 token 跳登录页
      // 403 无权限：仅弹提示，不跳转，避免误把超管专属菜单的访问失败误判为掉线
      if (res.code === 401) {
        ElMessage.error(res.message || '请求失败')
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_role')
        localStorage.removeItem('admin_my_menus')
        router.replace('/login')
      } else if (res.code === 403) {
        // 403 无权限：静默处理，不弹提示，由页面组件自行控制显示
        console.warn('无权限访问:', res.message)
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
