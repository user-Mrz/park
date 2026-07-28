<template>
  <view class="container">
    <!-- 加载中 -->
    <view class="status-box" v-if="loading">
      <view class="loading-text">加载中...</view>
    </view>

    <!-- 预约不存在 -->
    <view class="status-box error" v-else-if="!reservation">
      <view class="error-text">预约不存在或已失效</view>
      <button class="back-btn" @click="goBack">返回首页</button>
    </view>

    <!-- 预约详情 -->
    <view v-else>
      <!-- 顶部状态卡片 -->
      <view class="status-card" :class="'status-bg-' + reservation.status">
        <view class="status-icon">{{ statusIcon }}</view>
        <view class="status-label">{{ statusText(reservation.status) }}</view>
        <view class="status-hint" v-if="reservation.status === 1">请在预约时间内到达停车场</view>
        <view class="status-hint" v-else-if="reservation.status === 3 || reservation.status === 4">此预约已失效</view>
        <view class="status-hint" v-else-if="reservation.status === 2">预约已完成，感谢使用</view>
      </view>

      <!-- 预约信息 -->
      <view class="info-card">
        <view class="card-title">预约信息</view>
        <view class="info-row">
          <text class="info-label">预约编号</text>
          <text class="info-value">{{ reservation.reservationNo }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">预约车位</text>
          <text class="info-value">#{{ reservation.spotId }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">开始时间</text>
          <text class="info-value">{{ reservation.startTime }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">结束时间</text>
          <text class="info-value">{{ reservation.endTime }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">预约时长</text>
          <text class="info-value">{{ reservation.durationHours }}小时</text>
        </view>
        <view class="info-row fee-row">
          <text class="info-label">预约金额</text>
          <text class="info-value fee">¥{{ reservation.totalAmount }}</text>
        </view>
      </view>

      <!-- 操作按钮（仅进行中的预约显示） -->
      <view class="actions" v-if="reservation.status === 1">
        <button class="action-btn enter-btn" @click="handleEnter" :loading="entering">
          🚗 车辆到场
        </button>
        <button class="action-btn cancel-btn" @click="handleCancel" :loading="cancelling">
          ❌ 取消预约
        </button>
      </view>

      <!-- 底部提示 -->
      <view class="footnote" v-if="reservation.status === 1">
        <text>车辆到场后将自动完成预约并开始计费</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '@/utils/request'

const reservation = ref(null)
const loading = ref(true)
const entering = ref(false)
const cancelling = ref(false)

const statusIcon = computed(() => {
  const map = { 0: '⏳', 1: '✅', 2: '🏁', 3: '❌', 4: '⏰' }
  return map[reservation.value?.status] || '❓'
})

function statusText(s) {
  const map = { 0: '待确认', 1: '进行中', 2: '已完成', 3: '已取消', 4: '已过期' }
  return map[s] || '未知'
}

function goBack() {
  uni.switchTab({ url: '/pages/index/index' })
}

async function loadDetail(id) {
  loading.value = true
  try {
    const res = await get(`/reservation/${id}`)
    reservation.value = res.data
  } catch (e) {
    // 加载失败
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!reservation.value) return
  cancelling.value = true
  try {
    await post(`/reservation/cancel/${reservation.value.id}`)
    uni.showToast({ title: '预约已取消', icon: 'success' })
    reservation.value.status = 3
  } catch (e) {
    // 错误已由 request 工具处理
  } finally {
    cancelling.value = false
  }
}

async function handleEnter() {
  if (!reservation.value) return
  entering.value = true
  try {
    const userRes = await get('/user/info')
    const user = userRes.data
    const plateNumber = user.plateNumber || ''
    if (!plateNumber) {
      entering.value = false
      uni.showModal({
        title: '提示',
        content: '请先在个人中心设置车牌号',
        confirmText: '去设置',
        success(modalRes) {
          if (modalRes.confirm) uni.switchTab({ url: '/pages/mine/mine' })
        }
      })
      return
    }
    await post('/vehicle/enter', {
      spotId: reservation.value.spotId,
      reservationId: reservation.value.id,
      plateNumber: plateNumber
    })
    reservation.value.status = 2
    uni.showToast({ title: '入场成功，预约已完成', icon: 'success' })
  } catch (e) {
    // 错误已由 request 工具处理
  } finally {
    entering.value = false
  }
}

onLoad((options) => {
  const id = options?.id
  if (id) loadDetail(id)
  else loading.value = false
})
</script>

<style scoped>
.container { padding: 0 0 40rpx; }
.status-box { background: #fff; border-radius: 16rpx; padding: 80rpx 30rpx; margin: 30rpx; text-align: center; }
.loading-text { font-size: 28rpx; color: #999; }
.error-text { font-size: 28rpx; color: #F56C6C; margin-bottom: 30rpx; }
.back-btn { width: 240rpx; height: 72rpx; line-height: 72rpx; background: #007AFF; color: #fff; border-radius: 36rpx; font-size: 28rpx; border: none; }

/* 状态卡片 */
.status-card { margin: 30rpx; border-radius: 20rpx; padding: 50rpx 30rpx; text-align: center; color: #fff; }
.status-bg-0 { background: linear-gradient(135deg, #909399, #b0b3b8); }
.status-bg-1 { background: linear-gradient(135deg, #007AFF, #00c6ff); }
.status-bg-2 { background: linear-gradient(135deg, #67C23A, #85ce61); }
.status-bg-3 { background: linear-gradient(135deg, #F56C6C, #f89898); }
.status-bg-4 { background: linear-gradient(135deg, #E6A23C, #ebb563); }
.status-icon { font-size: 72rpx; margin-bottom: 12rpx; }
.status-label { font-size: 40rpx; font-weight: bold; }
.status-hint { font-size: 26rpx; opacity: 0.85; margin-top: 12rpx; }

/* 信息卡片 */
.info-card { background: #fff; border-radius: 16rpx; margin: 0 30rpx 30rpx; padding: 30rpx; box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06); }
.card-title { font-size: 32rpx; font-weight: bold; color: #333; margin-bottom: 24rpx; padding-bottom: 20rpx; border-bottom: 1rpx solid #f0f0f0; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; }
.info-label { color: #999; }
.info-value { color: #333; }
.fee-row { margin-top: 8rpx; padding-top: 20rpx; border-top: 1rpx solid #f0f0f0; }
.fee { font-size: 36rpx; font-weight: bold; color: #F56C6C; }

/* 操作按钮 */
.actions { margin: 0 30rpx; display: flex; flex-direction: column; gap: 20rpx; }
.action-btn { width: 100%; height: 96rpx; line-height: 96rpx; border-radius: 16rpx; font-size: 32rpx; font-weight: bold; border: none; }
.enter-btn { background: linear-gradient(135deg, #007AFF, #00c6ff); color: #fff; }
.cancel-btn { background: #fff; color: #F56C6C; border: 2rpx solid #F56C6C; }
.footnote { text-align: center; padding: 30rpx; font-size: 24rpx; color: #999; }
</style>
