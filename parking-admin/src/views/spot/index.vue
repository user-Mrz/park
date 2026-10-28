<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getSpots, addSpot, updateSpot, updateSpotStatus, deleteSpot, getParkingZones } from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

/** 校园分区（来自「停车区域管理」，仅启用中的区域） */
const ZONE_OPTIONS = ref<{ value: string; label: string }[]>([])

const ZONE_NAME = computed<Record<string, string>>(() =>
  Object.fromEntries(ZONE_OPTIONS.value.map((z) => [z.value, z.label]))
)

/** 加载启用中的停车区域，供车位所属区域下拉使用 */
const loadZones = async () => {
  try {
    const res: any = await getParkingZones({ status: 1 })
    ZONE_OPTIONS.value = (res.data || []).map((z: any) => ({
      value: z.zoneCode,
      label: z.zoneName
    }))
  } catch (e) {
    ZONE_OPTIONS.value = []
  }
}

/** 可停放身份（value = identity_type） */
const IDENTITY_OPTIONS = [
  { value: '1', label: '学生' },
  { value: '2', label: '教师' },
  { value: '3', label: '职工' },
  { value: '4', label: '访客' },
  { value: '5', label: '家长' }
]

const identityLabel = (code: string) => IDENTITY_OPTIONS.find((i) => i.value === code)?.label || code

const STATUS_TAG: Record<number, { type: string; text: string }> = {
  0: { type: 'success', text: '空闲' },
  1: { type: 'warning', text: '占用' },
  2: { type: 'primary', text: '预约' },
  3: { type: 'danger', text: '维护' }
}

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(50)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)

// 详情弹窗
const detailVisible = ref(false)
const detail = ref<any>(null)

// 搜索条件
const searchSpotCode = ref('')
const searchArea = ref('')
const searchZoneCode = ref('')

const formRef = ref<FormInstance>()
const form = reactive<any>({
  spotCode: '',
  area: '',
  zoneCode: 'teaching',
  allowIdentities: [] as string[],
  freeMinutes: 0,
  floor: '',
  pricePerHour: 5,
  remark: ''
})

