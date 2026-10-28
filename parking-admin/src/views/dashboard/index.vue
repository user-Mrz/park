<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDashboard, getSpotMap, getCampusStats, getCampusDailyStats, getViolations, exportCsv } from '@/api'
import SpotMap from '@/components/SpotMap.vue'
import HeaderClock from '@/components/HeaderClock.vue'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useSpotSocket, type SpotSocketMessage } from '@/hooks/useSpotSocket'
import { useUserStore } from '@/stores/user'

/** ECharts 拆分为独立异步 chunk：首页框架先渲染，图表组件加载完成后再绘制 */
const EChart = defineAsyncComponent(() => import('@/components/EChart.vue'))

const router = useRouter()
const userStore = useUserStore()

/** 检查是否有某个菜单权限（超管默认全部有） */
const hasMenu = (menuCode: string) => {
  return userStore.role === 'super_admin' || userStore.myMenus.includes(menuCode)
}

const fmtNum = (v: any) => Number(v || 0).toLocaleString('zh-CN')
const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(5, 16) : '-')

/** 大屏图表公共暗色配置 */
const AXIS = {
  axisLabel: { color: '#606266', fontSize: 11 },
  axisLine: { lineStyle: { color: '#e4e7ed' } },
  axisTick: { show: false }
}
const SPLIT = { splitLine: { lineStyle: { color: '#f0f2f5' } } }

const dash = ref<any>({})
const mapData = ref<any>({ spots: [], stats: {} })
const campus = ref<any>({ totals: {}, byIdentity: [], byDepartment: [], byZone: [] })
const violations = ref<any[]>([])
const error = ref(false)
const now = ref(new Date())
const exporting = ref('')

/* ================= 图表统计区间（营业统计 / 停车统计共用） ================= */
type RangeKey = '7' | '30' | 'custom'
const rangeKey = ref<RangeKey>('7')
const customRange = ref<any>(null)
const daily = ref<any[]>([])

const fmtDay = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/** 当前区间的起止日期（yyyy-MM-dd）；自定义未选满时回退为近 7 天 */
const rangeDates = computed<[string, string]>(() => {
  if (rangeKey.value === 'custom' && customRange.value && customRange.value.length === 2) {
    const [a, b] = customRange.value
    return a <= b ? [a, b] : [b, a]
  }
  const span = rangeKey.value === '30' ? 30 : 7
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (span - 1))
  return [fmtDay(start), fmtDay(end)]
})

const rangeLabel = computed(() => {
  const [s, e] = rangeDates.value
  return `${s.slice(5)} ~ ${e.slice(5)}`
})

const doExport = async (type: string, label: string) => {
  exporting.value = type
  try {
    await exportCsv(type)
    ElMessage.success(`${label}报表已开始下载`)
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = ''
  }
}

const loadAll = async (silent = false) => {
  try {
    const [startDate, endDate] = rangeDates.value
    // 根据权限决定是否发起对应请求，无权限的接口直接返回空数据
    const requests: Promise<any>[] = [
      getDashboard(),
      getSpotMap(),
      getCampusStats(),
      getCampusDailyStats({ startDate, endDate })
    ]
    if (hasMenu('violations')) {
      requests.push(getViolations({ page: 1, pageSize: 12 }))
    } else {
      requests.push(Promise.resolve({ data: { records: [] } }))
    }
    const [d, m, c, ds, v]: any[] = await Promise.all(requests)
    dash.value = d.data
    mapData.value = { spots: m.data?.spots || [], stats: m.data || {} }
    campus.value = {
      totals: c.data?.totals || {},
      byIdentity: c.data?.byIdentity || [],
      byDepartment: c.data?.byDepartment || [],
      byZone: c.data?.byZone || []
    }
    daily.value = ds.data?.days || []
    violations.value = v.data?.records || []
    error.value = false
  } catch (e) {
    // 自动刷新失败时不把页面切成"加载失败"，保留上一次的数据
    if (!silent) error.value = true
  }
}

/** 切换统计区间时只重新拉取区间日统计，避免整页重复请求 */
const loadDaily = async () => {
  try {
    const [startDate, endDate] = rangeDates.value
    const res: any = await getCampusDailyStats({ startDate, endDate })
    daily.value = res.data?.days || []
  } catch {
    daily.value = []
  }
}

watch(rangeDates, loadDaily)

/** 切换到自定义时预填当前区间，避免图表与选择器口径不一致 */
watch(rangeKey, (v) => {
  if (v === 'custom' && !customRange.value) customRange.value = [...rangeDates.value]
})

