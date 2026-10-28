<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getReservations, updateReservationStatus } from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** 状态元数据：键与后端 status 一致；antd Tag color → el-tag type（default→info、warning→warning、success→success、error→danger） */
const STATUS_TAG: Record<number, { type: TagType; text: string }> = {
  0: { type: 'info', text: '待确认' },
  1: { type: 'warning', text: '进行中' },
  2: { type: 'success', text: '已完成' },
  3: { type: 'danger', text: '已取消' },
  4: { type: 'info', text: '已过期' }
}

const statusTag = (status: number): { type: TagType; text: string } =>
  STATUS_TAG[status] || { type: 'info', text: '未知' }

const STATUS_OPTIONS = [
  { label: '待确认', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 },
  { label: '已过期', value: 4 }
]

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref<number | undefined>(undefined)

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)

// 搜索条件
const searchReservationNo = ref('')
const searchUserId = ref('')
const searchSpotId = ref('')

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (filterStatus.value !== undefined) params.status = filterStatus.value
    // 预约编号：模糊匹配
    const no = searchReservationNo.value.trim()
    if (no) params.reservationNo = no
    // 用户ID：必须能转成有效正数才传
    const uid = searchUserId.value.trim()
    if (uid) {
      const uidNum = Number(uid)
      if (!Number.isNaN(uidNum) && uidNum > 0) params.userId = uidNum
    }
    // 车位ID：同上
    const sid = searchSpotId.value.trim()
    if (sid) {
      const sidNum = Number(sid)
      if (!Number.isNaN(sidNum) && sidNum > 0) params.spotId = sidNum
    }
    const res: any = await getReservations(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => loadData())

/** 预约列表自动刷新：30 秒一次 */
useAutoRefresh(loadData, { intervalMs: 30000 })

const cancelReservation = async (row: any) => {
  await updateReservationStatus(row.id, 3)
  ElMessage.success('已取消')
  loadData()
}

const onStatusChange = (v: any) => {
  page.value = 1
  filterStatus.value = v === '' || v === null || v === undefined ? undefined : v
  loadData()
}

const onSearch = () => {
  page.value = 1
  loadData()
}

const onReset = () => {
  searchReservationNo.value = ''
  searchUserId.value = ''
  searchSpotId.value = ''
  filterStatus.value = undefined
  page.value = 1
  loadData()
}

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
      <template #header>
        <div class="page-header">
          <span>预约管理</span>
          <div class="card-extra">
            <el-select
              :model-value="filterStatus"
              placeholder="筛选状态"
              clearable
              style="width: 140px"
              @change="onStatusChange"
            >
              <el-option v-for="o in STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="search-bar">
        <el-input
          v-model="searchReservationNo"
          placeholder="预约编号"
          clearable
          style="width: 180px"
          @keyup.enter="onSearch"
        />
        <el-input
          v-model="searchUserId"
          placeholder="用户ID"
          clearable
          style="width: 140px"
          @keyup.enter="onSearch"
        />
        <el-input
          v-model="searchSpotId"
          placeholder="车位ID"
          clearable
          style="width: 140px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" :icon="'Search'" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <span class="search-hint">支持任意一项独立搜索，无需全部填写</span>
      </div>
      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column prop="reservationNo" label="预约编号" width="170" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="spotId" label="车位ID" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="durationHours" label="时长(h)" width="80" />
        <el-table-column prop="totalAmount" label="金额(元)" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type">{{ statusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" size="small" type="danger" @click="cancelReservation(row)">
              取消预约
            </el-button>
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

    <!-- 预约详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="预约ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="预约编号">{{ detail.reservationNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="车位ID">{{ detail.spotId }}</el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">{{ fmtTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">{{ fmtTime(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="时长(小时)">{{ detail.durationHours ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="金额(元)">{{ detail.totalAmount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="statusTag(detail.status).type">{{ statusTag(detail.status).text }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-extra {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.search-hint {
  font-size: 12px;
  color: #909399;
}
</style>
