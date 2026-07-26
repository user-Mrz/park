import request from '@/utils/request'

export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function login(data: { username: string; password: string; captchaKey: string; captchaCode: string }) {
  return request.post('/auth/admin/login', data)
}
