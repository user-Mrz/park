<template>
  <view class="container">
    <view class="enter-card">
      <view class="icon-area">🚗</view>
      <view class="title">车辆入场</view>
      <view class="desc">确认进入停车场</view>

      <view class="form-item">
        <text class="label">选择车位</text>
        <picker mode="selector" :range="availableSpots" range-key="spotCode" @change="onSpotChange">
          <view class="picker-view">{{ selectedSpot ? selectedSpot.spotCode + ' (' + selectedSpot.area + ')' : '请选择车位' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">车牌号</text>
        <input class="input" v-model="plateNumber" :placeholder="userPlate || '请输入车牌号'" />
      </view>

      <view class="info-row" v-if="selectedSpot">
        <text>费率：¥{{ selectedSpot.pricePerHour }}/小时</text>
      </view>

      <button class="enter-btn" @click="handleEnter" :loading="submitting">
        确认入场
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const availableSpots = ref([])
const selectedSpot = ref(null)
const plateNumber = ref('')
const userPlate = ref('')
const submitting = ref(false)

function onSpotChange(e) {
  selectedSpot.value = availableSpots.value[e.detail.value]
}

async function loadData() {
  try {
    const infoRes = await get('/user/info')
    userPlate.value = infoRes.data.plateNumber || ''
    plateNumber.value = userPlate.value
    const spotRes = await get('/spot/list', { page: 1, pageSize: 100 })
    availableSpots.value = (spotRes.data.records || []).filter(s => s.status === 0)
  } catch (e) {}
}

async function handleEnter() {
  if (!selectedSpot.value) {
    uni.showToast({ title: '请选择车位', icon: 'none' })
    return
  }
  const plate = plateNumber.value || userPlate.value
  if (!plate) {
    uni.showToast({ title: '请输入车牌号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await post('/vehicle/enter', {
      spotId: selectedSpot.value.id,
      plateNumber: plate
    })
    uni.showToast({ title: '入场成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.container { padding: 30rpx; }
.enter-card { background: #fff; border-radius: 16rpx; padding: 50rpx 30rpx; text-align: center; }
.icon-area { font-size: 100rpx; margin-bottom: 10rpx; }
.title { font-size: 36rpx; font-weight: bold; color: #333; margin-bottom: 8rpx; }
.desc { font-size: 26rpx; color: #999; margin-bottom: 40rpx; }
.form-item { text-align: left; margin-bottom: 24rpx; }
.label { display: block; font-size: 28rpx; color: #666; margin-bottom: 10rpx; }
.picker-view { height: 88rpx; line-height: 88rpx; background: #f8f8f8; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; }
.input { width: 100%; height: 88rpx; border: 2rpx solid #e6e6e6; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; box-sizing: border-box; }
.info-row { text-align: left; margin-top: 16rpx; font-size: 26rpx; color: #E6A23C; }
.enter-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #007AFF; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; margin-top: 30rpx; }
</style>
