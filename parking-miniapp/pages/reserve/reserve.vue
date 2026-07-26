<template>
  <view class="container">
    <view class="form-card">
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
const startIndex = ref([0, 0, 0])
const startDisplay = ref('')
const endTime = ref('')
const durationHours = ref(0)
const totalFee = ref(0)
const submitting = ref(false)

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
const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
const startRange = computed(() => [dates, hours])

// Duration text
const durationText = computed(() => {
  const opt = durationOptions.find(o => o.value === durationHours.value)
  return opt ? opt.label : ''
})

function onSpotChange(e) {
  selectedSpot.value = spotList.value[e.detail.value]
  calcFee()
}

function onStartColumnChange(e) {
  // Handle column change for multiSelector
}

function onStartChange(e) {
  const d = new Date(now)
  d.setDate(d.getDate() + e.detail.value[0])
  d.setHours(e.detail.value[1], 0, 0, 0)
  startDisplay.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:00`
  calcFee()
}

async function loadSpots() {
  try {
    const res = await get('/spot/list', { page: 1, pageSize: 100 })
    spotList.value = (res.data.records || []).filter(s => s.status === 0)
  } catch (e) {}
}

function onDurationChange(e) {
  durationHours.value = durationOptions[e.detail.value].value
  if (startDisplay.value) {
    const start = new Date(startDisplay.value.replace(/-/g, '/'))
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
    const start = new Date(startDisplay.value.replace(/-/g, '/'))
    const end = new Date(start.getTime() + durationHours.value * 3600000)
    await post('/reservation/create', {
      spotId: selectedSpot.value.id,
      startTime: start.toISOString().replace('T', ' ').substring(0, 19),
      endTime: end.toISOString().replace('T', ' ').substring(0, 19)
    })
    uni.showToast({ title: '预约成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

import { onMounted } from 'vue'
onMounted(() => { loadSpots() })
</script>

<style scoped>
.container { padding: 30rpx; }
.form-card { background: #fff; border-radius: 16rpx; padding: 30rpx; }
.form-title { font-size: 28rpx; color: #666; margin: 20rpx 0 10rpx; }
.picker-view { height: 88rpx; line-height: 88rpx; background: #f8f8f8; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; color: #333; }
.fee-preview { text-align: center; margin: 30rpx 0; font-size: 30rpx; color: #333; }
.fee-amount { font-size: 48rpx; font-weight: bold; color: #F56C6C; }
.submit-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #007AFF; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; margin-top: 20rpx; }
.submit-btn[disabled] { opacity: 0.5; }
</style>
