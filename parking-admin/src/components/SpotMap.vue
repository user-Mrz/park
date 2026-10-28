<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getSpotMap, forceExit } from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'
import { useSpotSocket, type SpotSocketMessage } from '@/hooks/useSpotSocket'
import LiveDuration from '@/components/LiveDuration.vue'

const props = defineProps<{
  /** 数据变化后通知父组件刷新（例如首页统计） */
  onChanged?: () => void
}>()

const STATUS_META: Record<number, { text: string; tag: string; color: string; bg: string }> = {
  0: { text: '空闲', tag: 'success', color: '#67c23a', bg: 'rgba(103, 194, 58, 0.12)' },
  1: { text: '占用', tag: 'warning', color: '#e6a23c', bg: 'rgba(230, 162, 60, 0.14)' },
  2: { text: '预约未停车', tag: 'primary', color: '#409eff', bg: 'rgba(64, 158, 255, 0.12)' },
  4: { text: '维修', tag: 'danger', color: '#f56c6c', bg: 'rgba(245, 108, 108, 0.12)' },
  5: { text: '预约已停车', tag: 'info', color: '#909399', bg: 'rgba(144, 147, 153, 0.12)' }
}

const LEGEND_ORDER = [0, 1, 2, 5, 4]

const ZONE_NAME: Record<string, string> = {
  teaching: '教学楼区',
  library: '图书馆区',
  dorm: '宿舍区',
  gym: '体育馆区',
  visitor: '访客与家长临时停车区'
}

const IDENTITY_NAME: Record<string, string> = {
  '1': '学生',
  '2': '教师',
  '3': '职工',
  '4': '访客',
  '5': '家长'
}

const spots = ref<any[]>([])
const loading = ref(false)
const detailOpen = ref(false)
const selectedSpot = ref<any>(null)
const acting = ref(false)

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

