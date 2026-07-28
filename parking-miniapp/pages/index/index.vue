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

    <!-- 正在停车（全部车辆） -->
    <view class="section" v-if="parkedVehicles.length > 0">
      <view class="section-title">正在停车（{{ parkedVehicles.length }}辆）</view>
      <view class="parking-card" v-for="pv in parkedVehicles" :key="pv.recordId">
        <view class="parking-row">
          <text class="label">车牌号</text>
          <text class="value bold">{{ pv.plateNumber }}</text>
        </view>
        <view class="parking-row">
          <text class="label">车位</text>
          <text class="value">{{ pv.spotCode }}</text>
        </view>
        <view class="parking-row">
          <text class="label">用户</text>
          <text class="value">{{ pv.nickname }}{{ pv.phone ? ' ' + pv.phone.substring(7) : '' }}</text>
        </view>
        <view class="parking-row">
          <text class="label">入场时间</text>
          <text class="value">{{ pv.entryTime }}</text>
        </view>
        <view class="parking-row">
          <text class="label">已停时长</text>
          <text class="value highlight">{{ formatDuration(pv.entryTime) }}</text>
        </view>
      </view>
    </view>
    <view class="section" v-else>
      <view class="section-title">正在停车</view>
      <view class="empty">暂无车辆停放</view>
    </view>

    <!-- 全部预约 -->
    <view class="section">
      <view class="section-title">全部预约（{{ activeReservations.length }}个）</view>
      <view v-if="activeReservations.length === 0" class="empty">暂无预约记录</view>
      <view class="reserve-card" v-for="r in activeReservations" :key="r.id" @click="goReserveDetail(r.id)">
        <view class="reserve-header">
          <text class="reserve-no">{{ r.reservationNo }}</text>
          <text class="reserve-status status-1">进行中</text>
        </view>
        <view class="reserve-detail">
          <text>{{ r.nickname }} · 车位#{{ r.spotId }} · {{ r.startTime }} ~ {{ r.endTime }}</text>
        </view>
        <view class="reserve-detail">
          <text>金额: ¥{{ r.totalAmount }}</text>
        </view>
        <view class="reserve-arrow">›</view>
      </view>
    </view>

    <!-- 全部车位（按区域分组） -->
    <view class="section">
      <view class="section-title">全部车位</view>
      <view class="area-group" v-for="group in areaGroups" :key="group.area">
        <view class="area-title">{{ group.area }}（{{ group.spots.length }}个）</view>
        <view class="spots-grid">
          <view class="spot-item" v-for="s in group.spots" :key="s.id" :class="'spot-status-' + s.status">
            <text class="spot-code">{{ s.spotCode }}</text>
            <text class="spot-price">¥{{ s.pricePerHour }}/h</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onShow } from 'vue'
import { get } from '@/utils/request'

const userInfo = ref(null)
const parkedVehicles = ref([])
const activeReservations = ref([])
const areaGroups = ref([])

function formatDuration(entryTime) {
  if (!entryTime) return '--'
  // iOS 兼容日期解析，兼容空格和 T 分隔符
  const normalized = entryTime.replace('T', ' ')
  const parts = normalized.split(' ')[0].split('-')
  const times = (normalized.split(' ')[1] || '00:00:00').split(':')
  const start = new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], +times[2] || 0).getTime()
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
  uni.navigateTo({ url: '/pages/exit/exit' })
}

function goRecharge() {
  const token = uni.getStorageSync('token')
  if (!token) { uni.navigateTo({ url: '/pages/login/login' }); return }
  uni.navigateTo({ url: '/pages/recharge/recharge' })
}

function goReserveDetail(id) {
  uni.navigateTo({ url: `/pages/reserve-detail/reserve-detail?id=${id}` })
}

async function loadData() {
  // 用户信息（需登录）
  const token = uni.getStorageSync('token')
  if (token) {
    try {
      const infoRes = await get('/user/info')
      userInfo.value = infoRes.data
    } catch (e) { /* ignore */ }
  }
  // 公开概览，失败则回退用 /spot/list
  try {
    const overRes = await get('/spot/overview')
    const data = overRes.data
    parkedVehicles.value = data.parkedVehicles || []
    activeReservations.value = data.activeReservations || []
    areaGroups.value = data.areaGroups || []
  } catch (e) {
    // 概览接口失败时，回退加载车位数据
    try {
      const spotRes = await get('/spot/list', { page: 1, pageSize: 500 })
      const allSpots = (spotRes.data && spotRes.data.records) || []
      const map = {}
      allSpots.forEach(s => {
        const area = s.area || '未分区'
        if (!map[area]) map[area] = []
        map[area].push(s)
      })
      areaGroups.value = Object.keys(map).sort().map(area => ({ area, spots: map[area] }))
    } catch (e2) {
      // ignore
    }
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
.reserve-card { background: #fff; border-radius: 12rpx; padding: 20rpx 30rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); position: relative; }
.reserve-card:active { background: #f8f8f8; }
.reserve-header { display: flex; justify-content: space-between; margin-bottom: 10rpx; padding-right: 30rpx; }
.reserve-no { font-size: 26rpx; color: #666; }
.reserve-detail { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.reserve-arrow { position: absolute; right: 24rpx; top: 50%; transform: translateY(-50%); font-size: 40rpx; color: #ccc; }
.status-0, .status-4 { color: #909399; }
.status-1 { color: #E6A23C; }
.status-2 { color: #67C23A; }
.status-3 { color: #F56C6C; }
.spots-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.spot-item { background: #fff; border-radius: 12rpx; padding: 20rpx; width: calc(25% - 12rpx); text-align: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.spot-status-0 { border-bottom: 4rpx solid #67C23A; }
.spot-status-1 { border-bottom: 4rpx solid #F56C6C; background: #fef0f0; }
.spot-status-2 { border-bottom: 4rpx solid #007AFF; background: #f0f7ff; }
.spot-code { display: block; font-size: 26rpx; font-weight: bold; color: #333; }
.spot-price { display: block; font-size: 22rpx; color: #F56C6C; margin-top: 4rpx; }
.area-group { margin-bottom: 20rpx; }
.area-title { font-size: 26rpx; font-weight: bold; color: #007AFF; margin-bottom: 12rpx; padding-left: 8rpx; border-left: 6rpx solid #007AFF; }
.empty { text-align: center; color: #999; padding: 40rpx 0; font-size: 26rpx; }
</style>
