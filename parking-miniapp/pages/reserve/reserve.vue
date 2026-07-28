<template>
  <view class="container">
    <!-- 加载中 -->
    <view class="status-box" v-if="loading">
      <view class="loading-text">加载车位中...</view>
    </view>

    <!-- 错误提示 -->
    <view class="status-box error" v-else-if="errorMsg">
      <view class="error-text">{{ errorMsg }}</view>
      <button class="retry-btn" @click="loadSpots">点击重试</button>
    </view>

    <!-- 正常表单 -->
    <view class="form-card" v-else>
      <view class="form-title">选择车位</view>
      <picker mode="selector" :range="spotList" range-key="spotCode" @change="onSpotChange">
        <view class="picker-view">{{ selectedSpot ? selectedSpot.spotCode + ' - ¥' + selectedSpot.pricePerHour + '/h' : '请选择车位' }}</view>
      </picker>

      <view class="form-title">开始时间</view>
      <picker mode="multiSelector" :range="startRange" :value="startIndex" @change="onStartChange" @columnchange="onStartColumnChange">
        <view class="picker-view">{{ startDisplay || '请选择开始时间' }}</view>
      </picker>

      <view class="form-title">预约时长</view>
      <picker mode="selector" :range="durationOptions" @change="onDurationChange">
        <view class="picker-view">{{ durationText || '请选择时长' }}</view>
      </picker>

      <view class="form-title" v-if="endTime">结束时间: {{ endTime }}</view>

      <view class="fee-preview" v-if="totalFee > 0">
        <text>预计费用：</text>
        <text class="fee-amount">¥{{ totalFee.toFixed(2) }}</text>
      </view>

      <button class="submit-btn" @click="handleReserve" :loading="submitting" :disabled="!canSubmit">
        确认预约
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { get, post } from '@/utils/request'

const spotList = ref([])
const selectedSpot = ref(null)
const startIndex = ref([0, 0])
const startDisplay = ref('')
const endTime = ref('')
const durationHours = ref(0)
const totalFee = ref(0)
const submitting = ref(false)
const loading = ref(true)
const errorMsg = ref('')

const durationOptions = [
  { label: '1小时', value: 1 }, { label: '2小时', value: 2 }, { label: '3小时', value: 3 },
  { label: '4小时', value: 4 }, { label: '6小时', value: 6 }, { label: '8小时', value: 8 },
  { label: '12小时', value: 12 }, { label: '1天', value: 24 }, { label: '2天', value: 48 },
  { label: '3天', value: 72 }, { label: '1周', value: 168 }, { label: '2周', value: 336 },
  { label: '1个月', value: 720 }
]

const canSubmit = computed(() => selectedSpot.value && startDisplay.value && durationHours.value > 0)

const now = new Date()
const dates = []
for (let i = 0; i < 14; i++) {
  const d = new Date(now)
  d.setDate(d.getDate() + i)
  dates.push(`${d.getMonth()+1}月${d.getDate()}日`)
}
const hours = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0') + ':00')
const startRange = computed(() => [dates, hours])

// Duration text
const durationText = computed(() => {
  const opt = durationOptions.find(o => o.value === durationHours.value)
  return opt ? opt.label : ''
})

// Build start display from indices
function buildStartDisplay() {
  if (startIndex.value[0] >= 0 && startIndex.value[1] >= 0) {
    const d = new Date(now)
    d.setDate(d.getDate() + startIndex.value[0])
    d.setHours(Number(hours[startIndex.value[1]].split(':')[0]), 0, 0, 0)
    startDisplay.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:00`
  }
}

function onSpotChange(e) {
  selectedSpot.value = spotList.value[e.detail.value]
  calcFee()
}

// Track column changes in multiSelector to keep indices in sync
function onStartColumnChange(e) {
  const { column, value } = e.detail
  const newIndex = [...startIndex.value]
  newIndex[column] = value
  startIndex.value = newIndex
}

function onStartChange(e) {
  startIndex.value = e.detail.value
  buildStartDisplay()
  calcFee()
}

async function loadSpots() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await get('/spot/list', { page: 1, pageSize: 100 })
    // Show vacant (status 0) spots only for new reservations
    spotList.value = (res.data.records || []).filter(s => s.status === 0)
    if (spotList.value.length === 0) {
      errorMsg.value = '暂无可预约车位'
    }
  } catch (e) {
    errorMsg.value = '加载车位失败，请检查网络'
  } finally {
    loading.value = false
  }
}

function onDurationChange(e) {
  durationHours.value = durationOptions[e.detail.value].value
  if (startDisplay.value) {
    // 用构造函数避免 iOS 日期字符串解析兼容问题
    const parts = startDisplay.value.split(' ')[0].split('-')
    const times = startDisplay.value.split(' ')[1].split(':')
    const start = new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], 0)
    const end = new Date(start.getTime() + durationHours.value * 3600000)
    endTime.value = `${end.getFullYear()}-${String(end.getMonth()+1).padStart(2,'0')}-${String(end.getDate()).padStart(2,'0')} ${String(end.getHours()).padStart(2,'0')}:00`
  }
  calcFee()
}

function calcFee() {
  if (selectedSpot.value && durationHours.value > 0) {
    totalFee.value = selectedSpot.value.pricePerHour * durationHours.value
  }
}

async function handleReserve() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    // 用构造函数避免 iOS 日期字符串解析兼容问题
    const parts = startDisplay.value.split(' ')[0].split('-')
    const times = startDisplay.value.split(' ')[1].split(':')
    const start = new Date(+parts[0], +parts[1] - 1, +parts[2], +times[0], +times[1], 0)
    const end = new Date(start.getTime() + durationHours.value * 3600000)
    const pad = (n) => String(n).padStart(2, '0')
    await post('/reservation/create', {
      spotId: selectedSpot.value.id,
      startTime: `${start.getFullYear()}-${pad(start.getMonth()+1)}-${pad(start.getDate())} ${pad(start.getHours())}:${pad(start.getMinutes())}:00`,
      endTime: `${end.getFullYear()}-${pad(end.getMonth()+1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:00`
    })
    uni.showToast({ title: '预约成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {
    // error already shown by request utility
  } finally {
    submitting.value = false
  }
}

import { onMounted } from 'vue'
onMounted(() => { loadSpots() })
</script>

<style scoped>
.container { padding: 30rpx; }
.status-box { background: #fff; border-radius: 16rpx; padding: 80rpx 30rpx; text-align: center; }
.loading-text { font-size: 28rpx; color: #999; }
.error-text { font-size: 28rpx; color: #F56C6C; margin-bottom: 30rpx; }
.retry-btn { width: 200rpx; height: 64rpx; line-height: 64rpx; background: #007AFF; color: #fff; border-radius: 32rpx; font-size: 26rpx; border: none; }
.form-card { background: #fff; border-radius: 16rpx; padding: 30rpx; }
.form-title { font-size: 28rpx; color: #666; margin: 20rpx 0 10rpx; }
.picker-view { height: 88rpx; line-height: 88rpx; background: #f8f8f8; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; color: #333; }
.fee-preview { text-align: center; margin: 30rpx 0; font-size: 30rpx; color: #333; }
.fee-amount { font-size: 48rpx; font-weight: bold; color: #F56C6C; }
.submit-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #007AFF; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; margin-top: 20rpx; }
.submit-btn[disabled] { opacity: 0.5; }
</style>
