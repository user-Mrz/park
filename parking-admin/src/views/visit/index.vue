<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getVisitReports,
  getVisitReportStats,
  approveVisitReport,
  rejectVisitReport,
  checkInVisit,
  checkOutVisit
} from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

/** 状态元数据：键与后端 status 一致（0~6），color 沿用原 antd Tag 颜色，模板中经 tagType 映射为 el-tag 类型 */
const STATUS_META: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'processing' },
  1: { text: '已通过', color: 'success' },
  2: { text: '已驳回', color: 'error' },
  3: { text: '已入场', color: 'warning' },
  4: { text: '已完成', color: 'default' },
  5: { text: '已过期', color: 'default' },
  6: { text: '已撤销', color: 'default' }
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

/** antd Tag 颜色 → Element Plus el-tag type（processing/blue/cyan→primary、error/red→danger、default→info） */
const TAG_TYPE: Record<string, TagType> = {
  processing: 'primary',
  blue: 'primary',
  cyan: 'primary',
  success: 'success',
  warning: 'warning',
  gold: 'warning',
  orange: 'warning',
  error: 'danger',
  red: 'danger',
  default: 'info'
}
const tagType = (color: string): TagType => TAG_TYPE[color] || 'info'

const statusMeta = (v: number) => STATUS_META[v] || { text: '未知', color: 'default' }

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

const loading = ref(false)
const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const status = ref<number | undefined>(undefined)
const keyword = ref('')
const stats = ref<any>({})
const remarkOpen = ref(false)
const remarkType = ref<'approve' | 'reject'>('approve')
const target = ref<any>(null)
const remark = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)

const load = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value, keyword: keyword.value }
    if (status.value !== undefined) params.status = status.value
    const [list, st]: any[] = await Promise.all([getVisitReports(params), getVisitReportStats()])
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    stats.value = st.data || {}
  } finally {
    if (!silent) loading.value = false
  }
}

load()

/** 与原 React 版一致：关键词变化即重新查询（原 useEffect 依赖 load(keyword) 的原有效果） */
watch(keyword, () => {
  load()
})

/** 访客报备自动刷新：30 秒一次（待审核报备自动出现） */
useAutoRefresh(load, { intervalMs: 30000 })

const statusOptions = Object.keys(STATUS_META).map((k) => ({
  value: Number(k),
  label: STATUS_META[Number(k)].text
}))

const statCards = computed(() => [
  { label: '待审核', value: stats.value.pending ?? 0, color: '#409EFF' },
  { label: '已通过', value: stats.value.approved ?? 0, color: '#67C23A' },
  { label: '在停访客/家长', value: stats.value.parked ?? 0, color: '#E6A23C' },
  { label: '已完成', value: stats.value.finished ?? 0, color: '#606266' },
  { label: '访客区空闲车位', value: stats.value.visitorZoneFree ?? 0, color: '#722ED1' },
  { label: '家长免费时长', value: `${stats.value.parentFreeMinutes ?? 30} 分钟`, color: '#13C2C2' }
])

const onSearch = () => {
  page.value = 1
  load()
}

const onStatusChange = (v: any) => {
  page.value = 1
  status.value = v === '' || v === null ? undefined : v
  load()
}

const onPageChange = (p: number) => {
  page.value = p
  load()
}

const openRemark = (row: any, type: 'approve' | 'reject') => {
  target.value = row
  remarkType.value = type
  remark.value = ''
  remarkOpen.value = true
}

const submitRemark = async () => {
  if (!target.value) return
  if (remarkType.value === 'approve') {
    await approveVisitReport(target.value.id, undefined, remark.value)
    ElMessage.success('已通过审核，并自动分配访客区车位')
  } else {
    await rejectVisitReport(target.value.id, remark.value)
    ElMessage.success('已驳回该报备')
  }
  remarkOpen.value = false
  load()
}

const doCheckIn = async (row: any) => {
  await checkInVisit(row.id)
  ElMessage.success('已登记入场')
  load()
}

