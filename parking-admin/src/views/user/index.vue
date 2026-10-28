<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUsers, getUserDetail, updateUserStatus, auditUserIdentity, getDashboard } from '@/api'
import { getUserRoles, changeUserRole } from '@/api/admin'

/** 校园身份类型 */
const IDENTITY_META: Record<number, { text: string; type: string }> = {
  1: { text: '学生', type: 'primary' },
  2: { text: '教师', type: 'success' },
  3: { text: '职工', type: 'warning' },
  4: { text: '访客', type: 'info' },
  5: { text: '家长', type: 'primary' }
}

/** 身份认证状态 */
const AUDIT_META: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'primary' },
  1: { text: '已认证', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

const IDENTITY_OPTIONS = Object.keys(IDENTITY_META).map((k) => ({
  value: Number(k),
  label: IDENTITY_META[Number(k)].text
}))

const AUDIT_OPTIONS = Object.keys(AUDIT_META).map((k) => ({
  value: Number(k),
  label: AUDIT_META[Number(k)].text
}))

const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

function maskOpenid(openid: any) {
  const s = String(openid || '')
  if (!s) return '未绑定微信'
  return s.length <= 10 ? s : s.slice(0, 6) + '****' + s.slice(-4)
}

const route = useRoute()
const message = ElMessage

const loading = ref(false)
const tableData = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref<number | undefined>(undefined)
const filterIdentity = ref<number | undefined>(undefined)
// 支持从首页「待审核身份」卡片跳转过来时直接筛选：/users?identityStatus=0
const filterAudit = ref<number | undefined>((() => {
  const v = route.query.identityStatus
  if (v === undefined || v === null || v === '') return undefined
  return Number(v)
})())

const auditTarget = ref<any>(null)
const auditResult = ref<number>(1)
const auditRemark = ref('')
const auditOpen = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentUser = ref<any>(null)
const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  disabledUsers: 0,
  pendingUsers: 0,
  totalBalance: '0.00'
})

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value, keyword: keyword.value }
    if (filterStatus.value !== undefined) {
      params.status = filterStatus.value
    }
    if (filterIdentity.value !== undefined) params.identityType = filterIdentity.value
    if (filterAudit.value !== undefined) params.identityStatus = filterAudit.value
    const res: any = await getUsers(params)
    tableData.value = res.data.records
    total.value = res.data.total

    // 加载统计数据（用户数从分页接口total获取，余额从dashboard获取）
    const [allRes, activeRes, disabledRes, pendingRes] = await Promise.all([
      getUsers({ page: 1, pageSize: 1 }),
      getUsers({ page: 1, pageSize: 1, status: 1 }),
      getUsers({ page: 1, pageSize: 1, status: 0 }),
      getUsers({ page: 1, pageSize: 1, identityStatus: 0 })
    ])
    let balance = '0.00'
    try {
      const dashRes: any = await getDashboard()
      balance = (Number(dashRes.data.totalUserBalance) || 0).toFixed(2)
    } catch {
      /* fallback */
    }
    stats.totalUsers = allRes.data.total
    stats.activeUsers = activeRes.data.total
    stats.disabledUsers = disabledRes.data.total
    stats.pendingUsers = pendingRes.data.total
    stats.totalBalance = balance
  } finally {
    loading.value = false
  }
}

/* ============ 用户角色字典与调整角色 ============ */
// roleCode -> 角色名称
const roleMap = ref<Record<string, string>>({})
// 角色下拉选项（来自「用户角色」接口的 records）
const roleOptions = ref<any[]>([])

const roleNameOf = (code: any) => (code ? roleMap.value[code] || code : '—')

const loadRoleOptions = async () => {
  try {
    const res: any = await getUserRoles()
    const records: any[] = res.data?.records || []
    roleOptions.value = records
    const m: Record<string, string> = {}
    records.forEach((r: any) => {
      m[r.roleCode] = r.roleName
    })
    roleMap.value = m
  } catch (e) {
    // 拉取失败时降级：直接显示 roleCode
  }
}

// 调整角色
const roleDialogVisible = ref(false)
const roleTarget = ref<any>(null)
const targetRoleCode = ref('')

const openChangeRole = (row: any) => {
  roleTarget.value = row
  targetRoleCode.value = row.roleCode || ''
  roleDialogVisible.value = true
}

