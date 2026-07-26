<template>
  <div class="page-container">
    <el-card>
      <template #header><span>扣费记录</span></template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="vehicleRecordId" label="停车记录ID" width="120" />
        <el-table-column prop="amount" label="扣费(元)" width="100" />
        <el-table-column prop="balanceBefore" label="扣前余额" width="100" />
        <el-table-column prop="balanceAfter" label="扣后余额" width="100" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }"><el-tag :type="row.type==='auto_deduct'?'':'warning'">{{ row.type==='auto_deduct'?'自动扣费':'手动操作' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="扣费时间" width="170" />
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPaymentRecords } from '@/api'

const loading = ref(false), tableData = ref([])
const page = ref(1), pageSize = ref(10), total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res: any = await getPaymentRecords({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(() => { loadData() })
</script>
