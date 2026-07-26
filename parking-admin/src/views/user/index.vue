<template>
  <div class="user-page">
    <!-- 快捷统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" @click="filterStatus = undefined; loadData()">
          <div class="stat-num">{{ stats.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card active" @click="filterStatus = 1; loadData()">
          <div class="stat-num">{{ stats.activeUsers }}</div>
          <div class="stat-label">正常用户</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card disabled" @click="filterStatus = 0; loadData()">
          <div class="stat-num">{{ stats.disabledUsers }}</div>
          <div class="stat-label">禁用用户</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card balance">
          <div class="stat-num">¥{{ stats.totalBalance }}</div>
          <div class="stat-label">账户总余额</div>
        </div>
      </el-col>
    </el-row>

    <!-- 用户列表 -->
    <el-card>
      <template #header>
        <div class="page-header">
          <span>
            用户列表
            <el-tag v-if="filterStatus === 1" size="small" type="success" style="margin-left:8px">正常</el-tag>
            <el-tag v-if="filterStatus === 0" size="small" type="danger" style="margin-left:8px">禁用</el-tag>
            <el-button v-if="filterStatus !== undefined" size="small" text type="info" @click="filterStatus=undefined;loadData()">清除筛选</el-button>
          </span>
          <el-input v-model="keyword" placeholder="搜索手机号/昵称/车牌" clearable style="width: 260px" @clear="loadData" @keyup.enter="loadData">
            <template #append><el-button @click="loadData" :icon="Search" /></template>
          </el-input>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="plateNumber" label="车牌号" min-width="110" />
        <el-table-column prop="balance" label="余额(元)" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="165" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm :title="row.status===1?'确定禁用该用户？':'确定启用该用户？'" @confirm="toggleStatus(row)">
              <template #reference>
                <el-button size="small" :type="row.status===1?'danger':'success'" link>
                  {{ row.status===1?'禁用':'启用' }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="480px" center>
      <el-descriptions :column="1" border v-if="currentUser">
        <el-descriptions-item label="ID" align="center">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="昵称" align="center">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号" align="center">{{ currentUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="车牌号" align="center">{{ currentUser.plateNumber || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="余额" align="center">¥{{ currentUser.balance }}</el-descriptions-item>
        <el-descriptions-item label="状态" align="center">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" align="center">{{ currentUser.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getUsers, getUserDetail, updateUserStatus } from '@/api'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref<number | undefined>(undefined)
const detailVisible = ref(false)
const currentUser = ref<any>(null)

const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  disabledUsers: 0,
  totalBalance: '0.00'
})

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value, keyword: keyword.value }
    if (filterStatus.value !== undefined && filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    const res: any = await getUsers(params)
    tableData.value = res.data.records
    total.value = res.data.total

    // 加载统计数据
    const allRes: any = await getUsers({ page: 1, pageSize: 1 })
    stats.totalUsers = allRes.data.total
    const activeRes: any = await getUsers({ page: 1, pageSize: 1, status: 1 })
    stats.activeUsers = activeRes.data.total
    const disabledRes: any = await getUsers({ page: 1, pageSize: 1, status: 0 })
    stats.disabledUsers = disabledRes.data.total
    // 余额从当前列表粗略汇总
    let sum = 0
    tableData.value.forEach((u: any) => { sum += Number(u.balance || 0) })
    stats.totalBalance = sum.toFixed(2)
  } finally { loading.value = false }
}

async function showDetail(row: any) {
  try {
    const res: any = await getUserDetail(row.id)
    currentUser.value = res.data
    detailVisible.value = true
  } catch { /* ignore */ }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadData()
}

onMounted(() => { loadData() })
</script>

<style scoped>
.user-page { padding: 0; }

/* 统计卡片 */
.stats-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}
.stat-card.active:hover { border-color: #67C23A; }
.stat-card.disabled:hover { border-color: #F56C6C; }
.stat-card.balance:hover { border-color: #E6A23C; }
.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 13px;
  color: #909399;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
