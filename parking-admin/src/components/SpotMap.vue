<template>
  <div class="spot-map">
    <div class="map-header">
      <span>车位状态地图</span>
      <div class="legend">
        <div class="legend-item"><span class="legend-dot vacant"></span><span>空闲</span></div>
        <div class="legend-item"><span class="legend-dot occupied"></span><span>占用</span></div>
        <div class="legend-item"><span class="legend-dot reserved"></span><span>预约未停车</span></div>
        <div class="legend-item"><span class="legend-dot reserved-parked"></span><span>预约已停车</span></div>
        <div class="legend-item"><span class="legend-dot maintenance"></span><span>维修</span></div>
      </div>
    </div>
    <div class="map-container">
      <div v-for="(group, area) in groupedSpots" :key="area" class="area-group">
        <div class="area-header">{{ area }}</div>
        <div class="spots-grid">
          <div 
            v-for="spot in group" 
            :key="spot.id" 
            class="spot-item"
            :class="getSpotClass(spot.displayStatus)"
            @click="showSpotDetail(spot)"
          >
            <div class="spot-code">{{ spot.spotCode }}</div>
            <div class="spot-floor">{{ spot.floor }}</div>
          </div>
        </div>
      </div>
    </div>
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-value">{{ stats.totalCount }}</span>
        <span class="stat-label">总车位</span>
      </div>
      <div class="stat-item">
        <span class="stat-value vacant">{{ stats.vacantCount }}</span>
        <span class="stat-label">空闲</span>
      </div>
      <div class="stat-item">
        <span class="stat-value occupied">{{ stats.occupiedCount }}</span>
        <span class="stat-label">占用</span>
      </div>
      <div class="stat-item">
        <span class="stat-value reserved">{{ stats.reservedUnparkedCount }}</span>
        <span class="stat-label">预约未停车</span>
      </div>
      <div class="stat-item">
        <span class="stat-value reserved-parked">{{ stats.reservedParkedCount }}</span>
        <span class="stat-label">预约已停车</span>
      </div>
      <div class="stat-item">
        <span class="stat-value maintenance">{{ stats.maintenanceCount }}</span>
        <span class="stat-label">维修</span>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="车位详情" width="400px">
      <el-descriptions :column="2" border v-if="selectedSpot">
        <el-descriptions-item label="车位编号">{{ selectedSpot.spotCode }}</el-descriptions-item>
        <el-descriptions-item label="区域">{{ selectedSpot.area }}</el-descriptions-item>
        <el-descriptions-item label="楼层">{{ selectedSpot.floor }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ selectedSpot.pricePerHour }}元/小时</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusTagType(selectedSpot.displayStatus)">
            {{ getStatusText(selectedSpot.displayStatus) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getSpotMap } from '@/api'

const spots = ref<any[]>([])
const stats = reactive({
  totalCount: 0,
  vacantCount: 0,
  occupiedCount: 0,
  reservedUnparkedCount: 0,
  reservedParkedCount: 0,
  maintenanceCount: 0
})
const dialogVisible = ref(false)
const selectedSpot = ref<any>(null)

const groupedSpots = computed(() => {
  const groups: Record<string, any[]> = {}
  spots.value.forEach(spot => {
    const area = spot.area || '未分组'
    if (!groups[area]) groups[area] = []
    groups[area].push(spot)
  })
  Object.keys(groups).forEach(key => {
    groups[key].sort((a, b) => a.spotCode.localeCompare(b.spotCode))
  })
  return groups
})

function getSpotClass(status: number) {
  const classes: Record<number, string> = {
    0: 'vacant',
    1: 'occupied',
    2: 'reserved',
    4: 'maintenance',
    5: 'reserved-parked'
  }
  return classes[status] || 'vacant'
}

function getStatusText(status: number) {
  const texts: Record<number, string> = {
    0: '空闲',
    1: '占用',
    2: '预约未停车',
    4: '维修',
    5: '预约已停车'
  }
  return texts[status] || '未知'
}

function getStatusTagType(status: number) {
  const types: Record<number, string> = {
    0: 'success',
    1: 'warning',
    2: 'info',
    4: 'danger',
    5: 'primary'
  }
  return types[status] || 'default'
}

function showSpotDetail(spot: any) {
  selectedSpot.value = spot
  dialogVisible.value = true
}

async function loadData() {
  try {
    const res: any = await getSpotMap()
    spots.value = res.data.spots || []
    Object.assign(stats, res.data)
  } catch (e) {
    console.error('Failed to load spot map:', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.spot-map {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: bold;
}
.legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}
.legend-dot.vacant { background-color: #67C23A; }
.legend-dot.occupied { background-color: #E6A23C; }
.legend-dot.reserved { background-color: #409EFF; }
.legend-dot.reserved-parked { background-color: #909399; }
.legend-dot.maintenance { background-color: #F56C6C; }
.map-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}
.area-group {
  margin-bottom: 20px;
}
.area-header {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 4px solid #409EFF;
}
.spots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
}
.spot-item {
  aspect-ratio: 4/3;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.spot-item:hover {
  transform: scale(1.05);
  border-color: #999;
}
.spot-item.vacant { background-color: #E8F5E9; }
.spot-item.occupied { background-color: #FEF3E2; }
.spot-item.reserved { background-color: #E3F2FD; }
.spot-item.reserved-parked { background-color: #F5F5F5; }
.spot-item.maintenance { background-color: #FDEDED; }
.spot-code {
  font-size: 12px;
  font-weight: bold;
  color: #303133;
}
.spot-floor {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}
.stats-bar {
  display: flex;
  justify-content: space-around;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
}
.stat-item {
  text-align: center;
}
.stat-value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.stat-value.vacant { color: #67C23A; }
.stat-value.occupied { color: #E6A23C; }
.stat-value.reserved { color: #409EFF; }
.stat-value.reserved-parked { color: #909399; }
.stat-value.maintenance { color: #F56C6C; }
.stat-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>