const rules: FormRules = {
  spotCode: [{ required: true, message: '请输入车位编号', trigger: 'blur' }],
  zoneCode: [{ required: true, message: '请选择校园分区', trigger: 'change' }],
  area: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
  allowIdentities: [{ required: true, message: '请选择可停放的身份', trigger: 'change' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
  pricePerHour: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

const loadData = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    // spotType=0 仅查询常驻车位（spot_type=1 的潮汐车位由「潮汐车位管理」页面维护）
    const params: any = { page: page.value, pageSize: pageSize.value, spotType: 0 }
    const code = searchSpotCode.value.trim()
    if (code) params.spotCode = code
    const areaName = searchArea.value.trim()
    if (areaName) params.area = areaName
    const zone = searchZoneCode.value.trim()
    if (zone) params.zoneCode = zone
    const res: any = await getSpots(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    if (!silent) loading.value = false
  }
}

const onSearch = () => {
  page.value = 1
  loadData()
}

const onReset = () => {
  searchSpotCode.value = ''
  searchArea.value = ''
  searchZoneCode.value = ''
  page.value = 1
  loadData()
}

onMounted(() => {
  loadZones()
  loadData()
})

/** 车位状态自动刷新：30 秒一次（编辑弹窗中的表单不受影响） */
useAutoRefresh(loadData, { intervalMs: 30000 })

const openAddDialog = async () => {
  isEdit.value = false
  editId.value = 0
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  form.spotCode = ''
  form.area = ''
  form.zoneCode = ZONE_OPTIONS.value[0]?.value || ''
  form.allowIdentities = ['1', '2', '3']
  form.freeMinutes = 0
  form.floor = ''
  form.pricePerHour = 5
  form.remark = ''
}

const openEditDialog = async (row: any) => {
  isEdit.value = true
  editId.value = row.id
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  form.spotCode = row.spotCode
  form.area = row.area
  form.zoneCode = row.zoneCode || ZONE_OPTIONS.value[0]?.value || ''
  form.allowIdentities = String(row.allowIdentities || '1,2,3')
    .split(',')
    .map((s: string) => s.trim())
    .filter(Boolean)
  form.freeMinutes = row.freeMinutes ?? 0
  form.floor = row.floor
  form.pricePerHour = row.pricePerHour
  form.remark = row.remark
}

const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    // 可停身份数组 -> 逗号分隔字符串（后端按身份类型判断可停区域）
    // 显式指定 spotType=0（常驻车位），与「潮汐车位管理」页面职责分离
    const payload = {
      ...form,
      allowIdentities: (form.allowIdentities || []).join(','),
      spotType: 0
    }
    if (isEdit.value) {
      await updateSpot(editId.value, payload)
    } else {
      await addSpot(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row: any) => {
  const newStatus = row.status === 3 ? 0 : 3
  await updateSpotStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadData()
}

const handleDelete = async (row: any) => {
  await deleteSpot(row.id)
  ElMessage.success('车位已删除')
  loadData()
}

/** 占用/预约中的车位不允许删除，点击时给出与原 Tooltip 一致的提示 */
const blockedTip = (row: any) => row.status === 1 || row.status === 2

const onDeleteClick = (row: any) => {
  if (blockedTip(row)) {
    ElMessage.warning('该车位占用/预约中，无法删除')
    return
  }
  ElMessageBox.confirm('删除后不可恢复，关联历史记录可能受影响', '确认删除该车位？', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  })
    .then(() => handleDelete(row))
    .catch(() => {})
}

const statusMeta = (status: number) => STATUS_TAG[status] || { type: 'info', text: '未知' }

/** 可停身份字段（逗号分隔字符串）-> 身份 code 数组 */
const allowCodes = (allow: any) =>
  String(allow || '')
    .split(',')
    .map((s: string) => s.trim())
    .filter(Boolean)

const pagedTotalText = () => `共 ${total.value} 条`

// 查看详情：直接复用列表行数据，无需额外请求
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
          <span>常驻车位管理</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-button type="primary" @click="openAddDialog">新增车位</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="searchSpotCode"
          placeholder="车位编号"
          clearable
          style="width: 160px"
          @keyup.enter="onSearch"
        />
        <el-input
          v-model="searchArea"
          placeholder="区域名称"
          clearable
          style="width: 160px"
          @keyup.enter="onSearch"
        />
        <el-select
          v-model="searchZoneCode"
          placeholder="校园分区"
          clearable
          style="width: 180px"
          @change="onSearch"
        >
          <el-option v-for="z in ZONE_OPTIONS" :key="z.value" :label="z.label" :value="z.value" />
        </el-select>
        <el-button type="primary" :icon="'Search'" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <span class="search-hint">支持任意一项独立搜索，无需全部填写</span>
      </div>

      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column label="车位编号" prop="spotCode" width="110" />

        <el-table-column label="校园分区" width="170">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <el-tag type="primary">{{ ZONE_NAME[row.zoneCode] || row.area || '未设置' }}</el-tag>
              <span style="font-size: 12px; color: #606266">{{ row.area || '' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="可停身份" width="170">
          <template #default="{ row }">
            <el-tag v-if="allowCodes(row.allowIdentities).length === 0">所有身份</el-tag>
            <template v-else>
              <el-tag
                v-for="c in allowCodes(row.allowIdentities)"
                :key="c"
                type="primary"
                style="margin-right: 4px"
              >
                {{ identityLabel(c) }}
              </el-tag>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="区域免费时长" width="110" align="right">
          <template #default="{ row }">
            {{ row.freeMinutes && row.freeMinutes > 0 ? `${row.freeMinutes} 分钟` : '按身份规则' }}
          </template>
        </el-table-column>

        <el-table-column label="楼层" prop="floor" width="80" />
        <el-table-column label="单价(元/小时)" prop="pricePerHour" width="120" />

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type as any">{{ statusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注" prop="remark" />

        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center">
              <el-button size="small" type="primary" link @click="viewDetail(row)">详情</el-button>
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.status === 3 ? 'primary' : 'default'"
                :style="
                  row.status !== 3
                    ? { marginLeft: '8px', color: '#E6A23C', borderColor: '#E6A23C' }
                    : { marginLeft: '8px' }
                "
                @click="toggleStatus(row)"
              >
                {{ row.status === 3 ? '恢复' : '维护' }}
              </el-button>
              <el-tooltip :content="blockedTip(row) ? '该车位占用/预约中，无法删除' : ''">
                <el-button
                  size="small"
                  type="danger"
                  style="margin-left: 8px"
                  :disabled="blockedTip(row)"
                  @click="onDeleteClick(row)"
                >
                  删除
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无车位数据" :image-size="70" />
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
          :total-text="pagedTotalText"
          @current-change="() => loadData()"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑车位' : '新增车位'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
        <el-form-item label="车位编号" prop="spotCode">
          <el-input v-model="form.spotCode" placeholder="如 A-001" />
        </el-form-item>

        <el-form-item label="校园分区" prop="zoneCode">
          <el-select v-model="form.zoneCode" placeholder="选择校园分区" style="width: 100%">
            <el-option v-for="z in ZONE_OPTIONS" :key="z.value" :label="z.label" :value="z.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="区域名称" prop="area">
          <el-input v-model="form.area" placeholder="如 教学楼区" />
        </el-form-item>

        <el-form-item label="可停身份" prop="allowIdentities">
          <el-select
            v-model="form.allowIdentities"
            multiple
            placeholder="可停放的身份（可多选）"
            style="width: 100%"
          >
            <el-option v-for="i in IDENTITY_OPTIONS" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
          <div style="font-size: 12px; color: #606266; line-height: 1.5">预约与入场都会按此限制校验</div>
        </el-form-item>

        <el-form-item label="区域免费时长" prop="freeMinutes">
          <el-input-number
            v-model="form.freeMinutes"
            :min="0"
            :max="1440"
            :precision="0"
            style="width: 100%"
            placeholder="分钟，0=按身份规则"
          />
          <div style="font-size: 12px; color: #606266; line-height: 1.5">
            0 表示按身份规则（教师/学生/家长等各自免费时长）
          </div>
        </el-form-item>

        <el-form-item label="楼层" prop="floor">
          <el-input v-model="form.floor" placeholder="如 B1" />
        </el-form-item>

        <el-form-item label="单价(元/时)" prop="pricePerHour">
          <el-input-number v-model="form.pricePerHour" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="备注信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 车位详情弹窗 -->
    <el-dialog v-model="detailVisible" title="车位详情" width="560px" destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="车位ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="车位编号">{{ detail.spotCode }}</el-descriptions-item>
        <el-descriptions-item label="校园分区">
          {{ ZONE_NAME[detail.zoneCode] || detail.zoneCode || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="区域名称">{{ detail.area || '—' }}</el-descriptions-item>
        <el-descriptions-item label="楼层">{{ detail.floor || '—' }}</el-descriptions-item>
        <el-descriptions-item label="单价(元/小时)">{{ detail.pricePerHour ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="区域免费时长">
          {{ detail.freeMinutes && detail.freeMinutes > 0 ? `${detail.freeMinutes} 分钟` : '按身份规则' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMeta(detail.status).type as any">{{ statusMeta(detail.status).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可停身份" :span="2">
          <template v-if="allowCodes(detail.allowIdentities).length === 0">所有身份</template>
          <template v-else>
            <el-tag
              v-for="c in allowCodes(detail.allowIdentities)"
              :key="c"
              type="primary"
              style="margin-right: 4px"
            >
              {{ identityLabel(c) }}
            </el-tag>
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
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
