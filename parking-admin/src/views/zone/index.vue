<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getParkingZones,
  addParkingZone,
  updateParkingZone,
  updateParkingZoneStatus,
  deleteParkingZone
} from '@/api'

interface Zone {
  id: string
  zoneCode: string
  zoneName: string
  description: string
  sortOrder: number
  status: number
  spotCount: number
  /** 系统内置区域（如潮汐区），不允许删除或停用 */
  builtin?: boolean
  createdAt: string
}

const loading = ref(false)
const zones = ref<Zone[]>([])
const keyword = ref('')
const statusFilter = ref<number | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (statusFilter.value !== null) params.status = statusFilter.value
    const res: any = await getParkingZones(params)
    zones.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const filteredZones = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return zones.value
  return zones.value.filter(
    (z) => z.zoneCode.includes(kw) || (z.zoneName || '').includes(kw)
  )
})

const totalSpots = computed(() => zones.value.reduce((sum, z) => sum + Number(z.spotCount || 0), 0))

// ---- 新增 / 编辑弹窗 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增停车区域')
const editingId = ref('')
const saving = ref(false)
const formRef = ref<any>(null)
const form = reactive({ zoneCode: '', zoneName: '', description: '', sortOrder: 0 })

const rules = {
  zoneCode: [
    { required: true, message: '请输入区域编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_-]{1,31}$/,
      message: '以小写字母开头，仅可含小写字母、数字、下划线、连字符，长度 2-32',
      trigger: 'blur'
    }
  ],
  zoneName: [{ required: true, message: '请输入区域名称', trigger: 'blur' }]
}

const openCreate = () => {
  dialogTitle.value = '新增停车区域'
  editingId.value = ''
  form.zoneCode = ''
  form.zoneName = ''
  form.description = ''
  // 默认排在末尾
  form.sortOrder = zones.value.length > 0
    ? Math.max(...zones.value.map((z) => Number(z.sortOrder) || 0)) + 1
    : 1
  dialogVisible.value = true
}

// el-table 插槽的 row 由组件推断为 DefaultRow，这里按行数据处理即可（与其它列表页保持一致）
const openEdit = (row: any) => {
  dialogTitle.value = '编辑停车区域'
  editingId.value = row.id
  form.zoneCode = row.zoneCode
  form.zoneName = row.zoneName
  form.description = row.description || ''
  form.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

const submit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      zoneCode: form.zoneCode.trim(),
      zoneName: form.zoneName.trim(),
      description: form.description.trim(),
      sortOrder: Number(form.sortOrder) || 0
    }
    if (editingId.value) {
      await updateParkingZone(editingId.value, payload)
      ElMessage.success('区域已更新')
    } else {
      await addParkingZone(payload)
      ElMessage.success('区域已新增')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row: any) => {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      next === 0 && Number(row.spotCount) > 0
        ? `「${row.zoneName}」下还有 ${row.spotCount} 个车位，停用前请先调整车位所属区域，确定继续？`
        : `确定${action}「${row.zoneName}」吗？`,
      `${action}停车区域`,
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await updateParkingZoneStatus(row.id, next)
  ElMessage.success(`已${action}`)
  loadData()
}

const remove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.zoneName}」吗？该区域下必须没有车位才能删除。`,
      '删除停车区域',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch (e) {
    return
  }
  await deleteParkingZone(row.id)
  ElMessage.success('已删除')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>停车区域管理</span>
          <span class="card-sub">共 {{ zones.length }} 个区域 · {{ totalSpots }} 个车位</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索区域编码或名称"
          clearable
          style="width: 240px"
        />
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="openCreate">新增区域</el-button>
        <el-button @click="loadData">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="filteredZones" row-key="id" size="default" style="width: 100%">
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="zoneCode" label="区域编码" width="140" />
        <el-table-column prop="zoneName" label="区域名称" min-width="180">
          <template #default="{ row }">
            <span>{{ row.zoneName }}</span>
            <el-tag v-if="row.builtin" type="warning" effect="plain" size="small" class="builtin-tag">
              内置
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spotCount" label="车位数" width="100" align="center">
          <template #default="{ row }">
            <span :class="Number(row.spotCount) > 0 ? 'spot-count' : 'spot-count zero'">
              {{ row.spotCount ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="区域说明" min-width="200">
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-tooltip v-if="row.builtin" content="系统内置区域，不允许停用" placement="top">
              <span class="disabled-btn"><el-button type="info" link size="small" disabled>停用</el-button></span>
            </el-tooltip>
            <el-button
              v-else
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-tooltip v-if="row.builtin" content="系统内置区域，不允许删除" placement="top">
              <span class="disabled-btn"><el-button type="info" link size="small" disabled>删除</el-button></span>
            </el-tooltip>
            <el-button v-else type="danger" link size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        style="margin-top: 14px"
        title="区域编码创建后不可修改；标记为「内置」的区域不可删除或停用"
        description="车位通过区域编码归属到区域。新增区域后，可在「常驻车位管理」中把车位分配到该区域；停用或删除区域前，需要先清空或迁走该区域下的车位。潮汐临时停车区由潮汐批次自动创建车位，删除后这些车位会在用户端整体消失，因此被锁定。"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="区域编码" prop="zoneCode">
          <el-input
            v-model="form.zoneCode"
            :disabled="!!editingId"
            placeholder="如 library、research"
            maxlength="32"
          />
          <div class="field-hint">小写字母开头，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="区域名称" prop="zoneName">
          <el-input v-model="form.zoneName" placeholder="如 图书馆区" maxlength="64" />
        </el-form-item>
        <el-form-item label="区域说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            placeholder="可选，用于说明该区域的位置或用途"
          />
        </el-form-item>
        <el-form-item label="展示排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <div class="field-hint">数值越小越靠前，用户端区域选择按此顺序展示</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
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
.spot-count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: #1d2129;
}
.spot-count.zero {
  color: #c0c4cc;
  font-weight: normal;
}
.builtin-tag {
  margin-left: 6px;
}
.disabled-btn {
  display: inline-block;
  margin: 0 6px;
}
.field-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
