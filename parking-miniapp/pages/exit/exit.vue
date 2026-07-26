<template>
  <view class="container">
    <view class="exit-card">
      <view class="icon-area">🏁</view>
      <view class="title">车辆离场</view>

      <view class="info-box" v-if="currentRecord">
        <view class="info-row"><text class="info-label">车牌号</text><text>{{ currentRecord.plateNumber }}</text></view>
        <view class="info-row"><text class="info-label">入场时间</text><text>{{ currentRecord.entryTime }}</text></view>
        <view class="info-row"><text class="info-label">已停时长</text><text class="highlight">{{ durationText }}</text></view>
        <view class="info-row fee-row"><text class="info-label">预计费用</text><text class="fee">¥{{ estimatedFee.toFixed(2) }}</text></view>
        <view class="info-row"><text class="info-label">账户余额</text><text>¥{{ (userInfo && userInfo.balance) || '0.00' }}</text></view>
      </view>

      <view class="warning" v-if="estimatedFee > (userInfo && userInfo.balance || 0)">
        ⚠️ 余额不足，请先充值再离场
      </view>

      <button class="exit-btn" @click="handleExit" :loading="submitting" :disabled="estimatedFee > (userInfo && userInfo.balance || 0)">
        确认离场并支付
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const currentRecord = ref(null)
const userInfo = ref(null)
const submitting = ref(false)

const durationText = computed(() => {
  if (!currentRecord.value) return '--'
  const start = new Date(currentRecord.value.entryTime.replace(/-/g, '/')).getTime()
  const now = Date.now()
  const diff = Math.floor((now - start) / 1000 / 60)
  if (diff < 60) return diff + '分钟'
  return Math.floor(diff / 60) + '小时' + (diff % 60) + '分钟'
})

const estimatedFee = computed(() => {
  if (!currentRecord.value) return 0
  const start = new Date(currentRecord.value.entryTime.replace(/-/g, '/')).getTime()
  const now = Date.now()
  const hours = Math.ceil((now - start) / 1000 / 3600 * 100) / 100
  // This will be calculated server-side; show rough estimate
  return Math.ceil(hours) * 5
})

async function loadData() {
  try {
    const infoRes = await get('/user/info')
    userInfo.value = infoRes.data
    const res = await get('/vehicle/current')
    currentRecord.value = res.data
  } catch (e) {}
}

async function handleExit() {
  if (!currentRecord.value) {
    uni.showToast({ title: '没有停车记录', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await post(`/vehicle/exit/${currentRecord.value.id}`)
    uni.showToast({ title: '离场成功，已自动扣费', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.container { padding: 30rpx; }
.exit-card { background: #fff; border-radius: 16rpx; padding: 50rpx 30rpx; text-align: center; }
.icon-area { font-size: 100rpx; margin-bottom: 10rpx; }
.title { font-size: 36rpx; font-weight: bold; color: #333; margin-bottom: 30rpx; }
.info-box { text-align: left; margin-bottom: 30rpx; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; font-size: 28rpx; }
.info-label { color: #999; }
.highlight { color: #E6A23C; font-weight: bold; }
.fee-row { margin-top: 10rpx; }
.fee { font-size: 36rpx; font-weight: bold; color: #F56C6C; }
.warning { background: #fef0f0; color: #F56C6C; padding: 16rpx; border-radius: 8rpx; font-size: 26rpx; margin-bottom: 20rpx; }
.exit-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #F56C6C; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; }
.exit-btn[disabled] { opacity: 0.5; }
</style>