/** 不允许选择未来日期（后端也会把区间截止到当天） */
const disableFutureDate = (d: Date) => d.getTime() > Date.now()

loadAll()

/** 大屏统计自动刷新：30 秒一次（标签页后台自动暂停，前端不再展示开关组件） */
useAutoRefresh(loadAll, { intervalMs: 30000 })

const { subscribe: subscribeSpotSocket, unsubscribe: unsubscribeSpotSocket } = useSpotSocket()


const applySpotCounts = (message: SpotSocketMessage) => {
  const counts = message.counts
  if (!counts) return
  dash.value.availableSpots = counts.vacant
  dash.value.totalSpots = counts.total
}

onMounted(() => subscribeSpotSocket(applySpotCounts))
onBeforeUnmount(() => unsubscribeSpotSocket(applySpotCounts))

/* ================= 左侧：停车区域统计卡 ================= */
const zoneCards = computed<{ name: string; occupied: number; vacant: number }[]>(() => {
  const zones = (campus.value.byZone || []).map((z: any) => ({
    name: z.zoneName,
    occupied: Number(z.occupiedCount) || 0,
    vacant: Number(z.vacantCount) || 0
  }))
  if (zones.length > 0) return zones.slice(0, 6)
  // 兜底：由车位地图分组统计
  const groups: Record<string, { occupied: number; vacant: number }> = {}
  ;(mapData.value.spots || []).forEach((s: any) => {
    const key = s.area || '未分组'
    groups[key] = groups[key] || { occupied: 0, vacant: 0 }
    if (Number(s.displayStatus) === 0) groups[key].vacant++
    else groups[key].occupied++
  })
  return Object.keys(groups)
    .sort((a, b) => a.localeCompare(b, 'zh-CN'))
    .slice(0, 6)
    .map((name) => ({ name, ...groups[name] }))
})

const zoneBadge = 'ABCDEFGH'

/* ================= 左侧：营业统计（区间每日收入柱状图） ================= */
const dailyLabels = computed(() => daily.value.map((d: any) => String(d.date || '').slice(5)))

const revenueOption = computed<any>(() => {
  if (daily.value.length === 0) {
    return {
      title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { fontSize: 13, color: '#909399' } }
    }
  }
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,.96)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#1d2129' },
      valueFormatter: (v: any) => fmtMoney(v)
    },
    grid: { left: 8, right: 14, top: 26, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: dailyLabels.value,
      ...AXIS,
      axisLabel: { ...AXIS.axisLabel, hideOverlap: true }
    },
    yAxis: { type: 'value', ...AXIS, ...SPLIT },
    series: [
      {
        name: '每日收入',
        type: 'bar',
        barMaxWidth: 22,
        data: daily.value.map((d: any) => Number(d.revenue) || 0),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: 'rgba(64,158,255,0.35)' }
            ]
          }
        }
      }
    ]
  }
})

/* ================= 左侧：停车统计（区间每日入场车次柱状图） ================= */
const usageOption = computed<any>(() => {
  if (daily.value.length === 0) {
    return {
      title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { fontSize: 13, color: '#909399' } }
    }
  }
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,.96)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#1d2129' },
      valueFormatter: (v: any) => `${v} 车次`
    },
    grid: { left: 8, right: 14, top: 26, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: dailyLabels.value,
      ...AXIS,
      axisLabel: { ...AXIS.axisLabel, hideOverlap: true }
    },
    yAxis: { type: 'value', minInterval: 1, ...AXIS, ...SPLIT },
    series: [
      {
        name: '入场车次',
        type: 'bar',
        barMaxWidth: 22,
        data: daily.value.map((d: any) => Number(d.vehicleCount) || 0),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#67c23a' },
              { offset: 1, color: 'rgba(103,194,58,0.35)' }
            ]
          }
        }
      }
    ]
  }
})

/* ================= 底部指标条 ================= */
const statCards = computed(() => {
  const all = [
    { label: '车位总数(个)', value: fmtNum(dash.value.totalSpots), icon: 'Location', to: '/spots', menu: 'spots' },
    { label: '在场车辆(辆)', value: fmtNum(dash.value.parkedVehicles), icon: 'Van', to: '/vehicles', menu: 'vehicles' },
    { label: '可用车位(个)', value: fmtNum(dash.value.availableSpots), icon: 'CircleCheck', to: '/spots', menu: 'spots' },
    { label: '今日预约(笔)', value: fmtNum(dash.value.todayReservations), icon: 'Calendar', to: '/reservations', menu: 'reservations' },
    { label: '今日收入(元)', value: Number(dash.value.todayRevenue || 0).toFixed(2), icon: 'Money', to: '/payments', menu: 'payments' },
    { label: '总用户数(人)', value: fmtNum(dash.value.totalUsers), icon: 'User', to: '/users', menu: 'users' }
  ]
  // 只保留有权限的卡片；无权限的显示为空（不渲染）
  return all.filter((c) => hasMenu(c.menu))
})

