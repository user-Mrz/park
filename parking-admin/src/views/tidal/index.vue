<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  cancelTidalBatch,
  createTidalBatch,
  deleteTidalBatch,
  forceDeleteTidalBatch,
  getTidalBatch,
  getTidalBatches,
  restoreTidalBatch,
  softDeleteTidalBatch,
  updateTidalBatch,
  type TidalBatch,
  type TidalBatchForm,
  type TidalSpot
} from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { getUserRoles } from '@/api/admin'

/** 允许停放身份（value = identity_type），与车位管理保持一致 */
const IDENTITY_OPTIONS = [
  { value: '1', label: '学生' },
  { value: '2', label: '教师' },
  { value: '3', label: '职工' },
  { value: '4', label: '访客' },
  { value: '5', label: '家长' }
]

/**
 * 可见「潮汐临时停车区」的身份集合。
 * 只有被授予 tidal 区域可见权限的用户角色，其对应身份才允许出现在批次的「允许停放身份」里，
 * 否则勾选后用户端根本看不到潮汐车位，属于无效配置。
 */
const tidalVisibleIdentities = ref<number[]>([])

const loadTidalVisibleIdentities = async () => {
  try {
    const res: any = await getUserRoles()
    const roles = res.data || []
    const set = new Set<number>()
    roles.forEach((r: any) => {
      const zones = Array.isArray(r.zones) ? r.zones : []
      const identity = Number(r.identityType)
      if (zones.includes('tidal') && !Number.isNaN(identity)) set.add(identity)
    })
    tidalVisibleIdentities.value = Array.from(set)
  } catch (e) {
    // 加载失败时不做限制，避免误禁用全部身份导致无法提交
    tidalVisibleIdentities.value = []
  }
}

/** 该身份是否可选：未加载到时（空集合）默认允许，避免接口异常造成表单不可用 */
const identitySelectable = (value: string) =>
  tidalVisibleIdentities.value.length === 0 || tidalVisibleIdentities.value.includes(Number(value))

/** 只保留已授予「潮汐临时停车区」可见权限的身份，无权限的身份不展示、无法勾选 */
const availableIdentities = computed(() => IDENTITY_OPTIONS.filter((i) => identitySelectable(i.value)))

/** 批次状态筛选（与后端 status：0待开放 1开放中 2已结束 3已取消 一致） */
const STATUS_OPTIONS = [
  { value: 0, label: '待开放' },
  { value: 1, label: '开放中' },
  { value: 2, label: '已结束' },
  { value: 3, label: '已取消' }
]

/** 当前阶段 el-tag 配色：开放前 warning / 开放中 success / 已结束 info / 已取消 danger */
const PHASE_TAG: Record<string, { type: string; text: string }> = {
  upcoming: { type: 'warning', text: '开放前·仅可预约' },
  open: { type: 'success', text: '开放中·可入场' },
  ended: { type: 'info', text: '已结束' },
  cancelled: { type: 'danger', text: '已取消' }
}

