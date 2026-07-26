<template>
  <view class="container">
    <!-- Header -->
    <view class="header">
      <view class="header-title">停车场管理系统</view>
      <view class="header-balance" v-if="userInfo">
        余额：<text class="balance-amount">¥{{ userInfo.balance || '0.00' }}</text>
      </view>
    </view>

    <!-- Quick Actions -->
    <view class="actions-grid">
      <view class="action-card" @click="goReserve">
        <view class="action-icon">📅</view>
        <text class="action-text">预约车位</text>
      </view>
      <view class="action-card" @click="goEnter">
        <view class="action-icon">🚗</view>
        <text class="action-text">进入停车场</text>
      </view>
      <view class="action-card" @click="goExit">
        <view class="action-icon">🏁</view>
        <text class="action-text">离开停车场</text>
      </view>
      <view class="action-card" @click="goRecharge">
        <view class="action-icon">💰</view>
        <text class="action-text">账户充值</text>
      </view>
    </view>

    <!-- Current Parking Status -->
    <view class="section" v-if="currentParking">
      <view class="section-title">当前停车</view>
      <view class="parking-card">
        <view class="parking-row">
          <text class="label">车牌号</text>
          <text class="value">{{ currentParking.plateNumber }}</text>
        </view>
        <view class="parking-row">
          <text class="label">车位</text>
          <text class="value">{{ currentParking.spotId }}</text>
        </view>
        <view class="parking-row">
          <text class="label">入场时间</text>
          <text class="value">{{ currentParking.entryTime }}</text>
        </view>
        <view class="parking-row">
          <text class="label">已停时长</text>
          <text class="value highlight">{{ formatDuration(currentParking.entryTime) }}</text>
        </view>
      </view>
    </view>

    <!-- Upcoming Reservations -->
    <view class="section">
      <view class="section-title">近期预约</view>
      <view v-if="reservations.length === 0" class="empty">暂无预约记录</view>
      <view class="reserve-card" v-for="r in reservations" :key="r.id">
        <view class="reserve-header">
          <text class="reserve-no">{{ r.reservationNo }}</text>
          <text class="reserve-status" :class="'status-' + r.status">{{ statusText(r.status) }}</text>
        </view>
        <view class="reserve-detail">
          <text>车位: {{ r.spotId }} | {{ r.startTime }} ~ {{ r.endTime }}</text>
        </view>
        <view class="reserve-detail">
          <text>金额: ¥{{ r.totalAmount }}</text>
        </view>
      </view>
    </view>

    <!-- Available Spots -->
    <view class="section">
      <view class="section-title">空闲车位</view>
      <view class="spots-grid" v-if="spots.length > 0">
        <view class="spot-item" v-for="s in spots" :key="s.id" :class="'spot-' + s.status">
          <text class="spot-code">{{ s.spotCode }}</text>
          <text class="spot-price">¥{{ s.pricePerHour }}/h</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onShow } from 'vue'
import { get, post } from '@/utils/request'

const userInfo = ref(null)
const currentParking = ref(null)
const reservations = ref([])
const spots = ref([])

function statusText(s) {
  const map = { 0: '待确认', 1: '进行中', 2: '已完成', 3: '已取消', 4: '已过期' }
  return map[s] || '未知'
}

function formatDuration(entryTime) {
  if (!entryTime) return '--'
  const start = new Date(entryTime.replace(/-/g, '/')).getTime()
  const now = Date.now()
  const diff = Math.floor((now - start) / 1000 / 60)
  if (diff < 60) return diff + '分钟'
  const hours = Math.floor(diff / 60)
  return hours + '小时' + (diff % 60) + '分钟'
}

function goReserve() {
  const token = uni.getStorageSync('token')
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return }
  uni.navigateTo({ url: '/pages/reserve/reserve' })
}

function goEnter() {
  const token = uni.getStorageSync('token')
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return }
  uni.navigateTo({ url: '/pages/enter/enter' })
}

function goExit() {
  const token = uni.getStorageSync('token')
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return }
  if (!currentParking.value) {
    uni.showToast({ title: '当前没有停车记录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/exit/exit?id=${currentParking.value.id}` })
}

function goRecharge() {
  const token = uni.getStorageSync('token')
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return }
  uni.navigateTo({ url: '/pages/recharge/recharge' })
}

async function loadData() {
  const token = uni.getStorageSync('token')
  if (!token) return
  try {
    const infoRes = await get('/user/info')
    userInfo.value = infoRes.data
    const currentRes = await get('/vehicle/current')
    currentParking.value = currentRes.data
    const resRes = await get('/user/reservations', { page: 1, pageSize: 5 })
    reservations.value = (resRes.data && resRes.data.records) || []
    const spotRes = await get('/spot/list', { page: 1, pageSize: 20 })
    spots.value = (spotRes.data && spotRes.data.records) ? spotRes.data.records.filter(s => s.status === 0).slice(0, 10) : []
  } catch (e) {
    // Silently fail for non-logged-in users
  }
}

onShow(() => { loadData() })
</script>

<style scoped>
.container { padding: 0 0 30rpx; }
.header { background: linear-gradient(135deg, #007AFF, #0055CC); padding: 40rpx 30rpx 50rpx; color: #fff; }
.header-title { font-size: 40rpx; font-weight: bold; margin-bottom: 10rpx; }
.header-balance { font-size: 28rpx; opacity: 0.9; }
.balance-amount { font-weight: bold; }
.actions-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; padding: 30rpx; margin-top: -30rpx; }
.action-card { background: #fff; border-radius: 16rpx; padding: 30rpx; text-align: center; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.08); }
.action-icon { font-size: 48rpx; margin-bottom: 10rpx; }
.action-text { font-size: 26rpx; color: #333; font-weight: 500; }
.section { margin: 20rpx; }
.section-title { font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; color: #333; }
.parking-card { background: #fff; border-radius: 16rpx; padding: 30rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06); }
.parking-row { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 28rpx; }
.parking-row .label { color: #999; }
.parking-row .value { color: #333; }
.parking-row .highlight { color: #E6A23C; font-weight: bold; }
.reserve-card { background: #fff; border-radius: 12rpx; padding: 20rpx 30rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); }
.reserve-header { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.reserve-no { font-size: 26rpx; color: #666; }
.reserve-detail { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.status-0, .status-4 { color: #909399; }
.status-1 { color: #E6A23C; }
.status-2 { color: #67C23A; }
.status-3 { color: #F56C6C; }
.spots-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.spot-item { background: #fff; border-radius: 12rpx; padding: 20rpx; width: calc(25% - 12rpx); text-align: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.spot-code { display: block; font-size: 26rpx; font-weight: bold; color: #333; }
.spot-price { display: block; font-size: 22rpx; color: #F56C6C; margin-top: 4rpx; }
.empty { text-align: center; color: #999; padding: 40rpx 0; font-size: 26rpx; }
</style>
