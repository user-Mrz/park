<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>预约管理</span>
          <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width:140px" @change="loadData">
            <el-option label="待确认" :value="0" /><el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" /><el-option label="已取消" :value="3" /><el-option label="已过期" :value="4" />
          </el-select>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="reservationNo" label="预约编号" width="170" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="spotId" label="车位ID" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="durationHours" label="时长(h)" width="80" />
        <el-table-column prop="totalAmount" label="金额(元)" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status===0" type="info">待确认</el-tag>
            <el-tag v-else-if="row.status===1" type="warning">进行中</el-tag>
            <el-tag v-else-if="row.status===2" type="success">已完成</el-tag>
            <el-tag v-else-if="row.status===3" type="danger">已取消</el-tag>
            <el-tag v-else type="info">已过期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status===1" size="small" type="danger" @click="cancelReservation(row)">取消预约</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getReservations, updateReservationStatus } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false), tableData = ref([])
const page = ref(1), pageSize = ref(10), total = ref(0)
const filterStatus = ref<number | undefined>(undefined)

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (filterStatus.value !== undefined && filterStatus.value !== '') params.status = filterStatus.value
    const res: any = await getReservations(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function cancelReservation(row: any) {
  await updateReservationStatus(row.id, 3)
  ElMessage.success('已取消')
  loadData()
}

onMounted(() => { loadData() })
</script>