/** 车位状态（与 spot/index.vue 的 STATUS_TAG 一致） */
const SPOT_STATUS_TAG: Record<number, { type: string; text: string }> = {
  0: { type: 'success', text: '空闲' },
  1: { type: 'warning', text: '占用' },
  2: { type: 'primary', text: '预约' },
  3: { type: 'danger', text: '维护' }
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<TidalBatch[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const keyword = ref('')
const status = ref<number | ''>('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
/** 当前编辑行（新建时为 null），用于判断批次是否已是终态 */
const editingRow = ref<TidalBatch | null>(null)

const formRef = ref<FormInstance>()
const form = reactive<TidalBatchForm>({
  batchName: '',
  area: '',
  spotPrefix: 'TD',
  spotCount: 50,
  floor: '',
  pricePerHour: 5,
  allowIdentities: '',
  freeMinutes: 0,
  openTime: '',
  closeTime: '',
  remark: ''
})

/** 允许身份的多选绑定数组，提交时拼成逗号分隔字符串写入 form.allowIdentities */
const allowIdentityList = ref<string[]>([])

const isEmpty = (v: unknown) => v === undefined || v === null || String(v).trim() === ''

/** 'yyyy-MM-dd HH:mm:ss' -> 毫秒时间戳（兼容 ISO 带 T 的写法） */
const parseTime = (t: string) => {
  const s = String(t || '').trim().replace('T', ' ')
  const ts = new Date(s.replace(/-/g, '/')).getTime()
  return Number.isNaN(ts) ? null : ts
}

/**
 * 表单校验规则（与后端一致）：
 * batchName/area/spotPrefix 必填；spotCount 1~500 的整数；
 * openTime/closeTime 必填且 closeTime 晚于 openTime；pricePerHour ≥ 0；freeMinutes 0~1440
 */
const rules: FormRules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  area: [{ required: true, message: '请输入显示区域名', trigger: 'blur' }],
  spotPrefix: [{ required: true, message: '请输入车位编号前缀', trigger: 'blur' }],
  spotCount: [
    {
      required: true,
      trigger: 'change',
      validator: (_rule, value, callback) => {
        const n = Number(value)
        if (!Number.isInteger(n) || n < 1 || n > 500) {
          callback(new Error('车位数需为 1~500 的整数'))
          return
        }
        callback()
      }
    }
  ],
  pricePerHour: [
    {
      required: true,
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if (Number(value) < 0) {
          callback(new Error('单价不能小于 0'))
          return
        }
        callback()
      }
    }
  ],
  freeMinutes: [
    {
      required: true,
      trigger: 'change',
      validator: (_rule, value, callback) => {
        const n = Number(value)
        if (!Number.isInteger(n) || n < 0 || n > 1440) {
          callback(new Error('免费时长需为 0~1440 的整数'))
          return
        }
        callback()
      }
    }
  ],
  openTime: [{ required: true, message: '请选择开放时间', trigger: 'change' }],
  closeTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if (isEmpty(value) || isEmpty(form.openTime)) {
          callback()
          return
        }
        const open = parseTime(form.openTime)
        const close = parseTime(String(value))
        if (open !== null && close !== null && close <= open) {
          callback(new Error('结束时间必须晚于开放时间'))
          return
        }
        callback()
      }
    }
  ]
}

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const res: any = await getTidalBatches({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value === '' ? '' : status.value,
      keyword: keyword.value
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => {
  loadTidalVisibleIdentities()
  loadData()
})

/** 批次阶段/占位变化自动刷新：30 秒一次（弹窗中的表单不受影响） */
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

/** 状态筛选变化即回到第一页重新查询（与其它列表页 keyword 的处理一致） */
watch(status, () => {
  page.value = 1
  loadData()
})

/**
 * 编辑弹窗是否处于「只读」。
 * 只有「已取消」的批次后端会拒绝修改；「已结束」的批次仍可编辑，
 * 把结束时间改到未来即可重新开放（后端会同步把自动停用的车位恢复为空闲）。
 */
const editLocked = computed(() => isEdit.value && !!editingRow.value && editingRow.value.status === 3)

const resetForm = () => {
  form.batchName = ''
  form.area = ''
  form.spotPrefix = 'TD'
  form.spotCount = 50
  form.floor = ''
  form.pricePerHour = 5
  form.allowIdentities = ''
  form.freeMinutes = 0
  form.openTime = ''
  form.closeTime = ''
  form.remark = ''
  allowIdentityList.value = []
}

const openAddDialog = async () => {
  isEdit.value = false
  editId.value = ''
  editingRow.value = null
  resetForm()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEditDialog = async (row: any) => {
  isEdit.value = true
  editId.value = row.id
  editingRow.value = row
  form.batchName = row.batchName || ''
  form.area = row.area || ''
  form.spotPrefix = row.spotPrefix || ''
  form.spotCount = row.spotCount ?? 1
  form.floor = row.floor || ''
  form.pricePerHour = row.pricePerHour ?? 0
  form.allowIdentities = row.allowIdentities || ''
  form.freeMinutes = row.freeMinutes ?? 0
  form.openTime = row.openTime || ''
  form.closeTime = row.closeTime || ''
  form.remark = row.remark || ''
  allowIdentityList.value = String(row.allowIdentities || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid === false) return
  // 已结束/已取消的批次：后端不再接受时段与车位改动，这里直接拦住并提示
  if (editLocked.value) {
    ElMessage.warning('该批次已结束/已取消，不能再修改')
    return
  }
  saving.value = true
  try {
    // 允许身份数组 -> 逗号分隔字符串（空字符串表示不限身份）；无权限身份不提交
    const payload: TidalBatchForm = {
      batchName: form.batchName.trim(),
      area: form.area.trim(),
      spotPrefix: form.spotPrefix.trim(),
      spotCount: Number(form.spotCount),
      floor: form.floor,
      pricePerHour: Number(form.pricePerHour),
      allowIdentities: allowIdentityList.value.filter((v) => identitySelectable(v)).join(','),
      freeMinutes: Number(form.freeMinutes),
      openTime: form.openTime,
      closeTime: form.closeTime,
      remark: form.remark
    }
    if (isEdit.value) {
      await updateTidalBatch(editId.value, payload)
    } else {
      await createTidalBatch(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

/** 取消批次：后端存在活跃预约或在场车辆时会报错，错误信息由 request 拦截器弹出 */
const handleCancel = async (row: any) => {
  await cancelTidalBatch(row.id)
  ElMessage.success('批次已取消（已预约/在场车辆不受影响）')
  loadData()
}

/** 删除批次：仅当所有车位空闲且无预约/停车历史，后端校验不通过时会报错 */
const handleDelete = async (row: any) => {
  await deleteTidalBatch(row.id)
  ElMessage.success('批次已删除')
  loadData()
}

const onDeleteClick = (row: any) => {
  ElMessageBox.confirm(
    `删除后不可恢复：批次「${row.batchName}」及其 ${row.spotCount} 个车位编号将被移除（存在预约/停车历史时后端会拒绝删除）`,
    '确认删除该批次？',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(() => handleDelete(row))
    .catch(() => {})
}

/** 恢复已取消的批次：按开放/结束时间重算状态，重算为待开放/开放中会自动重新启用车位 */
const handleRestore = async (row: any) => {
  await restoreTidalBatch(row.id)
  ElMessage.success('批次已恢复，将按开放时间重新生效')
  loadData()
}

/** 软删除已取消的批次：仅从管理端列表隐藏，车位与预约/停车记录全部保留 */
const handleSoftDelete = async (row: any) => {
  await softDeleteTidalBatch(row.id)
  ElMessage.success('批次已删除（车位与历史记录仍保留）')
  loadData()
}

/** 彻底删除已取消的批次：物理删除批次与全部车位，并抹除历史记录中的车位归属 */
const onForceDeleteClick = (row: any) => {
  ElMessageBox.confirm(
    `将物理删除批次「${row.batchName}」及其 ${row.spotCount} 个车位，历史记录中的车位将无法追溯，且不可恢复（存在在场/被预约车位时后端会拒绝）`,
    '确认彻底删除该批次？',
    {
      type: 'warning',
      confirmButtonText: '彻底删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    }
  )
    .then(() => handleForceDelete(row))
    .catch(() => {})
}

const handleForceDelete = async (row: any) => {
  await forceDeleteTidalBatch(row.id)
  ElMessage.success('批次及其全部车位已彻底删除')
  loadData()
}

/* ============ 查看车位（抽屉） ============ */
const drawerVisible = ref(false)
const drawerLoading = ref(false)
const detailBatch = ref<TidalBatch | null>(null)
const detailSpots = ref<TidalSpot[]>([])

const openSpotDrawer = async (row: any) => {
  drawerVisible.value = true
  drawerLoading.value = true
  detailBatch.value = row
  detailSpots.value = []
  try {
    const res: any = await getTidalBatch(row.id)
    detailBatch.value = res.data || row
    detailSpots.value = res.data?.spots || []
  } finally {
    drawerLoading.value = false
  }
}

/* ============ 展示辅助 ============ */
const phaseMeta = (row: any) => {
  if (row.status === 3) return PHASE_TAG.cancelled
  return PHASE_TAG[row.phase] || { type: 'info', text: row.phaseLabel || row.statusLabel || '未知' }
}

const phaseText = (row: any) =>
  row.status === 3 ? row.statusLabel || '已取消' : row.phaseLabel || phaseMeta(row).text

/** 允许身份：给中文身份名用 / 分隔成一个个 tag 展示；空串 = 不限身份 */
const identityNames = (row: any) =>
  String(row.zoneIdentityNames || '')
    .split(/[\/,，、]/)
    .map((s) => s.trim())
    .filter(Boolean)

const spotStatusMeta = (s: number) => SPOT_STATUS_TAG[s] || { type: 'info', text: '未知' }

const fmtTime = (t: string) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')

/** 距开放还有几天：仅开放前展示 */
const soonTip = computed(() => {
  const d = detailBatch.value?.daysUntilOpen ?? 0
  if (!detailBatch.value || detailBatch.value.phase !== 'upcoming' || d <= 0) return ''
  return `距开放还有 ${d} 天`
})

const totalText = () => `共 ${total.value} 条`
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>潮汐车位管理（临时车位批次）</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-button type="primary" @click="openAddDialog">
              <el-icon><Sunny /></el-icon>
              <span style="margin-left: 4px">新建临时车位批次</span>
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="潮汐车位用于开学、考试、比赛等高峰期的临时停车；用户在开放时间前 7 天可开始预约，开放时间内才可入场。"
        style="margin-bottom: 12px"
      />

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="批次名称 / 区域"
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
        <el-table-column label="批次名称" prop="batchName" min-width="170" show-overflow-tooltip />

        <el-table-column label="区域" prop="area" min-width="150" show-overflow-tooltip />

        <el-table-column label="编号前缀" prop="spotPrefix" width="100" align="center" />

        <el-table-column label="车位数（总/空闲/占用/预约）" width="190">
          <template #default="{ row }">
            <div class="count-cell">
              <b>{{ row.spotCount }}</b>
              <span class="c-vacant">空闲 {{ row.vacantCount }}</span>
              <span class="c-occupied">占用 {{ row.occupiedCount }}</span>
              <span class="c-reserved">预约 {{ row.reservedCount }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="单价(元/时)" width="105" align="right">
          <template #default="{ row }">{{ row.pricePerHour }}</template>
        </el-table-column>

        <el-table-column label="允许身份" width="170">
          <template #default="{ row }">
            <el-tag v-if="identityNames(row).length === 0" type="primary">不限身份</el-tag>
            <template v-else>
              <el-tag v-for="n in identityNames(row)" :key="n" type="primary" style="margin: 0 4px 4px 0">
                {{ n }}
              </el-tag>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="开放时间" width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <span>{{ fmtTime(row.openTime) }}</span>
              <span>~ {{ fmtTime(row.closeTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前阶段" width="150">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start">
              <el-tag :type="phaseMeta(row).type as any">{{ phaseText(row) }}</el-tag>
              <span v-if="row.status !== 3 && row.phase === 'upcoming' && row.daysUntilOpen > 0" class="sub-text">
                距开放 {{ row.daysUntilOpen }} 天
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>

        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 4px">
              <el-button size="small" @click="openSpotDrawer(row)">查看车位</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <!-- 未取消的批次：保留取消 / 删除入口 -->
              <template v-if="row.status !== 3">
                <el-popconfirm
                  title="取消后该批次车位不再接受新的预约与入场，确定？"
                  width="280"
                  @confirm="handleCancel(row)"
                >
                  <template #reference>
                    <el-button
                      size="small"
                      :style="{ color: '#e6a23c', borderColor: '#e6a23c' }"
                    >
                      取消批次
                    </el-button>
                  </template>
                </el-popconfirm>
                <el-button size="small" type="danger" @click="onDeleteClick(row)">删除</el-button>
              </template>
              <!-- 已取消的批次：提供恢复 / 软删除 / 彻底删除，解死结 -->
              <template v-else>
                <el-popconfirm
                  title="恢复后可按开放时间重新生效，确定恢复？"
                  width="280"
                  @confirm="handleRestore(row)"
                >
                  <template #reference>
                    <el-button
                      size="small"
                      :style="{ color: '#409eff', borderColor: '#409eff' }"
                    >
                      恢复批次
                    </el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm
                  title="软删除后批次从列表隐藏，车位与历史记录仍保留，确定？"
                  width="280"
                  @confirm="handleSoftDelete(row)"
                >
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
                <el-button size="small" type="danger" @click="onForceDeleteClick(row)">彻底删除</el-button>
              </template>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无潮汐车位批次" :image-size="70" />
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

    <!-- 新建 / 编辑批次 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑临时车位批次' : '新建临时车位批次'"
      width="620px"
      :close-on-click-modal="false"
    >
      <!-- 已结束/已取消的批次后端不再接受改动，这里提示并禁止保存 -->
      <el-alert
        v-if="editLocked"
        type="warning"
        show-icon
        :closable="false"
        title="该批次已取消，请先点击「恢复批次」后再修改"
        style="margin-bottom: 12px"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" label-position="right">
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="form.batchName" placeholder="如 2026 春季开学临时停车区" />
        </el-form-item>

        <el-form-item label="显示区域名" prop="area">
          <el-input v-model="form.area" placeholder="如 东门广场临时停车区" />
        </el-form-item>

        <el-form-item label="车位编号前缀" prop="spotPrefix">
          <el-input v-model="form.spotPrefix" placeholder="如 TD" />
          <div class="form-hint">
            最终车位编号为「{{ form.spotPrefix || 'TD' }}-001…{{ form.spotPrefix || 'TD' }}-0NN」，不能与已有车位编号重复
          </div>
        </el-form-item>

        <el-form-item label="车位数" prop="spotCount">
          <el-input-number v-model="form.spotCount" :min="1" :max="500" :precision="0" style="width: 100%" />
        </el-form-item>

        <el-form-item label="楼层/位置" prop="floor">
          <el-input v-model="form.floor" placeholder="如 东门广场地面（选填）" />
        </el-form-item>

        <el-form-item label="每小时单价" prop="pricePerHour">
          <el-input-number v-model="form.pricePerHour" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-form-item label="允许停放身份" prop="allowIdentities">
          <el-checkbox-group v-model="allowIdentityList">
            <el-checkbox
              v-for="i in availableIdentities"
              :key="i.value"
              :value="i.value"
            >
              {{ i.label }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="form-hint">
            全不选 = 不限身份；预约与入场都会按此限制校验。
            未被授予「潮汐临时停车区」可见权限的身份不会出现在选项中。
          </div>
        </el-form-item>

        <el-form-item label="免费时长" prop="freeMinutes">
          <el-input-number v-model="form.freeMinutes" :min="0" :max="1440" :precision="0" style="width: 100%" />
          <div class="form-hint">单位分钟，0 表示按身份规则（教师/学生/家长等各自免费时长）</div>
        </el-form-item>

        <el-form-item label="开放时间" prop="openTime">
          <el-date-picker
            v-model="form.openTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开放时间（精确到分钟）"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="closeTime">
          <el-date-picker
            v-model="form.closeTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间（须晚于开放时间）"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="editLocked"
          @click="handleSave"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看车位 -->
    <el-drawer v-model="drawerVisible" title="批次车位" size="560px">
      <div v-loading="drawerLoading">
        <div v-if="detailBatch" class="drawer-head">
          <div class="dh-title">{{ detailBatch.batchName }}</div>
          <div class="dh-sub">
            {{ detailBatch.area }}
            <span v-if="detailBatch.spotPrefix"> · 编号前缀 {{ detailBatch.spotPrefix }}</span>
            <span v-if="detailBatch.floor"> · {{ detailBatch.floor }}</span>
          </div>
          <div class="dh-tags">
            <el-tag :type="phaseMeta(detailBatch).type as any">{{ phaseText(detailBatch) }}</el-tag>
            <el-tag v-if="soonTip" type="warning">{{ soonTip }}</el-tag>
            <el-tag type="info">
              {{ fmtTime(detailBatch.openTime) }} ~ {{ fmtTime(detailBatch.closeTime) }}
            </el-tag>
          </div>
        </div>

        <div v-if="detailBatch" class="drawer-stats">
          <div class="ds-item">
            <span class="ds-label">车位总数</span>
            <span class="ds-value">{{ detailBatch.spotCount }}</span>
          </div>
          <div class="ds-item">
            <span class="ds-label">空闲</span>
            <span class="ds-value vacant">{{ detailBatch.vacantCount }}</span>
          </div>
          <div class="ds-item">
            <span class="ds-label">已占用</span>
            <span class="ds-value occupied">{{ detailBatch.occupiedCount }}</span>
          </div>
          <div class="ds-item">
            <span class="ds-label">已预约</span>
            <span class="ds-value reserved">{{ detailBatch.reservedCount }}</span>
          </div>
        </div>

        <el-empty v-if="!drawerLoading && detailSpots.length === 0" description="该批次暂无车位" :image-size="70" />

        <div v-else class="spot-grid">
          <div v-for="s in detailSpots" :key="s.id" class="spot-chip">
            <span class="sc-code">{{ s.spotCode }}</span>
            <el-tag size="small" :type="spotStatusMeta(s.status).type as any">
              {{ spotStatusMeta(s.status).text }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-drawer>
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
.count-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}
.count-cell b {
  font-size: 15px;
  color: #1d2129;
}
.count-cell span {
  font-size: 12px;
  color: #606266;
}
.count-cell .c-vacant {
  color: #67c23a;
}
.count-cell .c-occupied {
  color: #e6a23c;
}
.count-cell .c-reserved {
  color: #409eff;
}
.time-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  color: #606266;
}
.drawer-head {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f5f9ff;
  padding: 12px;
  margin-bottom: 12px;
}
.dh-title {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.dh-sub {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}
.dh-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.drawer-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
.ds-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #ffffff;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ds-label {
  font-size: 12px;
  color: #909399;
}
.ds-value {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  font-variant-numeric: tabular-nums;
}
.ds-value.vacant {
  color: #67c23a;
}
.ds-value.occupied {
  color: #e6a23c;
}
.ds-value.reserved {
  color: #409eff;
}
.spot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}
.spot-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 6px 10px;
  background: #ffffff;
}
.sc-code {
  font-weight: 600;
  color: #1d2129;
  font-size: 13px;
}
</style>
