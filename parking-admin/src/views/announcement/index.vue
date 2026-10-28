<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadInstance
} from 'element-plus'
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncement,
  getAnnouncements,
  updateAnnouncement,
  updateAnnouncementStatus,
  type Announcement,
  type AnnouncementForm
} from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

/** 允许的附件扩展名（与后端白名单一致，比较时统一转小写） */
const ALLOWED_EXT = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'txt',
  'zip',
  'rar',
  '7z',
  'jpg',
  'jpeg',
  'png',
  'gif',
  'webp',
  'bmp'
]

/** el-upload 的 accept：'.pdf,.doc,...' */
const ACCEPT = ALLOWED_EXT.map((e) => `.${e}`).join(',')

/** 单个附件大小上限 20MB（后端也会校验，前端先拦一次） */
const MAX_SIZE = 20 * 1024 * 1024

/** 状态下拉：全部 / 草稿 / 已发布 */
const STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref<Announcement[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const keyword = ref('')
const status = ref<number | ''>('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
/** 当前编辑的公告（新建时为 null），用于展示「现有附件」与勾选移除 */
const editingRow = ref<Announcement | null>(null)

const formRef = ref<FormInstance>()
const form = reactive<AnnouncementForm>({
  title: '',
  content: '',
  status: 1
})

/* ============ 附件上传状态 ============ */
const uploadRef = ref<UploadInstance>()
/** 用户选中的原始 File（:auto-upload="false" 时 el-upload 不会自己发请求） */
const selectedFile = ref<File | null>(null)
/** 编辑时勾选「移除附件」=> 提交 removeAttachment=true（且不传新文件） */
const removeAttachment = ref(false)

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告正文', trigger: 'blur' }]
}

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const res: any = await getAnnouncements({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: status.value === '' ? '' : status.value
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => loadData())


useAutoRefresh(loadData, { intervalMs: 30000 })

const onSearch = () => {
  page.value = 1
  loadData()
}

const onReset = () => {
  keyword.value = ''
  status.value = ''
  page.value = 1
  loadData()
}

const onPageChange = (p: number) => {
  page.value = p
  loadData()
}

/** 状态筛选变化即回到第一页重新查询（与 tidal 页 keyword 的处理一致） */
watch(status, () => {
  page.value = 1
  loadData()
})

/* ============ 展示辅助 ============ */
const fmtTime = (t: string) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')

/** 字节数 -> KB / MB（与后端 attachmentSize 对应） */
const fmtSize = (bytes: number) => {
  const n = Number(bytes || 0)
  if (!n || n <= 0) return '—'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

/** 取小写扩展名（无扩展名返回空串） */
const extOf = (name: string) => {
  const i = String(name || '').lastIndexOf('.')
  return i >= 0 ? String(name).slice(i + 1).toLowerCase() : ''
}

/** 本地校验：返回错误文案，'' 表示通过 */
const checkFile = (file: File) => {
  const ext = extOf(file.name)
  if (!ext || !ALLOWED_EXT.includes(ext)) {
    return `不支持的附件类型「${ext || '未知'}」，仅支持 ${ALLOWED_EXT.join(' / ')}`
  }
  if (file.size > MAX_SIZE) {
    return `附件大小 ${fmtSize(file.size)} 超过上限 20MB`
  }
  return ''
}

/**
 * 附件下载：不能用 axios（响应拦截器会把非 JSON 响应当成业务失败）。
 * 管理端 dev server 已把 /api 与 /uploads 代理到后端，
 * 直接新标签打开相对路径即可，后端会以 Content-Disposition 带上原始文件名。
 */
const openAttachment = (url: string) => {
  if (!url) {
    ElMessage.warning('该公告没有附件')
    return
  }
  window.open(url, '_blank', 'noopener')
}

/* ============ 弹窗内的附件交互 ============ */
const resetUpload = () => {
  selectedFile.value = null
  removeAttachment.value = false
  uploadRef.value?.clearFiles()
}

/** 选中文件（auto-upload=false 时由 el-upload 的 change 事件给出原始 File） */
const onFileChange = (uploadFile: UploadFile) => {
  const raw = uploadFile.raw
  if (!raw) return
  const err = checkFile(raw)
  if (err) {
    ElMessage.warning(err)
    // 校验不通过：从 el-upload 列表里移除该文件，同时清空选中项
    uploadRef.value?.handleRemove(uploadFile)
    selectedFile.value = null
    return
  }
  selectedFile.value = raw
  // 选了新附件就意味着替换，移除勾选交由后端按「有新文件」处理
  removeAttachment.value = false
}

const onFileRemove = () => {
  selectedFile.value = null
}

const onFileExceed = () => {
  ElMessage.warning('只能上传 1 个附件，请先移除已选文件')
}

/* ============ 新建 / 编辑 ============ */
const openAddDialog = async () => {
  isEdit.value = false
  editId.value = ''
  editingRow.value = null
  form.title = ''
  form.content = ''
  form.status = 1
  resetUpload()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

/**
 * 「查看/编辑」共用一个弹窗，不额外做只读弹窗：
 * 列表接口返回的就是完整 Announcement，直接编辑即可，无需再多点一次「编辑」。
 * 打开前会先拉一次详情（GET /admin/announcement/{id}）保证正文/附件是最新的，
 * 详情异常时退回列表行数据，弹窗照常可用（错误提示由 request 拦截器统一弹出）。
 */
const openEditDialog = async (row: any) => {
  let detail: Announcement = row
  try {
    const res: any = await getAnnouncement(row.id)
    if (res?.data) detail = res.data as Announcement
  } catch {
    detail = row
  }
  isEdit.value = true
  editId.value = detail.id
  editingRow.value = detail
  form.title = detail.title || ''
  form.content = detail.content || ''
  form.status = detail.status ?? 0
  resetUpload()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

/**
 * 组装 multipart/form-data：
 * 不手动设置 Content-Type，交给浏览器带上 boundary（request.ts 没有默认 Content-Type）。
 */
const buildFormData = () => {
  const fd = new FormData()
  fd.append('title', form.title.trim())
  fd.append('content', form.content)
  fd.append('status', String(form.status))
  if (selectedFile.value) {
    fd.append('file', selectedFile.value)
  } else if (isEdit.value && removeAttachment.value) {
    // 契约：removeAttachment=true 表示删除现有附件且不传新文件
    fd.append('removeAttachment', 'true')
  }
  return fd
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid === false) return
  // 二次兜底：防止绕过 on-change 校验（例如拖拽替换了文件）
  if (selectedFile.value) {
    const err = checkFile(selectedFile.value)
    if (err) {
      ElMessage.warning(err)
      return
    }
  }
  saving.value = true
  try {
    const fd = buildFormData()
    if (isEdit.value) {
      await updateAnnouncement(editId.value, fd)
    } else {
      await createAnnouncement(fd)
    }
    ElMessage.success(isEdit.value ? '公告已保存' : '公告已创建')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

/** 发布 / 下架：按当前状态提交相反动作（1发布 0下架） */
const handleToggleStatus = async (row: any) => {
  const next = row.status === 1 ? 0 : 1
  await updateAnnouncementStatus(row.id, next)
  ElMessage.success(next === 1 ? '公告已发布，用户可在小程序「公告」页查看' : '公告已下架')
  loadData()
}

/** 删除公告：后端会一并删除附件文件 */
const handleDelete = async (row: any) => {
  await deleteAnnouncement(row.id)
  ElMessage.success('公告已删除（附件文件已一并删除）')
  loadData()
}

const totalText = () => `共 ${total.value} 条`
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>公告管理</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-button type="primary" @click="openAddDialog">
              <el-icon><Bell /></el-icon>
              <span style="margin-left: 4px">发布公告</span>
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="公告用于发布放假通知、施工绕行、停车规则调整等信息，可携带附件（PDF/Word/Excel/图片/压缩包，单个不超过 20MB）；发布后用户可在小程序「公告」页查看并下载附件。"
        style="margin-bottom: 12px"
      />

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="标题 / 正文关键字"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" @click="onSearch">
          <el-icon><Search /></el-icon>
          <span style="margin-left: 4px">查询</span>
        </el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%">
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.statusLabel || (row.status === 1 ? '已发布' : '草稿') }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="附件" min-width="230">
          <template #default="{ row }">
            <div v-if="row.attachmentName" class="attach-cell">
              <el-icon class="attach-icon"><Paperclip /></el-icon>
              <el-tooltip :content="row.attachmentName" placement="top">
                <span class="attach-name">{{ row.attachmentName }}</span>
              </el-tooltip>
              <span class="attach-size">{{ fmtSize(row.attachmentSize) }}</span>
              <el-button
                type="primary"
                link
                size="small"
                @click="openAttachment(row.attachmentDownloadUrl)"
              >
                下载
              </el-button>
            </div>
            <span v-else class="sub-text">—</span>
          </template>
        </el-table-column>

        <el-table-column label="发布人" width="120">
          <template #default="{ row }">{{ row.publisher || '—' }}</template>
        </el-table-column>

        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ fmtTime(row.publishTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="浏览量" width="90" align="right">
          <template #default="{ row }">{{ row.viewCount ?? 0 }}</template>
        </el-table-column>

        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ fmtTime(row.updatedAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 4px">
              <el-button size="small" @click="openEditDialog(row)">查看/编辑</el-button>
              <el-popconfirm
                :title="
                  row.status === 1
                    ? '下架后用户在小程序「公告」页将不再看到该公告，确定？'
                    : '发布后用户可在小程序「公告」页查看并下载附件，确定？'
                "
                width="280"
                @confirm="handleToggleStatus(row)"
              >
                <template #reference>
                  <el-button
                    size="small"
                    :style="
                      row.status === 1
                        ? { color: '#e6a23c', borderColor: '#e6a23c' }
                        : { color: '#67c23a', borderColor: '#67c23a' }
                    "
                  >
                    {{ row.status === 1 ? '下架' : '发布' }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm
                title="删除后不可恢复，且会一并删除已上传的附件文件，确定删除该公告？"
                width="280"
                confirm-button-type="danger"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无公告" :image-size="70" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :hide-on-single-page="false"
          :total-text="totalText"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 发布 / 查看·编辑公告 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '查看 / 编辑公告' : '发布公告'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="right">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="128"
            show-word-limit
            placeholder="如 关于寒假期间校园停车场开放时间调整的通知"
          />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告正文，可用换行分段"
          />
          <div class="form-hint">小程序端按原样展示，建议用空行分段</div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">立即发布</el-radio>
            <el-radio :value="0">存为草稿</el-radio>
          </el-radio-group>
          <div class="form-hint">草稿仅管理端可见；发布后用户可在小程序「公告」页查看</div>
        </el-form-item>

        <el-form-item label="附件">
          <!-- 已有附件：显示原始文件名 + 下载 + 移除勾选（仅在编辑时出现） -->
          <div v-if="isEdit && editingRow && editingRow.attachmentName" class="existing-attach">
            <div class="ea-row">
              <el-icon class="attach-icon"><Paperclip /></el-icon>
              <span class="ea-name">{{ editingRow.attachmentName }}</span>
              <span class="attach-size">{{ fmtSize(editingRow.attachmentSize) }}</span>
              <el-button
                type="primary"
                link
                size="small"
                @click="openAttachment(editingRow.attachmentDownloadUrl)"
              >
                下载
              </el-button>
            </div>
            <el-checkbox v-model="removeAttachment" :disabled="!!selectedFile">
              移除该附件
              <span v-if="selectedFile" class="sub-text">（已选择新附件，提交后直接替换）</span>
            </el-checkbox>
          </div>

          <!-- auto-upload=false：交给带 admin_token 的 axios 提交，这里只负责收集 File -->
          <div class="upload-wrap">
            <el-upload
              ref="uploadRef"
              drag
              :auto-upload="false"
              :limit="1"
              :accept="ACCEPT"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :on-exceed="onFileExceed"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">将文件拖到此处，或<em>点击选择</em></div>
              <template #tip>
                <div class="form-hint">
                  可选 1 个附件（{{ ALLOWED_EXT.join(' / ') }}），单个不超过 20MB；不选则提交纯文本公告
                </div>
              </template>
            </el-upload>
          </div>

          <div v-if="selectedFile" class="picked-file">
            已选附件：{{ selectedFile.name }}（{{ fmtSize(selectedFile.size) }}）
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
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
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16px;
}
.form-hint {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
.sub-text {
  font-size: 12px;
  color: #909399;
}
.time-text {
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  color: #606266;
}
.attach-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.attach-icon {
  color: #409eff;
  flex-shrink: 0;
}
.attach-name {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
  font-size: 13px;
}
.attach-size {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.existing-attach {
  flex: 0 0 100%;
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f5f9ff;
  padding: 8px 10px;
  margin-bottom: 8px;
}
.ea-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  margin-bottom: 4px;
}
.ea-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-wrap {
  flex: 0 0 100%;
  width: 100%;
}
/* el-upload 默认是 inline-flex 的窄条，这里让它撑满表单项，拖拽区更直观 */
.upload-wrap :deep(.el-upload) {
  width: 100%;
}
.upload-wrap :deep(.el-upload-dragger) {
  width: 100%;
}
.upload-icon {
  font-size: 40px;
  color: #a8abb2;
  margin-bottom: 6px;
}
.upload-text {
  font-size: 13px;
  color: #606266;
}
.upload-text em {
  color: #409eff;
  font-style: normal;
}
.picked-file {
  flex: 0 0 100%;
  width: 100%;
  font-size: 12px;
  color: #67c23a;
  line-height: 1.6;
  margin-top: 2px;
}
</style>
