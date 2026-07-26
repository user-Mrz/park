<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>车辆进出管理</span>
          <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width:140px" @change="loadData">
            <el-option label="在场" :value="0" /><el-option label="已离场" :value="1" />
          </el-select>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column prop="spotId" label="车位ID" width="100" />
        <el-table-column prop="entryTime" label="入场时间" width="170" />
        <el-table-column prop="exitTime" label="出场时间" width="170" />
        <el-table-column prop="durationMinutes" label="停留(分)" width="80" />
        <el-table-column prop="totalFee" label="费用(元)" width="100" />
        <el-table-column prop="paymentStatus" label="支付" width="80">
          <template #default="{ row }"><el-tag :type="row.paymentStatus===1?'success':'danger'">{{ row.paymentStatus===1?'已付':'未付' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===0?'warning':'info'">{{ row.status===0?'在场':'已离场' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status===0" size="small" type="danger" @click="handleForceExit(row)">强制离场</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getVehicleRecords, forceExit } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false), tableData = ref([])
const page = ref(1), pageSize = ref(10), total = ref(0)
const filterStatus = ref<number | undefined>(undefined)

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (filterStatus.value !== undefined && filterStatus.value !== '') params.status = filterStatus.value
    const res: any = await getVehicleRecords(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function handleForceExit(row: any) {
  try {
    await ElMessageBox.confirm(`确定强制离场车辆 ${row.plateNumber}？`, '确认', { type: 'warning' })
    await forceExit(row.id)
    ElMessage.success('已强制离场')
    loadData()
  } catch (e) {}
}

onMounted(() => { loadData() })
</script>
