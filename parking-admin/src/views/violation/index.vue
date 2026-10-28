<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getViolations, getViolationStats, handleViolation } from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

/** antd Tag 颜色 → Element Plus el-tag type（orange/gold→warning、default→info） */
const TAG_TYPE: Record<string, string> = {
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
const tagType = (color: string) => TAG_TYPE[color] || 'info'

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

const loading = ref(false)
const rows = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const status = ref<number | undefined>(undefined)
const keyword = ref('')
const stats = ref<any>({})
const target = ref<any>(null)
const remark = ref('')
const open = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)

const load = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value, keyword: keyword.value }
    if (status.value !== undefined) params.status = status.value
    const [list, st]: any[] = await Promise.all([getViolations(params), getViolationStats()])
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

/** 违规记录自动刷新：30 秒一次（新违规自动出现） */
useAutoRefresh(load, { intervalMs: 30000 })

const statCards = computed(() => [
  { label: '违规总数', value: stats.value.total ?? 0, color: '#1d2129' },
  { label: '待处理', value: stats.value.pending ?? 0, color: '#F56C6C' },
  { label: '已处理', value: stats.value.handled ?? 0, color: '#67C23A' }
])

const statusOptions = [
  { value: 0, label: '未处理' },
  { value: 1, label: '已处理' }
]

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

const openHandle = (row: any) => {
  target.value = row
  remark.value = ''
  open.value = true
}

const submit = async () => {
  if (!target.value) return
  await handleViolation(target.value.id, remark.value)
  ElMessage.success('已处理')
  open.value = false
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
      <el-col v-for="c in statCards" :key="c.label" :xs="24" :sm="8">
        <el-card shadow="never" class="stat-card stat-card-hover">
          <div style="color: #606266; font-size: 13px; margin-bottom: 6px">{{ c.label }}</div>
          <div :style="{ fontSize: '22px', fontWeight: 700, color: c.color }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>校园违规记录（出场时超时自动生成）</span>
          <div class="card-extra">
            <el-select
              :model-value="status"
              placeholder="状态筛选"
              clearable
              style="width: 130px"
              @change="onStatusChange"
            >
              <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="搜索车牌"
              clearable
              style="width: 200px"
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

      <el-table v-loading="loading" :data="rows" row-key="id" size="default">
        <el-table-column label="车牌号" width="130">
          <template #default="{ row }">
            <b>{{ row.plateNumber || '-' }}</b>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'overtime'" type="warning">超时占用</el-tag>
            <el-tag v-else type="info">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="description" show-overflow-tooltip />
        <el-table-column label="扣分" prop="points" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">已处理</el-tag>
            <el-tag v-else type="danger">未处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="记录时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <span v-if="row.status === 1" style="color: #909399">—</span>
            <el-button v-else link type="primary" size="small" @click="openHandle(row)">处理</el-button>
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

    <el-dialog v-model="open" title="处理违规记录" width="520px">
      <div style="margin-bottom: 12px; color: #606266">
        车牌：<b>{{ target?.plateNumber }}</b>　{{ target?.description }}
      </div>
      <el-input
        v-model="remark"
        type="textarea"
        :rows="3"
        placeholder="处理意见（可选，例如：已电话通知车主）"
      />
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submit">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 违规详情弹窗 -->
    <el-dialog v-model="detailVisible" title="违规记录详情" width="520px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ detail.plateNumber || '—' }}</el-descriptions-item>
        <el-descriptions-item label="违规类型">
          <el-tag v-if="detail.type === 'overtime'" type="warning">超时占用</el-tag>
          <el-tag v-else type="info">{{ detail.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="扣分">{{ detail.points ?? 0 }} 分</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="记录时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="违规说明" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
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
.search-btn {
  cursor: pointer;
  color: #606266;
}
.search-btn:hover {
  color: #409eff;
}
</style>
