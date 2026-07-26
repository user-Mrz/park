import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref<any>(null)

  async function login(username: string, password: string, captchaKey: string, captchaCode: string) {
    const res: any = await loginApi({ username, password, captchaKey, captchaCode })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('admin_token', res.data.token)
    return res.data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
  }

  return { token, userInfo, login, logout }
})
