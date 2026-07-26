<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div>
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
            <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header><span>今日概览</span></template>
          <div class="overview">
            <div class="overview-item">
              <span class="overview-label">今日预约</span>
              <span class="overview-value">{{ dashboard.todayReservations || 0 }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">今日收入</span>
              <span class="overview-value">¥{{ dashboard.todayRevenue || '0.00' }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">在场车辆</span>
              <span class="overview-value">{{ dashboard.parkedVehicles || 0 }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">可用车位</span>
              <span class="overview-value">{{ dashboard.availableSpots || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" style="height: 260px">
          <template #header><span>快速操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/spots')">管理车位</el-button>
            <el-button type="success" @click="$router.push('/reservations')">查看预约</el-button>
            <el-button type="warning" @click="$router.push('/vehicles')">车辆进出</el-button>
            <el-button type="info" @click="$router.push('/config')">费率设置</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDashboard } from '@/api'

const dashboard = reactive<any>({})
const cards = ref([
  { label: '总用户数', value: 0, icon: 'User', color: '#409EFF' },
  { label: '总车位数', value: 0, icon: 'Location', color: '#67C23A' },
  { label: '在场车辆', value: 0, icon: 'Van', color: '#E6A23C' },
  { label: '可用车位', value: 0, icon: 'CircleCheck', color: '#F56C6C' }
])

onMounted(async () => {
  const res: any = await getDashboard()
  Object.assign(dashboard, res.data)
  cards.value[0].value = res.data.totalUsers || 0
  cards.value[1].value = res.data.totalSpots || 0
  cards.value[2].value = res.data.parkedVehicles || 0
  cards.value[3].value = res.data.availableSpots || 0
})
</script>

<style scoped>
.stat-card .stat-content { display: flex; justify-content: space-between; align-items: center; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.overview { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.overview-item { text-align: center; }
.overview-label { display: block; color: #909399; font-size: 14px; margin-bottom: 8px; }
.overview-value { display: block; font-size: 24px; font-weight: bold; color: #303133; }
.quick-actions { display: flex; flex-direction: column; gap: 12px; }
.quick-actions .el-button { width: 100%; }
</style>