/** 可停身份文案，如「学生/教师/职工」 */
const allowLabel = (allow: any) => {
  const codes = String(allow || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (codes.length === 0) return '所有身份'
  return codes.map((c) => IDENTITY_NAME[c] || c).join('/')
}

const loadData = async (silent = false) => {
  // WebSocket 正常且最近 30 秒有消息时跳过轮询；断线后自动恢复
  if (silent && wsConnected.value && Date.now() - wsLastMessageAt.value < 30000) return
  if (!silent) loading.value = true
  try {
    const res: any = await getSpotMap()
    const list: any[] = res.data?.spots || []
    spots.value = list
    // 抽屉打开时同步刷新其数据，避免详情里显示的是过期快照
    if (selectedSpot.value) {
      const fresh = list.find((s) => String(s.id) === String(selectedSpot.value.id))
      if (fresh) selectedSpot.value = fresh
    }
  } catch (e) {
    /* 错误提示由 request 拦截器处理 */
  } finally {
    if (!silent) loading.value = false
  }
}

loadData()

const {
  connected: wsConnected,
  lastMessageAt: wsLastMessageAt,
  subscribe: subscribeSpotSocket,
  unsubscribe: unsubscribeSpotSocket
} = useSpotSocket()

/** 服务端只推原始状态，这里按 getSpotMap 的口径映射成地图展示状态 */
const applyRealtimeStatus = (spot: any, status: number) => {
  spot.status = status
  if (status === 3) {
    spot.displayStatus = 4
    spot.vehicle = null
    spot.reservation = null
  } else if (status === 2) {
    spot.displayStatus = 2
  } else if (status === 1) {
    spot.displayStatus = spot.reservation ? 5 : 1
  } else if (status === 0) {
    spot.displayStatus = 0
    spot.vehicle = null
    spot.reservation = null
  }
}

const applyRealtimeMessage = (message: SpotSocketMessage) => {
  const items =
    message.type === 'snapshot'
      ? (message.spots || []).map((s) => ({ id: s.id, status: s.status }))
      : message.changes || []
  if (items.length === 0) return
  const spotById = new Map(spots.value.map((s: any) => [String(s.id), s]))
  let unknownSpot = false
  items.forEach((item) => {
    const spot = spotById.get(String(item.id))
    if (!spot) {
      unknownSpot = true
      return
    }
    applyRealtimeStatus(spot, item.status)
  })
  // 新增/删除车位在本地没有详情，静默补拉一次完整数据
  if (unknownSpot) loadData(true)
  // 详情抽屉打开时同步最新状态
  if (selectedSpot.value) {
    const fresh = spotById.get(String(selectedSpot.value.id))
    if (fresh) selectedSpot.value = fresh
  }
}

onMounted(() => subscribeSpotSocket(applyRealtimeMessage))
onBeforeUnmount(() => unsubscribeSpotSocket(applyRealtimeMessage))

/** 车位状态自动刷新：30 秒一次，标签页后台自动暂停（前端不再展示开关组件） */
useAutoRefresh(loadData, { intervalMs: 30000 })

const groupedSpots = computed(() => {
  const groups: Record<string, any[]> = {}
  spots.value.forEach((spot) => {
    const area = spot.area || '未分组'
    if (!groups[area]) groups[area] = []
    groups[area].push(spot)
  })
  return Object.keys(groups)
    .sort((a, b) => a.localeCompare(b, 'zh-CN'))
    .map((area) => {
      const list = groups[area].sort((a, b) => String(a.spotCode).localeCompare(String(b.spotCode)))
      const countBy: Record<number, number> = {}
      list.forEach((s) => {
        const st = Number(s.displayStatus)
        countBy[st] = (countBy[st] || 0) + 1
      })
      return { area, list, countBy, total: list.length }
    })
})

const metaOf = (status: any) => STATUS_META[Number(status)] || STATUS_META[0]

const openDetail = (spot: any) => {
  selectedSpot.value = spot
  detailOpen.value = true
}

const handleForceExit = async () => {
  const recordId = selectedSpot.value?.vehicle?.recordId
  if (!recordId) return
  acting.value = true
  try {
    await forceExit(recordId)
    ElMessage.success('已强制出场并结算')
    await loadData()
    detailOpen.value = false
    props.onChanged?.()
  } finally {
    acting.value = false
  }
}

const vehicle = computed(() => selectedSpot.value?.vehicle)
const reservation = computed(() => selectedSpot.value?.reservation)
const selectedMeta = computed(() =>
  selectedSpot.value ? metaOf(selectedSpot.value.displayStatus) : STATUS_META[0]
)

watch(detailOpen, (open) => {
  if (!open) selectedSpot.value = selectedSpot.value
})
</script>

<template>
  <div>
    <!-- 图例 + 刷新 -->
    <div class="spotmap-bar">
      <div class="spotmap-legend">
        <span v-for="st in LEGEND_ORDER" :key="st" class="legend-item">
          <i class="legend-dot" :style="{ backgroundColor: STATUS_META[st].color }" />
          {{ STATUS_META[st].text }}
        </span>
      </div>
      <div class="spotmap-actions">
        <el-tag v-if="wsConnected" type="success" size="small" effect="plain">实时</el-tag>
        <el-tag v-else type="info" size="small" effect="plain">轮询兜底</el-tag>
        <el-button size="small" :loading="loading" @click="loadData()">
          <el-icon><Refresh /></el-icon>
          <span style="margin-left: 4px">刷新</span>
        </el-button>
      </div>
    </div>

    <el-empty v-if="groupedSpots.length === 0" description="暂无车位数据" style="padding: 40px 0" />

    <div v-else class="map-areas">
      <div v-for="g in groupedSpots" :key="g.area" class="area-group">
        <div class="area-group-header">
          <span class="area-group-title">{{ g.area }}</span>
          <div class="area-group-chips">
            <span class="area-chip"><i class="chip-dot" style="background: #67c23a" />空闲 {{ g.countBy[0] || 0 }}</span>
            <span class="area-chip"><i class="chip-dot" style="background: #e6a23c" />占用 {{ g.countBy[1] || 0 }}</span>
            <span class="area-chip"><i class="chip-dot" style="background: #409eff" />预约 {{ g.countBy[2] || 0 }}</span>
            <span class="area-chip"><i class="chip-dot" style="background: #f56c6c" />维修 {{ g.countBy[4] || 0 }}</span>
            <span class="area-chip">{{ g.total }} 个车位</span>
          </div>
        </div>
        <div class="spots-grid">
          <div
            v-for="spot in g.list"
            :key="spot.id"
            class="spot-item"
            :class="`spot-${Number(spot.displayStatus)}`"
            :style="{
              border: `1px solid ${metaOf(spot.displayStatus).color}33`,
              backgroundColor: metaOf(spot.displayStatus).bg
            }"
            :title="`${spot.spotCode} · ${metaOf(spot.displayStatus).text}${spot.vehicle?.plateNumber ? ' · ' + spot.vehicle.plateNumber : ''}`"
            @click="openDetail(spot)"
          >
            <div class="spot-code">{{ spot.spotCode }}</div>
            <div v-if="spot.vehicle?.plateNumber" class="spot-plate">{{ spot.vehicle.plateNumber }}</div>
            <div v-else class="spot-floor">{{ spot.floor }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 车位详情抽屉 -->
    <el-drawer
      v-model="detailOpen"
      :title="selectedSpot ? `车位详情 · ${selectedSpot.spotCode}` : '车位详情'"
      size="470px"
      destroy-on-close
    >
      <template v-if="selectedSpot">
        <div class="spot-detail-head">
          <div>
            <div class="sd-code">{{ selectedSpot.spotCode }}</div>
            <div class="sd-sub">
              {{ selectedSpot.area }} · {{ selectedSpot.floor }} · {{ fmtMoney(selectedSpot.pricePerHour) }}/小时
            </div>
            <div class="sd-tags">
              <el-tag type="primary" size="small">{{ ZONE_NAME[selectedSpot.zoneCode] || '未设置分区' }}</el-tag>
              <el-tag type="success" size="small">可停：{{ allowLabel(selectedSpot.allowIdentities) }}</el-tag>
              <el-tag v-if="selectedSpot.freeMinutes > 0" type="warning" size="small">
                区域免费 {{ selectedSpot.freeMinutes }} 分钟
              </el-tag>
            </div>
          </div>
          <el-tag :type="selectedMeta.tag as any" size="large">{{ selectedMeta.text }}</el-tag>
        </div>

        <!-- 停放车辆 -->
        <div class="sd-section-title">
          <el-icon color="#e6a23c"><Van /></el-icon>
          <span>停放车辆</span>
        </div>
        <template v-if="vehicle">
          <div class="sd-plate">{{ vehicle.plateNumber || '未记录车牌' }}</div>
          <el-descriptions :column="1" border size="small" style="margin-bottom: 14px">
            <el-descriptions-item label="入场时间">{{ fmtTime(vehicle.entryTime) }}</el-descriptions-item>
            <el-descriptions-item label="已停时长">
              <LiveDuration :entry-time="vehicle.entryTime" :fallback-minutes="vehicle.parkedMinutes" />
            </el-descriptions-item>
            <el-descriptions-item label="预估费用">
              <span style="color: #f56c6c; font-weight: 600">{{ fmtMoney(vehicle.estimatedFee) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="车主">
              {{ vehicle.nickname || '-' }}{{ vehicle.phone ? `（${vehicle.phone}）` : '' }}
            </el-descriptions-item>
            <el-descriptions-item label="停车记录号">
              <span style="font-size: 12px">{{ vehicle.recordId }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-popconfirm
            title="将立即结算费用、释放车位，确定继续？"
            confirm-button-type="danger"
            width="260"
            @confirm="handleForceExit"
          >
            <template #reference>
              <el-button type="danger" :loading="acting" style="width: 100%">强制出场并结算</el-button>
            </template>
          </el-popconfirm>
        </template>
        <el-empty v-else description="该车位当前无车辆停放" :image-size="70" />

        <!-- 预约信息 -->
        <div class="sd-section-title" style="margin-top: 18px">
          <el-icon color="#409eff"><Calendar /></el-icon>
          <span>预约信息</span>
        </div>
        <el-descriptions v-if="reservation" :column="1" border size="small">
          <el-descriptions-item label="预约编号">
            <span style="font-size: 12px">{{ reservation.reservationNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预约时段">
            {{ fmtTime(reservation.startTime) }} ~ {{ fmtTime(reservation.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="预约时长">{{ Number(reservation.durationHours || 0) }} 小时</el-descriptions-item>
          <el-descriptions-item label="预约金额">{{ fmtMoney(reservation.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag type="primary" size="small">进行中</el-tag></el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="该车位没有进行中的预约" :image-size="70" />

        <div v-if="loading" style="text-align: center; margin-top: 16px">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.spotmap-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.spotmap-legend {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #606266;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}
.spotmap-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.spots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
  gap: 8px;
}
.spot-item {
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  padding: 2px 4px;
  overflow: hidden;
}
.spot-item:hover {
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.28);
  transform: translateY(-1px);
}
.spot-code {
  font-size: 12px;
  font-weight: 700;
  color: #1d2129;
}
.spot-plate {
  font-size: 10px;
  color: #e6a23c;
  margin-top: 2px;
  font-weight: 600;
}
.spot-floor {
  font-size: 10px;
  color: #606266;
  margin-top: 2px;
}
.spot-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f5f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  margin-bottom: 18px;
}
.sd-code {
  font-size: 20px;
  font-weight: 700;
  color: #1d2129;
}
.sd-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
.sd-tags {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.sd-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 10px;
}
.sd-plate {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #e6a23c;
  text-align: center;
  padding: 12px 0;
  background: rgba(230, 162, 60, 0.10);
  border: 1px solid rgba(230, 162, 60, 0.35);
  border-radius: 8px;
  margin-bottom: 12px;
}
</style>
