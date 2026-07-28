<template>
  <view class="container">
    <view class="tabs">
      <view class="tab-item" :class="{ active: tab === 0 }" @click="tab=0">停车记录</view>
      <view class="tab-item" :class="{ active: tab === 1 }" @click="tab=1">充值记录</view>
      <view class="tab-item" :class="{ active: tab === 2 }" @click="tab=2">扣费记录</view>
    </view>

    <!-- Vehicle Records -->
    <view v-if="tab === 0">
      <view class="record-card" v-for="r in vehicleRecords" :key="r.id">
        <view class="record-header">
          <text class="plate">{{ r.plateNumber }}</text>
          <text class="status" :class="'vstatus-' + r.status">{{ r.status === 0 ? '在场' : '已离场' }}</text>
        </view>
        <view class="record-row"><text class="rl">入场</text><text>{{ r.entryTime }}</text></view>
        <view class="record-row" v-if="r.exitTime"><text class="rl">离场</text><text>{{ r.exitTime }}</text></view>
        <view class="record-row" v-if="r.totalFee"><text class="rl">费用</text><text class="fee-text">¥{{ r.totalFee }}</text></view>
      </view>
      <view v-if="vehicleRecords.length === 0" class="empty">暂无记录</view>
    </view>

    <!-- Recharge Records -->
    <view v-if="tab === 1">
      <view class="record-card" v-for="r in rechargeRecords" :key="r.id">
        <view class="record-row"><text class="rl">订单号</text><text>{{ r.orderNo }}</text></view>
        <view class="record-row"><text class="rl">金额</text><text class="amount-in">+¥{{ r.amount }}</text></view>
        <view class="record-row"><text class="rl">时间</text><text>{{ r.createdAt }}</text></view>
      </view>
      <view v-if="rechargeRecords.length === 0" class="empty">暂无记录</view>
    </view>

    <!-- Payment Records -->
    <view v-if="tab === 2">
      <view class="record-card" v-for="r in paymentRecords" :key="r.id">
        <view class="record-row"><text class="rl">扣费</text><text class="amount-out">-¥{{ r.amount }}</text></view>
        <view class="record-row"><text class="rl">扣前</text><text>¥{{ r.balanceBefore }}</text></view>
        <view class="record-row"><text class="rl">扣后</text><text>¥{{ r.balanceAfter }}</text></view>
        <view class="record-row"><text class="rl">时间</text><text>{{ r.createdAt }}</text></view>
      </view>
      <view v-if="paymentRecords.length === 0" class="empty">暂无记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { get } from '@/utils/request'

const tab = ref(0)
const vehicleRecords = ref([])
const rechargeRecords = ref([])
const paymentRecords = ref([])

// 从路由参数读取初始 tab
{
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const opts = page.$page?.options || page.options || {}
  if (opts.tab) {
    const t = parseInt(opts.tab)
    if (t >= 0 && t <= 2) tab.value = t
  }
}
// 首次加载
loadTabData()

async function loadVehicleRecords() {
  try {
    const res = await get('/user/vehicle-records', { page: 1, pageSize: 50 })
    vehicleRecords.value = (res.data && res.data.records) || []
  } catch (e) {}
}

async function loadRechargeRecords() {
  try {
    const res = await get('/user/recharge/records', { page: 1, pageSize: 50 })
    rechargeRecords.value = (res.data && res.data.records) || []
  } catch (e) {}
}

async function loadPaymentRecords() {
  try {
    const res = await get('/user/payment/records', { page: 1, pageSize: 50 })
    paymentRecords.value = (res.data && res.data.records) || []
  } catch (e) {}
}

function loadTabData() {
  if (tab.value === 0) loadVehicleRecords()
  else if (tab.value === 1) loadRechargeRecords()
  else loadPaymentRecords()
}

watch(tab, loadTabData)
</script>

<style scoped>
.container { padding: 20rpx; }
.tabs { display: flex; background: #fff; border-radius: 12rpx; margin-bottom: 20rpx; overflow: hidden; }
.tab-item { flex: 1; text-align: center; padding: 24rpx 0; font-size: 28rpx; color: #666; }
.tab-item.active { color: #007AFF; font-weight: bold; border-bottom: 4rpx solid #007AFF; }
.record-card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx; }
.record-header { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.plate { font-size: 30rpx; font-weight: bold; }
.record-row { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 26rpx; }
.rl { color: #999; }
.fee-text { color: #F56C6C; font-weight: bold; }
.amount-in { color: #67C23A; font-weight: bold; }
.amount-out { color: #F56C6C; font-weight: bold; }
.vstatus-0 { color: #E6A23C; }
.vstatus-1 { color: #67C23A; }
.empty { text-align: center; color: #999; padding: 80rpx 0; font-size: 26rpx; }
</style>
