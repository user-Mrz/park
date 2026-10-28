<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPaymentRecords } from '@/api'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** 扣费类型：与后端 t_payment_record.type 取值一致 */
const TYPE_META: Record<string, { label: string; type: TagType }> = {
  auto_deduct: { label: '停车自动扣费', type: 'info' },
  reservation_settle: { label: '预约结算', type: 'primary' },
  admin_force: { label: '管理员强制出场', type: 'warning' },
  monthly_pass: { label: '月卡开通/续费', type: 'success' }
}
const typeMeta = (t: string): { label: string; type: TagType } =>
  TYPE_META[t] || { label: t || '未知', type: 'info' }

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
    const res: any = await getPaymentRecords({ page: page.value, pageSize: pageSize.value })
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
      <template #header>扣费记录</template>
      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="关联记录" width="120">
          <template #default="{ row }">
            {{ row.vehicleRecordId ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="扣费(元)" width="100" />
        <el-table-column prop="balanceBefore" label="扣前余额" width="100" />
        <el-table-column prop="balanceAfter" label="扣后余额" width="100" />
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            <el-tag :type="typeMeta(row.type).type">{{ typeMeta(row.type).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="扣费时间" width="170" />
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

    <!-- 扣费详情弹窗 -->
    <el-dialog v-model="detailVisible" title="扣费记录详情" width="540px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="关联记录">{{ detail.vehicleRecordId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="扣费金额(元)">{{ detail.amount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="扣前余额">{{ detail.balanceBefore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="扣后余额">{{ detail.balanceAfter ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="扣费类型" :span="2">
          <el-tag :type="typeMeta(detail.type).type">{{ typeMeta(detail.type).label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="扣费时间" :span="2">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 容器与分页样式复用 src/styles/global.css 的 .page-container / .pagination-wrap */
</style>
