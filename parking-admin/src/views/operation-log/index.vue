<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogs, getOperationLogDetail } from '@/api/admin'

interface Row {
  id: string
  adminId: string | null
  username: string
  operation: string
  menuCode: string
  type: string
  uri: string
  params: string
  responseStatus: number
  costMs: number
  ip: string
  operationTime: string
}

const loading = ref(false)
const rows = ref<Row[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const username = ref('')
const menuCode = ref('')
const type = ref('')
const startTime = ref('')
const endTime = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Row | null>(null)

const MENU_LABELS: Record<string, string> = {
  dashboard: '首页',
  users: '用户管理',
  departments: '学院管理',
  spots: '常驻车位管理',
  tidal: '潮汐车位管理',
  reservations: '预约管理',
  vehicles: '车辆进出',
  visits: '访客与家长报备',
  announcements: '公告管理',
  violations: '违规记录',
  blacklist: '黑白名单',
  recharges: '充值记录',
  payments: '扣费记录',
  config: '系统配置',
  rules: '停车规则',
  server: '硬件监控',
  accounts: '管理员账号管理',
  login_logs: '登录日志',
  operation_logs: '操作日志'
}

const TYPE_TAG: Record<string, string> = {
  CREATE: 'success',
  UPDATE: 'primary',
  DELETE: 'danger',
  AUDIT: 'warning'
}

const TYPE_LABEL: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  AUDIT: '审核'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (username.value) params.username = username.value
    if (menuCode.value) params.menuCode = menuCode.value
    if (type.value) params.type = type.value
    if (startTime.value) params.startTime = startTime.value
    if (endTime.value) params.endTime = endTime.value
    const res: any = await getOperationLogs(params)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())

const onSearch = () => {
  page.value = 1
  loadData()
}
const onReset = () => {
  username.value = ''
  menuCode.value = ''
  type.value = ''
  startTime.value = ''
  endTime.value = ''
  page.value = 1
  loadData()
}

const fmtTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')
const menuLabel = (c: string) => (c ? MENU_LABELS[c] || c : '—')

// 美化 JSON 参数，非 JSON 则原样返回
const formatParams = (raw: string): string => {
  if (!raw) return ''
  try {
    const obj = JSON.parse(raw)
    return JSON.stringify(obj, null, 2)
  } catch {
    return raw
  }
}

// 查看详情
const viewDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res: any = await getOperationLogDetail(row.id)
    detail.value = res.data as Row
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header"><span>管理员操作日志</span></div>
      </template>

      <div class="search-bar">
        <el-input v-model="username" placeholder="用户名模糊搜索" clearable style="width: 180px" @keyup.enter="onSearch" />
        <el-select v-model="menuCode" placeholder="所属菜单" clearable style="width: 180px">
          <el-option v-for="(label, code) in MENU_LABELS" :key="code" :value="code" :label="label" />
        </el-select>
        <el-select v-model="type" placeholder="操作类型" clearable style="width: 130px">
          <el-option value="CREATE" label="新增" />
          <el-option value="UPDATE" label="修改" />
          <el-option value="DELETE" label="删除" />
          <el-option value="AUDIT" label="审核" />
        </el-select>
        <el-date-picker
          v-model="startTime"
          type="datetime"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="开始时间"
          style="width: 200px"
        />
        <el-date-picker
          v-model="endTime"
          type="datetime"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="结束时间"
          style="width: 200px"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <span class="search-tip">支持任意一项独立搜索</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%; margin-top: 12px">
        <el-table-column label="时间" width="170">
          <template #default="{ row }"><span class="time-text">{{ fmtTime(row.operationTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作人" prop="username" width="130" show-overflow-tooltip />
        <el-table-column label="操作" prop="operation" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(TYPE_TAG[row.type] as any) || 'info'" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属菜单" width="140">
          <template #default="{ row }">{{ menuLabel(row.menuCode) }}</template>
        </el-table-column>
        <el-table-column label="URI" prop="uri" min-width="220" show-overflow-tooltip />
        <el-table-column label="参数" prop="params" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态码" prop="responseStatus" width="80" align="center" />
        <el-table-column label="耗时(ms)" prop="costMs" width="90" align="right" />
        <el-table-column label="IP" prop="ip" width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 16px; justify-content: flex-end; display: flex"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="操作日志详情"
      width="720px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="管理员ID">{{ detail.adminId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="操作人">{{ detail.username }}</el-descriptions-item>
            <el-descriptions-item label="操作时间">{{ fmtTime(detail.operationTime) }}</el-descriptions-item>
            <el-descriptions-item label="操作类型">
              <el-tag :type="(TYPE_TAG[detail.type] as any) || 'info'" size="small">
                {{ TYPE_LABEL[detail.type] || detail.type || '—' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="所属菜单">{{ menuLabel(detail.menuCode) }}</el-descriptions-item>
            <el-descriptions-item label="操作描述" :span="2">{{ detail.operation || '—' }}</el-descriptions-item>
            <el-descriptions-item label="请求URI" :span="2">{{ detail.uri || '—' }}</el-descriptions-item>
            <el-descriptions-item label="响应状态码">{{ detail.responseStatus ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detail.costMs != null ? detail.costMs + ' ms' : '—' }}</el-descriptions-item>
            <el-descriptions-item label="IP" :span="2">{{ detail.ip || '—' }}</el-descriptions-item>
          </el-descriptions>
          <div class="long-field">
            <div class="long-field-label">请求参数</div>
            <pre class="long-field-content">{{ detail.params ? formatParams(detail.params) : '—' }}</pre>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
.time-text {
  color: #606266;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.detail-body {
  min-height: 80px;
}
.long-field {
  margin-top: 16px;
}
.long-field-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.long-field-content {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 360px;
  overflow: auto;
}
</style>
