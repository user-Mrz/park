<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getScheduledJobs,
  getScheduledJobHandlers,
  createScheduledJob,
  updateScheduledJob,
  deleteScheduledJob,
  updateScheduledJobStatus,
  runScheduledJob
} from '@/api'

interface Job {
  id: number
  jobName: string
  jobGroup: string
  handlerKey: string
  cronExpression: string
  status: number
  remark: string
  lastRunTime: string | null
  lastRunResult: string
  createdAt: string
  updatedAt: string
}

interface Handler {
  key: string
  name: string
  description: string
  defaultCron: string
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<Job[]>([])
const handlers = ref<Handler[]>([])
const keyword = ref('')
const statusFilter = ref<number | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const open = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const formRef = ref<FormInstance>()

const form = reactive({
  jobName: '',
  jobGroup: 'SYSTEM',
  handlerKey: '',
  cronExpression: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  handlerKey: [{ required: true, message: '请选择执行器', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }]
}

const handlerName = (key: string) => handlers.value.find((h) => h.key === key)?.name || key

const handlerOptions = computed(() => handlers.value)

const load = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (statusFilter.value !== null) params.status = statusFilter.value
    const res: any = await getScheduledJobs(params)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const loadHandlers = async () => {
  if (handlers.value.length) return
  const res: any = await getScheduledJobHandlers()
  handlers.value = res.data || []
}

onMounted(async () => {
  await loadHandlers()
  await load()
})

const search = () => {
  page.value = 1
  load()
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = null
  page.value = 1
  load()
}

const openAdd = async () => {
  await loadHandlers()
  isEdit.value = false
  editId.value = 0
  open.value = true
  await nextTick()
  formRef.value?.clearValidate()
  form.jobName = ''
  form.jobGroup = 'SYSTEM'
  form.handlerKey = handlers.value[0]?.key || ''
  form.cronExpression = handlers.value[0]?.defaultCron || '0 * * * * ?'
  form.status = 1
  form.remark = ''
}

const openEdit = async (row: Job) => {
  await loadHandlers()
  isEdit.value = true
  editId.value = row.id
  open.value = true
  await nextTick()
  formRef.value?.clearValidate()
  form.jobName = row.jobName
  form.jobGroup = row.jobGroup || 'SYSTEM'
  form.handlerKey = row.handlerKey
  form.cronExpression = row.cronExpression
  form.status = row.status
  form.remark = row.remark || ''
}

/** 新增时切换执行器，自动带出默认 Cron */
const onHandlerChange = (key: string) => {
  if (isEdit.value) return
  const handler = handlers.value.find((h) => h.key === key)
  if (handler) form.cronExpression = handler.defaultCron
}

const quickCrons = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每 5 分钟', value: '0 */5 * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每天 9 点', value: '0 0 9 * * ?' }
]

const applyCron = (value: string) => {
  form.cronExpression = value
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      jobName: form.jobName.trim(),
      jobGroup: form.jobGroup.trim() || 'SYSTEM',
      handlerKey: form.handlerKey,
      cronExpression: form.cronExpression.trim(),
      status: form.status,
      remark: form.remark
    }
    if (isEdit.value) {
      await updateScheduledJob(editId.value, payload)
      ElMessage.success('已保存，调度计划已即时生效')
    } else {
      await createScheduledJob(payload)
      ElMessage.success('任务已创建')
    }
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggle = async (row: Job, value: string | number | boolean) => {
  const next = Number(value)
  try {
    await updateScheduledJobStatus(row.id, next)
    ElMessage.success(next === 1 ? '任务已启动' : '任务已暂停')
    load()
  } catch {
    // 失败时保持原状态
    load()
  }
}

const run = async (row: Job) => {
  await runScheduledJob(row.id)
  ElMessage.success('已触发执行，稍后刷新可查看执行结果')
}

const remove = async (row: Job) => {
  await deleteScheduledJob(row.id)
  ElMessage.success('已删除')
  if (rows.value.length === 1 && page.value > 1) page.value -= 1
  load()
}

const onPageChange = (value: number) => {
  page.value = value
  load()
}

const onSizeChange = (value: number) => {
  pageSize.value = value
  page.value = 1
  load()
}

const formatTime = (value: string | null) => (value ? value.replace('T', ' ') : '—')

const resultType = (value: string) => (!value ? 'info' : value.startsWith('失败') ? 'danger' : 'success')

const resultText = (value: string) => (!value ? '未执行' : value)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>定时任务管理</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-input
              v-model="keyword"
              placeholder="搜索任务名称 / 执行器 / 备注"
              clearable
              style="width: 240px"
              @keyup.enter="search"
              @clear="search"
            />
            <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="search">
              <el-option label="运行中" :value="1" />
              <el-option label="已暂停" :value="0" />
            </el-select>
            <el-button @click="search">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" @click="openAdd">
              <el-icon><Plus /></el-icon>
              <span style="margin-left: 4px">新增任务</span>
            </el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px">
        <template #title>
          Cron 格式：秒 分 时 日 月 周（例：0 */5 * * * ? 表示每 5 分钟）。执行器为后端白名单任务，
          修改 Cron 或启停后立即生效；任务运行中再次触发会等待上次执行完成，避免堆叠。
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%">
        <el-table-column label="任务名称" min-width="190">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <span style="font-weight: 600">{{ row.jobName }}</span>
              <span v-if="row.remark" style="font-size: 12px; color: #909399">{{ row.remark }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分组" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.jobGroup || 'SYSTEM' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="执行器" min-width="170">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <span>{{ handlerName(row.handlerKey) }}</span>
              <span style="font-size: 12px; color: #909399">{{ row.handlerKey }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Cron 表达式" width="170">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" style="font-family: Consolas, monospace">
              {{ row.cronExpression }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="运行"
              inactive-text="暂停"
              inline-prompt
              @change="toggle(row, $event)"
            />
          </template>
        </el-table-column>

        <el-table-column label="上次执行" min-width="210">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px">
              <span style="font-size: 12px; color: #606266">{{ formatTime(row.lastRunTime) }}</span>
              <el-tag size="small" :type="resultType(row.lastRunResult)" effect="light">
                {{ resultText(row.lastRunResult) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div style="display: flex; gap: 4px; align-items: center; justify-content: center">
              <el-popconfirm title="立即执行一次该任务？不会影响原有调度计划" width="240" @confirm="run(row)">
                <template #reference>
                  <el-button type="success" link size="small">执行</el-button>
                </template>
              </el-popconfirm>
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm
                title="删除后该任务将不再执行，确定删除？"
                width="240"
                confirm-button-type="danger"
                @confirm="remove(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无定时任务" :image-size="70" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="open"
      :title="isEdit ? '编辑定时任务' : '新增定时任务'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="如 预约超时自动释放" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup">
          <el-input v-model="form.jobGroup" placeholder="默认 SYSTEM" />
        </el-form-item>
        <el-form-item label="执行器" prop="handlerKey">
          <el-select
            v-model="form.handlerKey"
            placeholder="请选择执行器"
            style="width: 100%"
            @change="onHandlerChange"
          >
            <el-option
              v-for="item in handlerOptions"
              :key="item.key"
              :label="item.name"
              :value="item.key"
            >
              <div style="display: flex; flex-direction: column">
                <span>{{ item.name }}</span>
                <span style="font-size: 12px; color: #909399">{{ item.description }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Cron" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="秒 分 时 日 月 周，如 0 */5 * * * ?" />
          <div style="display: flex; gap: 6px; flex-wrap: wrap; margin-top: 6px">
            <el-button
              v-for="preset in quickCrons"
              :key="preset.value"
              size="small"
              plain
              @click="applyCron(preset.value)"
            >
              {{ preset.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="运行"
            inactive-text="暂停"
          />
          <span style="margin-left: 10px; font-size: 12px; color: #909399">
            保存后立即按此状态启动或暂停调度
          </span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="任务用途说明（选填）" maxlength="255" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
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