const doCheckOut = async (row: any) => {
  await checkOutVisit(row.id)
  ElMessage.success('已出场结算（按身份免费时长与费率）')
  load()
}

// 查看详情：直接复用列表行数据
const viewDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}
</script>

<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col v-for="c in statCards" :key="c.label" :xs="12" :sm="8" :xl="4">
        <el-card shadow="never" class="stat-card stat-card-hover">
          <div style="color: #606266; font-size: 13px; margin-bottom: 6px">{{ c.label }}</div>
          <div :style="{ fontSize: '22px', fontWeight: 700, color: c.color }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>访客与家长车辆报备</span>
          <div class="card-extra">
            <el-select
              :model-value="status"
              placeholder="状态筛选"
              clearable
              style="width: 140px"
              @change="onStatusChange"
            >
              <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="搜索车牌/来访人"
              clearable
              style="width: 220px"
              @keyup.enter="onSearch"
            >
              <template #suffix>
                <el-icon class="search-btn" @click="onSearch"><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="load()">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%">
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.visitType === 2" type="primary">家长接送</el-tag>
            <el-tag v-else type="primary">访客</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来访人" width="110">
          <template #default="{ row }">{{ row.visitorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="来访车牌" width="130">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ row.plateNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column label="事由" show-overflow-tooltip>
          <template #default="{ row }">{{ row.purpose || '-' }}</template>
        </el-table-column>
        <el-table-column label="预计时段" width="230">
          <template #default="{ row }">
            {{ fmtTime(row.expectedStart) }} ~ {{ fmtTime(row.expectedEnd) }}
          </template>
        </el-table-column>
        <el-table-column label="报备人" width="170">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="车位" width="90">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(statusMeta(row.status).color)">{{ statusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <div v-if="row.status === 0" class="op-group">
              <el-button link type="primary" size="small" @click="openRemark(row, 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="openRemark(row, 'reject')">驳回</el-button>
            </div>
            <el-popconfirm v-else-if="row.status === 1" title="登记访客车辆入场？" @confirm="doCheckIn(row)">
              <template #reference>
                <el-button link type="primary" size="small">登记入场</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm v-else-if="row.status === 3" title="确认出场并结算费用？" @confirm="doCheckOut(row)">
              <template #reference>
                <el-button link type="primary" size="small">出场结算</el-button>
              </template>
            </el-popconfirm>
            <span v-else style="color: #909399">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="remarkOpen"
      :title="remarkType === 'approve' ? '通过报备（自动分配访客区车位）' : '驳回报备'"
      width="520px"
    >
      <div style="margin-bottom: 12px; color: #606266">
        车牌：<b>{{ target?.plateNumber }}</b>　类型：{{ target?.visitType === 2 ? '家长接送' : '访客' }}
      </div>
      <el-input
        v-model="remark"
        type="textarea"
        :rows="3"
        :placeholder="remarkType === 'approve' ? '审核意见（可选）' : '驳回原因（建议填写）'"
      />
      <template #footer>
        <el-button @click="remarkOpen = false">取消</el-button>
        <el-button :type="remarkType === 'approve' ? 'primary' : 'danger'" @click="submitRemark">
          {{ remarkType === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 访客报备详情弹窗 -->
    <el-dialog v-model="detailVisible" title="访客与家长报备详情" width="580px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="报备ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag type="primary">{{ detail.visitType === 2 ? '家长接送' : '访客' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="来访人">{{ detail.visitorName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="来访车牌">{{ detail.plateNumber || '—' }}</el-descriptions-item>
        <el-descriptions-item label="报备人">{{ detail.ownerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="分配车位">{{ detail.spotCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预计开始" :span="2">{{ fmtTime(detail.expectedStart) }}</el-descriptions-item>
        <el-descriptions-item label="预计结束" :span="2">{{ fmtTime(detail.expectedEnd) }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="tagType(statusMeta(detail.status).color)">{{ statusMeta(detail.status).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="来访事由" :span="2">{{ detail.purpose || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.op-group {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}
.search-btn {
  cursor: pointer;
  color: #606266;
}
.search-btn:hover {
  color: #409eff;
}
</style>
