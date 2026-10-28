<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getLoginLogs, getLoginLogDetail } from '@/api/admin'

interface Row {
  id: string
  adminId: string | null
  username: string
  loginStatus: string
  failReason: string | null
  ip: string
  userAgent: string
  loginTime: string
}

const loading = ref(false)
const rows = ref<Row[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const username = ref('')
const loginStatus = ref('')
const startTime = ref('')
const endTime = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Row | null>(null)

const FAIL_REASON_LABEL: Record<string, string> = {
  CAPTCHA_INVALID: '验证码错误',
  ACCOUNT_NOT_FOUND: '账号不存在',
  ACCOUNT_DISABLED: '账号已停用',
  PASSWORD_INCORRECT: '密码错误'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (username.value) params.username = username.value
    if (loginStatus.value) params.loginStatus = loginStatus.value
    if (startTime.value) params.startTime = startTime.value
    if (endTime.value) params.endTime = endTime.value
    const res: any = await getLoginLogs(params)
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
  loginStatus.value = ''
  startTime.value = ''
  endTime.value = ''
  page.value = 1
  loadData()
}

const fmtTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')
const failLabel = (r: string | null) => (r ? FAIL_REASON_LABEL[r] || r : '—')

// 查看详情
const viewDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res: any = await getLoginLogDetail(row.id)
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
        <div class="page-header"><span>管理员登录日志</span></div>
      </template>

      <div class="search-bar">
        <el-input v-model="username" placeholder="用户名模糊搜索" clearable style="width: 180px" @keyup.enter="onSearch" />
        <el-select v-model="loginStatus" placeholder="全部状态" clearable style="width: 130px">
          <el-option value="SUCCESS" label="成功" />
          <el-option value="FAIL" label="失败" />
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
          <template #default="{ row }"><span class="time-text">{{ fmtTime(row.loginTime) }}</span></template>
        </el-table-column>
        <el-table-column label="用户名" prop="username" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.loginStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="160">
          <template #default="{ row }">
            <span v-if="row.failReason" style="color: #f56c6c">{{ failLabel(row.failReason) }}</span>
            <span v-else style="color: #909399">—</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="150" show-overflow-tooltip />
        <el-table-column label="User-Agent" prop="userAgent" min-width="280" show-overflow-tooltip />
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
      title="登录日志详情"
      width="640px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="管理员ID">{{ detail.adminId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
            <el-descriptions-item label="登录状态">
              <el-tag :type="detail.loginStatus === 'SUCCESS' ? 'success' : 'danger'">
                {{ detail.loginStatus === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失败原因" :span="2">
              <span v-if="detail.failReason" style="color: #f56c6c">{{ failLabel(detail.failReason) }}</span>
              <span v-else style="color: #909399">—</span>
            </el-descriptions-item>
            <el-descriptions-item label="IP" :span="2">{{ detail.ip || '—' }}</el-descriptions-item>
            <el-descriptions-item label="登录时间" :span="2">{{ fmtTime(detail.loginTime) }}</el-descriptions-item>
          </el-descriptions>
          <div class="long-field">
            <div class="long-field-label">User-Agent</div>
            <pre class="long-field-content">{{ detail.userAgent || '—' }}</pre>
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
  max-height: 300px;
  overflow: auto;
}
</style>