/* ================= 右侧：待办告警 ================= */
const alarms = computed(() => {
  const totals = campus.value.totals || {}
  const all = [
    { label: '待处理违规', count: Number(totals.pendingViolations) || 0, to: '/violations', critical: true, menu: 'violations' },
    { label: '待审访客报备', count: Number(totals.pendingVisitReports) || 0, to: '/visits', critical: true, menu: 'visits' },
    { label: '待审核身份', count: Number(totals.pendingUsers) || 0, to: '/users?identityStatus=0', critical: false, menu: 'users' },
    { label: '黑名单车辆', count: Number(totals.blacklist) || 0, to: '/blacklist', critical: true, menu: 'blacklist' }
  ]
  // 只保留有权限的告警项；无权限的显示为空（不渲染）
  return all.filter((a) => hasMenu(a.menu))
})
const alarmTotal = computed(() => alarms.value.reduce((s, a) => s + a.count, 0))

const exportLabels: Record<string, string> = {
  users: '用户',
  reservations: '预约',
  payments: '扣费',
  violations: '违规',
  visits: '访客报备'
}

/** 根据权限过滤可导出的报表类型 */
const exportOptions = computed(() => {
  const all = [
    { key: 'users', label: '导出用户报表', menu: 'users' },
    { key: 'reservations', label: '导出预约报表', menu: 'reservations' },
    { key: 'payments', label: '导出扣费报表', menu: 'payments' },
    { key: 'violations', label: '导出违规报表', menu: 'violations' },
    { key: 'visits', label: '导出访客报备', menu: 'visits' }
  ]
  return all.filter((o) => hasMenu(o.menu))
})

const onExportCommand = (key: string) => {
  doExport(key, exportLabels[key] || key)
}

/** 有权限才跳转，无权限静默忽略 */
const goTo = (path: string, menu: string) => {
  if (hasMenu(menu)) {
    router.push(path)
  }
}
</script>

