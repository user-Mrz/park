<template>
  <view class="container">
    <view class="recharge-card">
      <view class="balance-section">
        <text class="balance-label">当前余额</text>
        <text class="balance-value">¥{{ (userInfo && userInfo.balance) || '0.00' }}</text>
      </view>

      <view class="amount-title">选择充值金额</view>
      <view class="amount-grid">
        <view class="amount-item" v-for="a in presetAmounts" :key="a" :class="{ active: amount === a && !customAmount }" @click="selectAmount(a)">
          ¥{{ a }}
        </view>
      </view>

      <view class="custom-amount">
        <text class="custom-label">自定义金额</text>
        <input class="custom-input" v-model="customAmount" type="digit" placeholder="输入金额" />
      </view>

      <button class="recharge-btn" @click="handleRecharge" :loading="submitting">
        立即充值 ¥{{ finalAmount.toFixed(2) }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const userInfo = ref(null)
const amount = ref(0)
const customAmount = ref('')
const presetAmounts = [10, 20, 50, 100, 200, 500]
const submitting = ref(false)

const finalAmount = computed(() => {
  if (customAmount.value && Number(customAmount.value) > 0) return Number(customAmount.value)
  return amount.value
})

function selectAmount(val) {
  amount.value = val
  customAmount.value = ''
}

async function loadUser() {
  try { const res = await get('/user/info'); userInfo.value = res.data } catch (e) {}
}

async function handleRecharge() {
  if (finalAmount.value <= 0) {
    uni.showToast({ title: '请选择或输入金额', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await post('/user/recharge', { amount: finalAmount.value, payMethod: 'wechat' })
    uni.showToast({ title: '充值成功', icon: 'success' })
    loadUser()
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onMounted(() => { loadUser() })
</script>

<style scoped>
.container { padding: 30rpx; }
.recharge-card { background: #fff; border-radius: 16rpx; padding: 30rpx; }
.balance-section { text-align: center; padding: 30rpx 0; border-bottom: 1rpx solid #f0f0f0; margin-bottom: 30rpx; }
.balance-label { display: block; font-size: 26rpx; color: #999; margin-bottom: 8rpx; }
.balance-value { font-size: 56rpx; font-weight: bold; color: #333; }
.amount-title { font-size: 28rpx; color: #666; margin-bottom: 16rpx; }
.amount-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16rpx; margin-bottom: 30rpx; }
.amount-item { text-align: center; padding: 24rpx; background: #f8f8f8; border-radius: 12rpx; font-size: 32rpx; color: #333; border: 2rpx solid transparent; }
.amount-item.active { border-color: #007AFF; color: #007AFF; background: #f0f7ff; }
.custom-amount { display: flex; align-items: center; gap: 16rpx; margin-bottom: 30rpx; }
.custom-label { font-size: 26rpx; color: #666; white-space: nowrap; }
.custom-input { flex: 1; height: 72rpx; border: 2rpx solid #e6e6e6; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.recharge-btn { width: 100%; height: 88rpx; line-height: 88rpx; background: #007AFF; color: #fff; border: none; border-radius: 12rpx; font-size: 32rpx; }
</style>
