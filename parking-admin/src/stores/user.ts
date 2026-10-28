import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { getMyMenus } from '@/api/admin'

interface UserState {
  token: string
  userInfo: any
  role: string
  myMenus: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('admin_token') || '',
    userInfo: null,
    role: localStorage.getItem('admin_role') || '',
    myMenus: JSON.parse(localStorage.getItem('admin_my_menus') || '[]')
  }),
  actions: {
    async login(username: string, password: string, captchaKey: string, captchaCode: string) {
      const res: any = await loginApi({ username, password, captchaKey, captchaCode })
      localStorage.setItem('admin_token', res.data.token)
      this.token = res.data.token
      this.userInfo = res.data
      this.role = res.data.role || ''
      localStorage.setItem('admin_role', this.role)
      // 登录成功后立即拉取自己的菜单 code 列表
      await this.fetchMyMenus()
      return res.data
    },
    async fetchMyMenus() {
      try {
        const res: any = await getMyMenus()
        this.myMenus = Array.isArray(res?.data) ? res.data : []
        localStorage.setItem('admin_my_menus', JSON.stringify(this.myMenus))
      } catch (e) {
        // 拉取失败按空权限处理
        this.myMenus = []
        localStorage.setItem('admin_my_menus', '[]')
      }
    },
    logout() {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      localStorage.removeItem('admin_my_menus')
      this.token = ''
      this.userInfo = null
      this.role = ''
      this.myMenus = []
    }
  }
})