const submitChangeRole = async () => {
  if (!roleTarget.value) return
  if (!targetRoleCode.value) {
    message.warning('请选择目标角色')
    return
  }
  try {
    await changeUserRole(roleTarget.value.id, { roleCode: targetRoleCode.value })
    message.success('角色调整成功')
    roleDialogVisible.value = false
    loadData()
  } catch (e) {
    // 拦截器已弹错误提示
  }
}

onMounted(() => {
  loadData()
  loadRoleOptions()
})

const handleFilter = (status: number | undefined) => {
  page.value = 1
  filterStatus.value = status
  loadData()
}

const showDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  currentUser.value = null
  try {
    const res: any = await getUserDetail(row.id)
    currentUser.value = res.data
  } catch {
    /* 错误提示由 request 拦截器统一处理 */
  } finally {
    detailLoading.value = false
  }
}

const toggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  message.success(newStatus === 1 ? '已启用该用户' : '已禁用该用户')
  loadData()
  // 详情抽屉打开时同步刷新
  if (
    detailVisible.value &&
    currentUser.value &&
    String(currentUser.value.id) === String(row.id)
  ) {
    showDetail({ id: row.id })
  }
}

const openAudit = (row: any, result: number = 1) => {
  auditTarget.value = row
  auditResult.value = result
  auditRemark.value = ''
  auditOpen.value = true
}

const submitAudit = async () => {
  if (!auditTarget.value) return
  await auditUserIdentity(auditTarget.value.id, auditResult.value, auditRemark.value)
  const label =
    auditResult.value === 1 ? '已通过身份认证' : auditResult.value === 2 ? '已驳回身份认证' : '已重置为待审核'
  message.success(label)
  auditOpen.value = false
  loadData()
  if (
    detailVisible.value &&
    currentUser.value &&
    String(currentUser.value.id) === String(auditTarget.value.id)
  ) {
    showDetail({ id: auditTarget.value.id })
  }
}

const statCards = computed(() => [
  { label: '用户总数', value: stats.totalUsers, cls: '', onClick: () => handleFilter(undefined) },
  { label: '正常用户', value: stats.activeUsers, cls: 'active', onClick: () => handleFilter(1) },
  { label: '禁用用户', value: stats.disabledUsers, cls: 'disabled', onClick: () => handleFilter(0) },
  {
    label: '待审核身份',
    value: stats.pendingUsers,
    cls: 'pending',
    onClick: () => {
      page.value = 1
      filterAudit.value = 0
      loadData()
    }
  },
  {
    label: '账户总余额',
    value: `¥${stats.totalBalance}`,
    cls: 'balance',
    onClick: undefined as undefined | (() => void)
  }
])

const biz = computed<any>(() => currentUser.value?.stats || {})

const bizCards = computed(() => [
  {
    label: '停车次数',
    value: biz.value.vehicleRecordCount ?? 0,
    extra: biz.value.parkingNow ? `在场 ${biz.value.parkingNow} 辆` : '当前无在场车辆'
  },
  {
    label: '预约总数',
    value: biz.value.reservationCount ?? 0,
    extra: biz.value.activeReservationCount ? `进行中 ${biz.value.activeReservationCount} 个` : '无进行中预约'
  },
  { label: '累计充值', value: fmtMoney(biz.value.rechargeAmount), extra: `${biz.value.rechargeCount ?? 0} 笔` },
  { label: '累计消费', value: fmtMoney(biz.value.paymentAmount), extra: '停车扣费合计' },
  {
    label: '长租月卡',
    value: biz.value.monthlyPassActive ? '生效中' : '未开通',
    extra: biz.value.monthlyPassActive
      ? `${biz.value.monthlyPassPlate || ''} · ${biz.value.monthlyPassExpireDate || ''} 到期`
      : `累计 ${biz.value.monthlyPassCount ?? 0} 次`
  },
  {
    label: '月卡累计支出',
    value: fmtMoney(biz.value.monthlyPassAmount),
    extra: '已排除退款记录'
  }
])

const auditOkText = computed(() =>
  auditResult.value === 1 ? '通过认证' : auditResult.value === 2 ? '驳回' : '重置为待审核'
)

const auditRemarkPlaceholder = computed(() =>
  auditResult.value === 1 ? '审核意见（可选，如：学号核验通过）' : '处理原因（建议填写）'
)

const identityText = (t: any) => (IDENTITY_META[t] || { text: '未知' }).text
const identityType = (t: any) => (IDENTITY_META[t] || { type: 'info' }).type
const auditText = (t: any) => (AUDIT_META[t] || AUDIT_META[0]).text
const auditType = (t: any) => (AUDIT_META[t] || AUDIT_META[0]).type

