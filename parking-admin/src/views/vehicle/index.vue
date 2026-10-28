<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVehicleRecords, forceExit } from '@/api'
import LiveDuration from '@/components/LiveDuration.vue'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

const STATUS_OPTIONS = [
  { label: '在场', value: 0 },
  { label: '已离场', value: 1 }
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
const searchPlateNumber = ref('')
const searchUserId = ref('')
const searchSpotId = ref('')

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (filterStatus.value !== undefined) params.status = filterStatus.value
    // 车牌号：模糊匹配，去空格后非空才传
    const plate = searchPlateNumber.value.trim()
    if (plate) params.plateNumber = plate
    // 用户ID：必须能转成有效数字才传
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
    const res: any = await getVehicleRecords(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => loadData())

/** 车辆进出自动刷新：30 秒一次（在场车辆与已停时长自动跟着走） */
useAutoRefresh(loadData, { intervalMs: 30000 })

const handleForceExit = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定强制离场车辆 ${row.plateNumber}？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      confirmButtonType: 'danger'
    })
  } catch {
    /* 用户取消 */
    return
  }
  try {
    await forceExit(row.id)
    ElMessage.success('已强制离场')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '强制离场失败')
  }
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
  searchPlateNumber.value = ''
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
          <span>车辆进出管理</span>
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
          v-model="searchPlateNumber"
          placeholder="车牌号"
          clearable
          style="width: 160px"
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
        <el-table-column prop="id" label="记录ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="plateNumber" label="车牌号" width="120" />
        <el-table-column prop="spotId" label="车位ID" width="100" />
        <el-table-column prop="entryTime" label="入场时间" width="170" />
        <el-table-column prop="exitTime" label="出场时间" width="170" />
        <el-table-column label="停留" width="110">
          <template #default="{ row }">
            <!-- 在场车辆没有结束时间，按入场时间实时走字；已离场用服务端结算的分钟数 -->
            <LiveDuration
              v-if="row.status === 0"
              :entry-time="row.entryTime"
              :fallback-minutes="row.durationMinutes"
            />
            <span v-else>{{ `${Number(row.durationMinutes || 0)} 分` }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalFee" label="费用(元)" width="100" />
        <el-table-column label="支付" width="80">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === 1 ? 'success' : 'danger'">
              {{ row.paymentStatus === 1 ? '已付' : '未付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'info'">{{ row.status === 0 ? '在场' : '已离场' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" @click="handleForceExit(row)">
              强制离场
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

    <!-- 车辆进出详情弹窗 -->
    <el-dialog v-model="detailVisible" title="车辆进出详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ detail.plateNumber }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="车位ID">{{ detail.spotId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="入场时间" :span="2">{{ fmtTime(detail.entryTime) }}</el-descriptions-item>
        <el-descriptions-item label="出场时间" :span="2">{{ fmtTime(detail.exitTime) }}</el-descriptions-item>
        <el-descriptions-item label="停留时长">{{ detail.durationMinutes != null ? `${detail.durationMinutes} 分钟` : '—' }}</el-descriptions-item>
        <el-descriptions-item label="费用(元)">{{ detail.totalFee ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="detail.paymentStatus === 1 ? 'success' : 'danger'">
            {{ detail.paymentStatus === 1 ? '已付' : '未付' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="在场状态">
          <el-tag :type="detail.status === 0 ? 'warning' : 'info'">
            {{ detail.status === 0 ? '在场' : '已离场' }}
          </el-tag>
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
