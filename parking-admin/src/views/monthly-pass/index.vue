<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMonthlyPasses, getMonthlyPassStats } from '@/api'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const IDENTITY_LABELS: Record<number, string> = {
  1: '学生', 2: '教师', 3: '职工', 4: '访客', 5: '家长'
}

const PASS_TYPE_OPTIONS = [
  { value: 1, label: '月卡' },
  { value: 2, label: '季卡' },
  { value: 3, label: '年卡' }
]

const STATUS_OPTIONS = [
  { value: 1, label: '生效中' },
  { value: 2, label: '已过期' },
  { value: 3, label: '已退款' }
]

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filter = ref<{ keyword: string; status: number | null; passType: number | null }>({
  keyword: '',
  status: null,
  passType: null
})

const stats = ref<any>({
  totalCount: 0,
  activeCount: 0,
  expiring7dCount: 0,
  expiredCount: 0,
  refundedCount: 0,
  totalRevenue: '0.00',
  monthRevenue: '0.00'
})

const loadStats = async () => {
  try {
    const res: any = await getMonthlyPassStats()
    stats.value = res.data || stats.value
  } catch (e) {
    // 统计失败不阻断列表
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (filter.value.keyword) params.keyword = filter.value.keyword.trim()
    if (filter.value.status !== null) params.status = filter.value.status
    if (filter.value.passType !== null) params.passType = filter.value.passType
    const res: any = await getMonthlyPasses(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadData()
})

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  filter.value = { keyword: '', status: null, passType: null }
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

/** 本地日期（yyyy-MM-dd），用于与后端 LocalDate 字符串做字典序比较 */
const todayStr = computed(() => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
})

/** 综合状态：退款优先；生效中若已过到期日则显示已过期 */
const rowStatus = (row: any): { type: TagType; text: string } => {
  if (Number(row.status) === 3) return { type: 'danger', text: '已退款' }
  if (Number(row.status) === 2) return { type: 'info', text: '已过期' }
  if (row.expireDate && String(row.expireDate) < todayStr.value) {
    return { type: 'warning', text: '已过期' }
  }
  return { type: 'success', text: '生效中' }
}

/** 剩余天数高亮：7 天内到期用橙色提醒 */
const remainClass = (row: any) => {
  const d = Number(row.remainingDays || 0)
  if (Number(row.status) !== 1 || d <= 0) return 'remain-expired'
  if (d <= 7) return 'remain-warn'
  return 'remain-ok'
}

const fmtDate = (v: any) => (v ? String(v).slice(0, 10) : '—')
const fmtTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)
const viewDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}
</script>

<template>
  <div class="page-container">
    <!-- 经营概览 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">生效中月卡</div>
        <div class="stat-value">{{ stats.activeCount }}</div>
        <div class="stat-hint">当前可免按次计费的车辆</div>
      </div>
      <div class="stat-card warn">
        <div class="stat-label">7 天内到期</div>
        <div class="stat-value">{{ stats.expiring7dCount }}</div>
        <div class="stat-hint">可提前提醒用户续费</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已过期</div>
        <div class="stat-value">{{ stats.expiredCount }}</div>
        <div class="stat-hint">到期后按次计费</div>
      </div>
      <div class="stat-card money">
        <div class="stat-label">本月长租收入</div>
        <div class="stat-value">¥{{ stats.monthRevenue }}</div>
        <div class="stat-hint">已排除退款记录</div>
      </div>
      <div class="stat-card money">
        <div class="stat-label">累计长租收入</div>
        <div class="stat-value">¥{{ stats.totalRevenue }}</div>
        <div class="stat-hint">共 {{ stats.totalCount }} 笔开通记录</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>月卡 / 长租记录</span>
          <span class="card-sub">有效期内出场按次停车费自动归零，预约费不豁免</span>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="filter.keyword"
          placeholder="车牌 / 昵称 / 手机号 / 学号工号"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filter.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="o in STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="filter.passType" placeholder="全部卡类型" clearable style="width: 140px">
          <el-option v-for="o in PASS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column prop="plateNumber" label="生效车牌" width="120" />
        <el-table-column label="车主" min-width="150">
          <template #default="{ row }">
            <div class="owner-cell">
              <span class="owner-name">{{ row.nickname || '—' }}</span>
              <span class="owner-sub">{{ row.phone || row.userNo || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="身份" width="80">
          <template #default="{ row }">
            {{ IDENTITY_LABELS[row.identityType] || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="卡类型" width="90">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ row.passTypeLabel || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <span class="date-range">{{ fmtDate(row.startDate) }} ~ {{ fmtDate(row.expireDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100" align="center">
          <template #default="{ row }">
            <span :class="remainClass(row)">
              {{ Number(row.status) === 3 ? '—' : row.remainingDays + ' 天' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="实付(元)" width="100" align="right">
          <template #default="{ row }">
            <span class="amount">{{ row.pricePaid ?? '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="rowStatus(row).type">{{ rowStatus(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="开通时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="月卡详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID" :span="2">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="生效车牌">{{ detail.plateNumber || '—' }}</el-descriptions-item>
        <el-descriptions-item label="卡类型">{{ detail.passTypeLabel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="车主">{{ detail.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份">{{ IDENTITY_LABELS[detail.identityType] || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学号/工号">{{ detail.userNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{ fmtDate(detail.startDate) }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ fmtDate(detail.expireDate) }}</el-descriptions-item>
        <el-descriptions-item label="剩余天数">{{ detail.remainingDays }} 天</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ detail.pricePaid ?? '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="rowStatus(detail).type">{{ rowStatus(detail).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开通时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="用户ID" :span="2">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(31, 41, 55, 0.04);
  border-left: 3px solid #409eff;
}
.stat-card.warn {
  border-left-color: #e6a23c;
}
.stat-card.money {
  border-left-color: #67c23a;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1d2129;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.stat-hint {
  font-size: 12px;
  color: #b1b3b8;
  margin-top: 6px;
}
.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.card-sub {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.owner-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}
.owner-name {
  color: #1d2129;
}
.owner-sub {
  font-size: 12px;
  color: #909399;
}
.date-range {
  font-variant-numeric: tabular-nums;
  color: #606266;
}
.amount {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: #1d2129;
}
.remain-ok {
  color: #67c23a;
  font-weight: 600;
}
.remain-warn {
  color: #e6a23c;
  font-weight: 600;
}
.remain-expired {
  color: #909399;
}
</style>
