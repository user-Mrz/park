<template>
  <div class="page-container">
    <el-card>
      <template #header><span>充值记录</span></template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="amount" label="金额(元)" width="100" />
        <el-table-column prop="payMethod" label="支付方式" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':row.status===0?'warning':'danger'">{{ row.status===1?'成功':row.status===0?'处理中':'失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="充值时间" width="170" />
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRechargeRecords } from '@/api'

const loading = ref(false), tableData = ref([])
const page = ref(1), pageSize = ref(10), total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res: any = await getRechargeRecords({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(() => { loadData() })
</script>
