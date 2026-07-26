<template>
  <view class="login-container">
    <view class="login-card">
      <view class="app-logo">🅿️</view>
      <view class="app-name">停车场</view>
      <view class="app-desc">智能停车，轻松出行</view>

      <view v-if="loginType === 'phone'">
        <view class="input-group">
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-group">
          <input class="input" v-model="password" type="password" placeholder="请输入密码" />
        </view>
        <view class="captcha-row">
          <input class="input captcha-input" v-model="captchaCode" placeholder="验证码" />
          <image v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="loadCaptcha" />
        </view>
        <button class="login-btn" @click="handleLogin" :loading="loading">登录</button>
        <view class="switch-row">
          <text>没有账号？</text>
          <text class="link" @click="loginType='register'">立即注册</text>
        </view>
      </view>

      <view v-if="loginType === 'register'">
        <view class="input-group">
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-group">
          <input class="input" v-model="password" type="password" placeholder="请输入密码" />
        </view>
        <view class="input-group">
          <input class="input" v-model="nickname" placeholder="昵称（选填）" />
        </view>
        <view class="input-group">
          <input class="input" v-model="plateNumber" placeholder="车牌号（选填）" />
        </view>
        <view class="captcha-row">
          <input class="input captcha-input" v-model="captchaCode" placeholder="验证码" />
          <image v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="loadCaptcha" />
        </view>
        <button class="login-btn" @click="handleRegister" :loading="loading">注册</button>
        <view class="switch-row">
          <text>已有账号？</text>
          <text class="link" @click="loginType='phone'">去登录</text>
        </view>
      </view>

      <view class="divider"><text>其他登录方式</text></view>
      <button class="wechat-btn" @click="handleWechatLogin">微信一键登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const loginType = ref('phone')
const phone = ref('')
const password = ref('')
const nickname = ref('')
const plateNumber = ref('')
const captchaCode = ref('')
const captchaImg = ref('')
const captchaKey = ref('')
const loading = ref(false)

async function loadCaptcha() {
  const res = await get('/auth/captcha')
  captchaKey.value = res.data.key
  captchaImg.value = res.data.image
}

async function handleLogin() {
  if (!phone.value || !password.value) {
    uni.showToast({ title: '请输入手机号和密码', icon: 'none' })
    return
  }
  if (!captchaCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await post('/auth/login', {
      phone: phone.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value
    })
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.data))
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
  } catch (e) {
    loadCaptcha()
    captchaCode.value = ''
  } finally { loading.value = false }
}

async function handleRegister() {
  if (!phone.value || !password.value) {
    uni.showToast({ title: '请输入手机号和密码', icon: 'none' })
    return
  }
  if (!captchaCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await post('/auth/register', {
      phone: phone.value,
      password: password.value,
      nickname: nickname.value,
      plateNumber: plateNumber.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value
    })
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.data))
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
  } catch (e) {
    loadCaptcha()
    captchaCode.value = ''
  } finally { loading.value = false }
}

function handleWechatLogin() {
  uni.login({
    provider: 'weixin',
    success: async (loginRes) => {
      try {
        const res = await post('/auth/wechat-login', { code: loginRes.code })
        uni.setStorageSync('token', res.data.token)
        uni.setStorageSync('userInfo', JSON.stringify(res.data))
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
      } catch (e) { /* failed */ }
    },
    fail: () => {
      uni.showToast({ title: '微信登录失败', icon: 'none' })
    }
  })
}

onMounted(() => { loadCaptcha() })
</script>

<style scoped>
.login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea, #764ba2); padding: 40rpx; }
.login-card { width: 100%; max-width: 600rpx; background: #fff; border-radius: 24rpx; padding: 60rpx 40rpx; }
.app-logo { text-align: center; font-size: 80rpx; margin-bottom: 10rpx; }
.app-name { text-align: center; font-size: 40rpx; font-weight: bold; color: #333; }
.app-desc { text-align: center; font-size: 26rpx; color: #999; margin-bottom: 40rpx; }
.input-group { margin-bottom: 24rpx; }
.input { width: 100%; height: 88rpx; border: 2rpx solid #e6e6e6; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; box-sizing: border-box; }
.captcha-row { display: flex; gap: 16rpx; align-items: center; margin-bottom: 24rpx; }
.captcha-input { flex: 1; }
.captcha-img { width: 200rpx; height: 80rpx; border-radius: 8rpx; border: 1rpx solid #ddd; }
.login-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #007AFF; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; margin-top: 20rpx; }
.switch-row { text-align: center; margin-top: 24rpx; font-size: 26rpx; color: #666; }
.link { color: #007AFF; margin-left: 8rpx; }
.divider { text-align: center; margin: 40rpx 0 20rpx; position: relative; }
.divider text { background: #fff; padding: 0 20rpx; color: #999; font-size: 24rpx; position: relative; z-index: 1; }
.wechat-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #07C160; color: #fff; border: none; border-radius: 12rpx; font-size: 28rpx; }
</style>
