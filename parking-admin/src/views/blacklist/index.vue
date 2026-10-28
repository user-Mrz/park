<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBlacklist, addBlacklist, removeBlacklist } from '@/api'

interface BlacklistRow {
  id: number
  plateNumber: string
  userId: number | null
  reason: string
  untilTime: string | null
  createdAt: string
}

const loading = ref(false)
const rows = ref<BlacklistRow[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

const open = ref(false)
const saving = ref(false)
const form = reactive({
  plateNumber: '',
  reason: '',
  untilTime: ''
})

const fmtTime = (value: string | null) => (value ? value.replace('T', ' ').slice(0, 19) : '—')

const load = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res: any = await getBlacklist(params)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)

const search = () => {
  page.value = 1
  load()
}

const openAdd = () => {
  form.plateNumber = ''
  form.reason = ''
  form.untilTime = ''
  open.value = true
}

const submit = async () => {
  if (!form.plateNumber.trim()) {
    ElMessage.warning('请填写车牌号')
    return
  }
  saving.value = true
  try {
    await addBlacklist({
      plateNumber: form.plateNumber.trim().toUpperCase(),
      reason: form.reason,
      untilTime: form.untilTime || null
    })
    ElMessage.success('已加入黑名单（该车牌将无法预约与报备）')
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row: BlacklistRow) => {
  await removeBlacklist(row.id)
  ElMessage.success('已移出黑名单')
  if (rows.value.length === 1 && page.value > 1) page.value -= 1
  load()
}

const onPageChange = (value: number) => {
  page.value = value
  load()
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>校园车辆黑名单</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-input
              v-model="keyword"
              placeholder="搜索车牌或原因"
              clearable
              style="width: 220px"
              @keyup.enter="search"
              @clear="search"
            />
            <el-button @click="search">查询</el-button>
            <el-button @click="load">刷新</el-button>
            <el-button type="primary" @click="openAdd">
              <el-icon><Plus /></el-icon>
              <span style="margin-left: 4px">加入黑名单</span>
            </el-button>
          </div>
        </div>
      </template>

      <div style="font-size: 12px; color: #606266; margin-bottom: 12px">
        命中黑名单的车牌/账号将无法预约车位与提交访客报备，请谨慎添加。
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%">
        <el-table-column label="车牌号" width="150">
          <template #default="{ row }">
            <b>{{ row.plateNumber || '（按用户）' }}</b>
          </template>
        </el-table-column>

        <el-table-column label="用户 ID" width="200">
          <template #default="{ row }">{{ row.userId ? String(row.userId) : '—' }}</template>
        </el-table-column>

        <el-table-column label="原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>

        <el-table-column label="有效期" width="190">
          <template #default="{ row }">
            <el-tag v-if="!row.untilTime" type="danger" size="small">永久</el-tag>
            <span v-else>{{ fmtTime(row.untilTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="加入时间" width="180">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确定将该车辆移出黑名单？" width="240" @confirm="remove(row)">
              <template #reference>
                <el-button type="danger" link size="small">移出</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无黑名单记录" :image-size="70" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="open" title="加入黑名单" width="480px" :close-on-click-modal="false">
      <el-form label-width="80px" label-position="right">
        <el-form-item label="车牌号" required>
          <el-input v-model="form.plateNumber" placeholder="如 粤B12345" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="2"
            placeholder="如：多次超时占用、占用他人预约车位"
          />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="form.untilTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示永久"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16px;
}
</style>