<template>
  <div class="bs-wrapper">
    <!-- ============ 顶部标题栏 ============ -->
    <div class="bs-header">
      <div class="bs-header-side">
        <span>
          <HeaderClock mode="date" />
        </span>
      </div>
      <div class="bs-header-title">
        <span class="bs-wing left" />
        <h1>停车场管理系统</h1>
        <span class="bs-wing right" />
      </div>
      <div class="bs-header-side right">
        <span>欢迎回来，管理员</span>
        <HeaderClock mode="time" class="bs-clock" />
      </div>
    </div>

    <!-- ============ 三栏主体 ============ -->
    <div class="bs-body">
      <!-- -------- 左栏 -------- -->
      <div class="bs-col side">
        <div class="tech-panel panel-fixed">
          <span class="corner" />
          <div class="tech-title">
            停车区域统计
            <span class="tech-sub">已停 / 空闲</span>
          </div>
          <div class="zone-grid">
            <el-empty
              v-if="zoneCards.length === 0"
              description="暂无分区"
              :image-size="60"
              style="grid-column: 1 / -1; margin: 12px 0"
            />
            <div v-for="(z, i) in zoneCards" :key="z.name" class="zone-card">
              <div class="zone-card-head">
                <span class="z-badge">{{ zoneBadge[i] || '•' }}</span>
                <span class="zone-card-name" :title="z.name">{{ z.name }}</span>
              </div>
              <div class="zone-card-nums">
                <span class="occupied">已停<b>{{ z.occupied }}</b></span>
                <span class="vacant">空闲<b>{{ z.vacant }}</b></span>
              </div>
            </div>
          </div>
        </div>

        <div class="tech-panel panel-flex">
          <span class="corner" />
          <div class="tech-title">
            营业统计
            <span class="tech-sub range-pick">
              <el-radio-group v-model="rangeKey" size="small">
                <el-radio-button value="7">7天</el-radio-button>
                <el-radio-button value="30">30天</el-radio-button>
                <el-radio-button value="custom">自定义</el-radio-button>
              </el-radio-group>
            </span>
          </div>
          <div v-if="rangeKey === 'custom'" class="range-custom">
            <el-date-picker
              v-model="customRange"
              type="daterange"
              size="small"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
              :disabled-date="disableFutureDate"
              style="width: 100%"
            />
          </div>
          <div style="flex: 1; min-height: 0; padding: 4px 6px">
            <EChart :option="revenueOption" height="100%" />
          </div>
        </div>

        <div class="tech-panel panel-flex">
          <span class="corner" />
          <div class="tech-title">
            停车统计
            <span class="tech-sub">{{ rangeLabel }} · 每日车次</span>
          </div>
          <div style="flex: 1; min-height: 0; padding: 4px 6px">
            <EChart :option="usageOption" height="100%" />
          </div>
        </div>
      </div>

      <!-- -------- 中栏 -------- -->
      <div class="bs-col">
        <div class="tech-panel bs-map">
          <span class="corner" />
          <div class="tech-title">
            停车场 · 车位分布总览
            <span class="tech-sub" style="display: flex; align-items: center; gap: 12px">
              <el-tag v-if="error" type="danger" size="small">加载失败，稍后自动重试</el-tag>
              <el-button size="small" plain @click="loadAll()">刷新</el-button>
              <el-dropdown trigger="click" @command="onExportCommand">
                <el-button size="small" plain :loading="!!exporting">
                  <el-icon><Download /></el-icon>
                  <span style="margin-left: 4px">报表导出</span>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="opt in exportOptions" :key="opt.key" :command="opt.key">{{ opt.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </span>
          </div>
          <div class="bs-map-scroll">
            <SpotMap :on-changed="loadAll" />
          </div>
          <div class="bs-stats">
            <div
              v-for="c in statCards"
              :key="c.label"
              class="bs-stat"
              :title="hasMenu(c.menu) ? '点击查看详情' : ''"
              :style="{ cursor: hasMenu(c.menu) ? 'pointer' : 'default' }"
              @click="goTo(c.to, c.menu)"
            >
              <span class="bs-stat-icon"><el-icon><component :is="c.icon" /></el-icon></span>
              <span class="bs-stat-value">{{ c.value }}</span>
              <span class="bs-stat-label">{{ c.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- -------- 右栏 -------- -->
      <div class="bs-col side">
        <div class="tech-panel panel-flex" style="flex: 1.25">
          <span class="corner" />
          <div class="tech-title">
            违停时长统计
            <span class="tech-sub">最近 {{ violations.length }} 条</span>
          </div>
          <div class="alarm-list">
            <el-empty
              v-if="violations.length === 0"
              description="暂无违停记录"
              :image-size="60"
              style="margin-top: 24px"
            />
            <div
              v-for="v in violations"
              :key="v.id"
              class="alarm-item"
              :style="{ cursor: hasMenu('violations') ? 'pointer' : 'default' }"
              :title="hasMenu('violations') ? (v.description || '点击查看违规记录') : ''"
              @click="goTo('/violations', 'violations')"
            >
              <el-icon color="#e6a23c"><Van /></el-icon>
              <span class="al-plate">{{ v.plateNumber || '无车牌' }}</span>
              <el-tag :type="v.status === 1 ? 'success' : 'danger'" size="small" style="margin: 0">
                {{ v.status === 1 ? '已处理' : '未处理' }}
              </el-tag>
              <span class="al-time">{{ fmtTime(v.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div class="tech-panel panel-flex">
          <span class="corner" />
          <div class="tech-title">
            告警提醒
            <span class="tech-sub">
              <el-icon :color="alarmTotal > 0 ? '#f56c6c' : '#67c23a'" style="margin-right: 4px">
                <Bell />
              </el-icon>
              待办 {{ alarmTotal }} 项
            </span>
          </div>
          <div class="alarm-list">
            <div
              v-for="a in alarms"
              :key="a.label"
              class="alarm-item"
              :class="{ critical: a.count > 0 && a.critical }"
            >
              <el-icon :color="a.count > 0 ? '#e6a23c' : '#67c23a'"><Warning /></el-icon>
              <span class="al-plate" style="font-size: 12px">{{ a.label }}</span>
              <span :style="{ color: a.count > 0 ? '#f56c6c' : '#67c23a', fontWeight: 700 }">{{ a.count }}</span>
              <el-button link size="small" style="margin-left: auto; padding: 0" @click="goTo(a.to, a.menu)">
                查看 <el-icon style="font-size: 10px"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 图表区间选择器：压缩内边距以适配侧栏宽度 */
.range-pick :deep(.el-radio-button__inner) {
  padding: 4px 7px;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0;
}
.range-custom {
  padding: 8px 8px 0;
}
</style>

