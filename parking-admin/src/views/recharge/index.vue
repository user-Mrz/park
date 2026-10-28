<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getRechargeRecords } from '@/api'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** 状态元数据：键与后端 status 一致；antd Tag color → el-tag type（warning→warning、success→success、error→danger、default→info） */
const STATUS_TAG: Record<number, { type: TagType; text: string }> = {
  0: { type: 'warning', text: '处理中' },
  1: { type: 'success', text: '成功' },
  2: { type: 'danger', text: '失败' }
}

const statusTag = (status: number): { type: TagType; text: string } =>
  STATUS_TAG[status] || { type: 'info', text: '未知' }

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getRechargeRecords({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())

const onPageChange = (p: number) => {
  page.value = p
  loadData()
}

const onSizeChange = (s: number) => {
  pageSize.value = s
  page.value = 1
  loadData()
}

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')

// 查看详情：直接复用列表行数据
const viewDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>充值记录</template>
      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="amount" label="金额(元)" width="100" />
        <el-table-column prop="payMethod" label="支付方式" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type">{{ statusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="充值时间" width="170" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 充值详情弹窗 -->
    <el-dialog v-model="detailVisible" title="充值记录详情" width="520px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="金额(元)">{{ detail.amount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payMethod || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="statusTag(detail.status).type">{{ statusTag(detail.status).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="充值时间" :span="2">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 容器与分页样式复用 src/styles/global.css 的 .page-container / .pagination-wrap */
</style>