/** 复制用户ID（与原管理端一致：点击即复制并提示） */
const copyId = async (id: any) => {
  const text = String(id ?? '')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('用户ID已复制')
  } catch {
    ElMessage.warning(`复制失败，请手动复制：${text}`)
  }
}

const hasFilter = computed(
  () => filterStatus.value !== undefined || filterIdentity.value !== undefined || filterAudit.value !== undefined
)

const clearFilters = () => {
  filterIdentity.value = undefined
  filterAudit.value = undefined
  handleFilter(undefined)
}

const goPending = () => {
  page.value = 1
  filterAudit.value = 0
  loadData()
}
</script>

<template>
  <div class="user-page">
    <div class="stats-row">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="stat-card-item"
        :class="card.cls"
        :style="{
          cursor: card.onClick ? 'pointer' : 'default',
          border:
            card.cls === 'pending' && stats.pendingUsers > 0
              ? '2px solid #409eff'
              : '2px solid transparent'
        }"
        @click="card.onClick && card.onClick()"
      >
        <div
          class="stat-value"
          :style="{
            color: card.cls === 'pending' && stats.pendingUsers > 0 ? '#409EFF' : '#1d2129'
          }"
        >
          {{ card.value }}
        </div>
        <div class="stat-label">{{ card.label }}</div>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>
            用户列表
            <el-tag
              v-if="stats.pendingUsers > 0"
              type="primary"
              style="margin-left: 8px; cursor: pointer"
              @click="goPending"
            >
              待审核身份 {{ stats.pendingUsers }} 人
            </el-tag>
            <el-tag v-if="filterIdentity !== undefined" type="primary" style="margin-left: 8px">
              {{ identityText(filterIdentity) }}
            </el-tag>
            <el-tag v-if="filterAudit !== undefined" type="primary" style="margin-left: 8px">
              {{ auditText(filterAudit) }}
            </el-tag>
            <el-tag v-if="filterStatus === 1" type="success" style="margin-left: 8px">正常</el-tag>
            <el-tag v-if="filterStatus === 0" type="danger" style="margin-left: 8px">禁用</el-tag>
            <el-button v-if="hasFilter" type="primary" link size="small" style="margin-left: 4px" @click="clearFilters">
              清除筛选
            </el-button>
          </span>

          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-select
              v-model="filterIdentity"
              clearable
              placeholder="校园身份"
              style="width: 120px"
              @change="() => { page = 1; loadData() }"
            >
              <el-option v-for="o in IDENTITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>

            <el-select
              v-model="filterAudit"
              clearable
              placeholder="认证状态"
              style="width: 120px"
              @change="() => { page = 1; loadData() }"
            >
              <el-option v-for="o in AUDIT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>

            <el-input
              v-model="keyword"
              placeholder="搜索手机号/昵称/车牌/学号"
              clearable
              style="width: 240px"
              @keyup.enter="() => { page = 1; loadData() }"
            >
              <template #suffix>
                <el-icon style="cursor: pointer" @click="() => { page = 1; loadData() }"><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" row-key="id" size="default" style="width: 100%">
        <el-table-column label="用户" width="210">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px">
              <el-avatar :size="36" :src="row.avatar || undefined">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div style="min-width: 0">
                <div style="font-weight: 600; color: #1d2129">{{ row.nickname || '（未设置昵称）' }}</div>
                <el-tooltip content="点击复制用户ID">
                  <span class="copy-id" @click="copyId(row.id)">ID {{ String(row.id).slice(0, 8) }}…</span>
                </el-tooltip>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="校园身份" width="220">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px">
              <div style="display: flex; gap: 6px; align-items: center">
                <el-tag :type="identityType(row.identityType) as any">{{ identityText(row.identityType) }}</el-tag>
                <el-tag :type="auditType(row.identityStatus) as any">{{ auditText(row.identityStatus) }}</el-tag>
              </div>
              <span style="font-size: 12px; color: #606266">
                {{ row.userNo ? `学号/工号 ${row.userNo}` : '未填写学号/工号' }}
                {{ row.department ? ` · ${row.department}` : '' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag type="info">{{ roleNameOf(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="130">
          <template #default="{ row }">
            <span v-if="row.phone">{{ row.phone }}</span>
            <span v-else style="color: #606266">未绑定</span>
          </template>
        </el-table-column>

        <el-table-column label="车牌号" width="120">
          <template #default="{ row }">
            <span v-if="row.plateNumber">{{ row.plateNumber }}</span>
            <span v-else style="color: #606266">未设置</span>
          </template>
        </el-table-column>

        <el-table-column label="余额" width="120" align="right">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ fmtMoney(row.balance) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="165">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 2px; flex-wrap: wrap">
              <el-button type="primary" link size="small" @click="showDetail(row)">查看信息</el-button>
              <el-button type="primary" link size="small" @click="openChangeRole(row)">调整角色</el-button>

              <!-- 身份审核入口对所有用户开放：待审核可直接通过/驳回，已认证可复核或重置 -->
              <el-button
                type="primary"
                link
                size="small"
                :class="{ 'danger-link': row.identityStatus === 0 }"
                @click="openAudit(row, 1)"
              >
                {{ row.identityStatus === 0 ? '身份审核' : '复核/驳回' }}
              </el-button>

              <el-popconfirm
                :title="
                  row.status === 1
                    ? '确定禁用该用户？禁用后将取消其预约并结算在停车辆'
                    : '确定启用该用户？'
                "
                width="280"
                @confirm="toggleStatus(row)"
              >
                <template #reference>
                  <el-button type="primary" link size="small" :class="{ 'danger-link': row.status === 1 }">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无用户数据" :image-size="70" />
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
          @current-change="() => loadData()"
        />
      </div>
    </el-card>

    <!-- 用户信息抽屉 -->
    <el-drawer v-model="detailVisible" title="用户信息" size="620px" destroy-on-close>
      <div v-if="detailLoading" style="text-align: center; padding: 80px 0">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>
      <el-empty v-else-if="!currentUser" description="未获取到用户信息" />

      <template v-else>
        <!-- 头部：头像 + 昵称 + 状态 -->
        <div class="detail-head">
          <el-avatar :size="64" :src="currentUser.avatar || undefined">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div style="flex: 1; min-width: 0">
            <div style="font-size: 18px; font-weight: 700; color: #1d2129">
              {{ currentUser.nickname || '（未设置昵称）' }}
            </div>
            <div style="margin-top: 6px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
              <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
                {{ currentUser.status === 1 ? '账号正常' : '账号已禁用' }}
              </el-tag>
              <span style="color: #606266">{{ currentUser.phone || '未绑定手机号' }}</span>
              <el-tag v-if="currentUser.plateNumber" type="primary">{{ currentUser.plateNumber }}</el-tag>
            </div>
          </div>
          <div style="text-align: right">
            <div style="font-size: 12px; color: #606266">账户余额</div>
            <div style="font-size: 20px; font-weight: 700; color: #409EFF">{{ fmtMoney(currentUser.balance) }}</div>
          </div>
        </div>

        <!-- 业务统计 -->
        <div class="section-title">业务统计</div>
        <div class="biz-grid">
          <div v-for="c in bizCards" :key="c.label" class="biz-card">
            <div style="font-size: 13px; color: #606266; margin-bottom: 6px">{{ c.label }}</div>
            <div style="font-size: 20px; font-weight: 700; color: #1d2129">{{ c.value }}</div>
            <div style="font-size: 12px; color: #909399; margin-top: 4px">{{ c.extra }}</div>
          </div>
        </div>

        <!-- 账号资料 -->
        <div class="section-title">账号资料</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="用户 ID">
            <span style="font-size: 12px">{{ String(currentUser.id) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="校园身份">
            <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
              <el-tag :type="identityType(currentUser.identityType) as any">
                {{ currentUser.identityLabel || identityText(currentUser.identityType) }}
              </el-tag>
              <!-- 认证状态以后端返回的 identityStatus 为准；缺失时显示“—”而不是误导性的“待审核” -->
              <el-tag v-if="currentUser.identityStatus === undefined || currentUser.identityStatus === null">—</el-tag>
              <el-tag v-else :type="auditType(currentUser.identityStatus) as any">
                {{ currentUser.identityStatusLabel || auditText(currentUser.identityStatus) }}
              </el-tag>
            </div>
          </el-descriptions-item>

          <el-descriptions-item label="学号 / 工号">{{ currentUser.userNo || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="院系 / 部门">{{ currentUser.department || '-' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ currentUser.className || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentUser.identityRemark" label="审核备注">
            {{ currentUser.identityRemark }}
          </el-descriptions-item>
          <el-descriptions-item label="微信绑定">{{ maskOpenid(currentUser.openid) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="车牌号">{{ currentUser.plateNumber || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="头像">
            <a v-if="currentUser.avatar" :href="currentUser.avatar" target="_blank" rel="noreferrer">查看原图</a>
            <span v-else>未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ fmtTime(currentUser.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="资料更新时间">{{ fmtTime(currentUser.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="最近一次停车">
            {{
              biz.lastEntryTime
                ? `${fmtTime(biz.lastEntryTime)}${biz.lastPlateNumber ? ' · ' + biz.lastPlateNumber : ''}`
                : '暂无停车记录'
            }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px; display: flex; gap: 12px; justify-content: flex-end; flex-wrap: wrap">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="danger" @click="openAudit(currentUser, 2)">驳回认证</el-button>
          <el-button type="primary" @click="openAudit(currentUser, 1)">认证通过</el-button>
          <el-popconfirm
            :title="currentUser.status === 1 ? '确定禁用该用户？' : '确定启用该用户？'"
            width="240"
            @confirm="toggleStatus(currentUser)"
          >
            <template #reference>
              <el-button type="primary" :class="{ 'danger-btn': currentUser.status === 1 }">
                {{ currentUser.status === 1 ? '禁用账号' : '启用账号' }}
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </template>
    </el-drawer>

    <!-- 调整用户角色 -->
    <el-dialog v-model="roleDialogVisible" title="调整用户角色" width="440px">
      <div style="margin-bottom: 12px; color: #606266">
        用户：<b>{{ roleTarget?.nickname || roleTarget?.phone || '-' }}</b>　当前角色：
        <b>{{ roleNameOf(roleTarget?.roleCode) }}</b>
      </div>
      <el-select v-model="targetRoleCode" placeholder="请选择目标角色" style="width: 100%">
        <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangeRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 校园身份审核 -->
    <el-dialog v-model="auditOpen" title="校园身份认证审核" width="520px">
      <div style="margin-bottom: 12px; color: #606266">
        用户：<b>{{ auditTarget?.nickname || '-' }}</b>　身份：
        {{ identityText(auditTarget?.identityType) }}　学号/工号：
        <b>{{ auditTarget?.userNo || '未填写' }}</b>
        <div style="margin-top: 4px; font-size: 12px; color: #606266">
          {{ auditTarget?.department || '未填写院系' }}{{ auditTarget?.className ? ` · ${auditTarget.className}` : '' }}
          　当前状态：{{ auditText(auditTarget?.identityStatus) }}
        </div>
      </div>

      <div style="margin-bottom: 12px">
        <div style="font-size: 13px; color: #606266; margin-bottom: 6px">审核结果</div>
        <el-radio-group v-model="auditResult">
          <el-radio-button :value="1">通过认证</el-radio-button>
          <el-radio-button :value="2">驳回</el-radio-button>
          <el-radio-button :value="0">重置为待审核</el-radio-button>
        </el-radio-group>
        <div style="font-size: 12px; color: #606266; margin-top: 6px">
          通过后该用户可预约车位并享受校园优惠；驳回或重置后按访客规则计费。
        </div>
      </div>

      <el-input
        v-model="auditRemark"
        type="textarea"
        :rows="3"
        :placeholder="auditRemarkPlaceholder"
      />

      <template #footer>
        <el-button @click="auditOpen = false">取消</el-button>
        <el-button :type="auditResult === 2 ? 'danger' : 'primary'" @click="submitAudit">
          {{ auditOkText }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-page {
  padding: 0;
}

/* 用户ID：可点击复制 */
.copy-id {
  font-size: 12px;
  color: #606266;
  cursor: pointer;
}
.copy-id:hover {
  color: #409eff;
}

/* 顶部统计卡：与原 antd Row/Col 断点近似的自适应网格 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.stat-card-item {
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px 16px;
  text-align: center;
  transition: all 0.2s;
}
.stat-card-item:hover {
  background: #f5f9ff;
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 13px;
  color: #606266;
}

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

.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 12px;
}

.biz-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.biz-card {
  background: #f5f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 14px 16px;
}

/* 表格/抽屉内的红色文字按钮（替代原 antd 的 danger link / danger 主按钮） */
:deep(.danger-link.el-button--primary.is-link) {
  color: #f56c6c;
}
:deep(.danger-link.el-button--primary.is-link:hover) {
  color: #f89898;
}
.danger-btn {
  --el-button-bg-color: #f56c6c;
  --el-button-border-color: #f56c6c;
  --el-button-hover-bg-color: #f89898;
  --el-button-hover-border-color: #f89898;
  --el-button-active-bg-color: #dd6161;
  --el-button-active-border-color: #dd6161;
}
</style>
