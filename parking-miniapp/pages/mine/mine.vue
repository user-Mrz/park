<template>
  <view class="container">
    <!-- User Info -->
    <view class="user-card">
      <view class="avatar">👤</view>
      <view class="user-info" v-if="isLoggedIn">
        <text class="nickname">{{ userInfo?.nickname || '用户' }}</text>
        <text class="phone">{{ userInfo?.phone || '' }}</text>
      </view>
      <view class="user-info" v-else>
        <text class="nickname">未登录</text>
        <text class="phone">点击登录使用更多功能</text>
      </view>
    </view>

    <view class="balance-bar" v-if="isLoggedIn">
      <text>账户余额</text>
      <text class="balance-num">¥{{ userInfo?.balance || '0.00' }}</text>
      <text class="recharge-link" @click="goRecharge">充值</text>
    </view>

    <!-- Menu Items -->
    <view class="menu-list">
      <view class="menu-item" @click="goPage('/pages/reserve/reserve')">
        <text>📅 预约车位</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/enter/enter')">
        <text>🚗 车辆入场</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/exit/exit')">
        <text>🏁 车辆离场</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/record/record')">
        <text>📋 停车记录</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/record/record?tab=1')">
        <text>💰 充值记录</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="menu-list" style="margin-top: 20rpx;">
      <view class="menu-item" @click="handleLogout" v-if="isLoggedIn">
        <text style="color:#F56C6C">🚪 退出登录</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/login/login')" v-else>
        <text style="color:#007AFF">🔑 登录/注册</text>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onShow } from 'vue'
import { get } from '@/utils/request'

const userInfo = ref(null)
const isLoggedIn = computed(() => !!userInfo.value)

function goPage(e) {
  // UniApp 编译后传事件对象，开发模式直接传字符串
  const url = typeof e === 'string' ? e : (e.currentTarget?.dataset?.url || '')
  if (!url) return
  const token = uni.getStorageSync('token')
  if (!token && url !== '/pages/login/login') {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  uni.navigateTo({ url })
}

function goRecharge() {
  uni.navigateTo({ url: '/pages/recharge/recharge' })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success(res) {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        userInfo.value = null
        uni.showToast({ title: '已退出', icon: 'none' })
      }
    }
  })
}

async function fetchUserInfo() {
  const token = uni.getStorageSync('token')
  if (!token) {
    userInfo.value = null
    return
  }
  try {
    const res = await get('/user/info')
    if (res.data) {
      userInfo.value = res.data
      // Also update localStorage for other pages
      uni.setStorageSync('userInfo', JSON.stringify(res.data))
    }
  } catch (e) {
    // Fallback to cached data on network error
    try {
      const stored = uni.getStorageSync('userInfo')
      if (stored) userInfo.value = JSON.parse(stored)
    } catch (e2) { userInfo.value = null }
  }
}

onShow(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.container { padding-bottom: 30rpx; }
.user-card { background: linear-gradient(135deg, #667eea, #764ba2); padding: 40rpx 30rpx; display: flex; align-items: center; gap: 24rpx; margin-bottom: 0; }
.avatar { width: 100rpx; height: 100rpx; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48rpx; }
.nickname { display: block; font-size: 36rpx; color: #fff; font-weight: bold; }
.phone { display: block; font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 6rpx; }
.balance-bar { display: flex; align-items: center; background: #fff; padding: 24rpx 30rpx; font-size: 28rpx; color: #333; margin-bottom: 20rpx; }
.balance-num { flex: 1; font-size: 32rpx; font-weight: bold; color: #333; text-align: center; }
.recharge-link { color: #007AFF; font-size: 26rpx; }
.menu-list { background: #fff; border-radius: 12rpx; margin: 0 20rpx; overflow: hidden; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 30rpx; font-size: 30rpx; border-bottom: 1rpx solid #f5f5f5; }
.menu-item:last-child { border-bottom: none; }
.arrow { font-size: 36rpx; color: #ccc; }
</style>
