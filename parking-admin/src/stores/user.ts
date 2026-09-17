import { create } from 'zustand'
import { login as loginApi } from '@/api/auth'

interface UserState {
  token: string
  userInfo: any
  login: (username: string, password: string, captchaKey: string, captchaCode: string) => Promise<any>
  logout: () => void
}

export const useUserStore = create<UserState>((set) => ({
  token: localStorage.getItem('admin_token') || '',
  userInfo: null,
  async login(username, password, captchaKey, captchaCode) {
    const res: any = await loginApi({ username, password, captchaKey, captchaCode })
    localStorage.setItem('admin_token', res.data.token)
    set({ token: res.data.token, userInfo: res.data })
    return res.data
  },
  logout() {
    localStorage.removeItem('admin_token')
    set({ token: '', userInfo: null })
  }
}))
