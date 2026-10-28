<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getServerMonitor } from '@/api'
import { useAutoRefresh } from '@/hooks/useAutoRefresh'

interface ServerInfo {
  cpu: { cpuNum: number; total: number; sys: number; used: number; wait: number; free: number }
  mem: { total: number; used: number; free: number }
  jvm: { total: number; max: number; free: number; version: string; home: string; startTime: number; runTime: number }
  sys: { computerName: string; computerIp: string; userDir: string; osName: string; osArch: string }
  sysFiles: Array<{ dirName: string; sysTypeName: string; typeName: string; total: string; free: string; used: string; usage: number }>
  jvmUsed: number
}

const info = ref<ServerInfo | null>(null)

/** 字节数转可读大小 */
const fmtBytes = (size?: number) => {
  if (!size && size !== 0) return '0 B'
  const kb = 1024
  const mb = kb * 1024
  const gb = mb * 1024
  if (size >= gb) return `${(size / gb).toFixed(1)} GB`
  if (size >= mb) return `${(size / mb).toFixed(1)} MB`
  if (size >= kb) return `${(size / kb).toFixed(1)} KB`
  return `${size} B`
}

/** 毫秒时长转可读 */
const fmtDuration = (ms: number) => {
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  return d > 0 ? `${d}天${h}小时${m}分钟` : h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

/** 占比（百分数，1 位小数） */
const pct = (part: number, total: number) => (total > 0 ? Math.round((part / total) * 1000) / 10 : 0)

/** 进度条配色：使用率越高越红 */
const usageColor = (v: number) => (v >= 80 ? '#f56c6c' : v >= 50 ? '#e6a23c' : '#409eff')

const loadData = async (silent = false) => {
  try {
    const res: any = await getServerMonitor()
    info.value = res?.data || null
  } catch (e: any) {
    if (!silent) ElMessage.error(e?.message || '硬件数据获取失败')
  }
}


useAutoRefresh(loadData, { intervalMs: 30000 })


onMounted(() => loadData())

const cpuUsed = () => {
  const c = info.value?.cpu
  if (!c) return 0
  return pct((c.used || 0) + (c.sys || 0), c.total)
}
const memUsed = () => {
  const m = info.value?.mem
  if (!m) return 0
  return pct(m.used, m.total)
}
const jvmUsed = () => info.value?.jvmUsed ?? 0
const jvmUsedPct = () => {
  const j = info.value?.jvm
  if (!j) return 0
  return pct(jvmUsed(), j.max)
}
</script>

<template>
  <div class="page-container">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      class="page-alert"
      title="服务器硬件监控"
      description="参考若依服务器监控设计，实时采集部署后端的服务器 CPU、内存、JVM 与磁盘硬件数据。CPU 占用经 1 秒采样计算。"
    />

    <el-row :gutter="16">
      <!-- CPU -->
      <el-col :xs="24" :lg="8">
        <el-card class="tech-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><Monitor /></el-icon>CPU</span>
              <el-tag size="small" type="primary" effect="plain">{{ info?.cpu?.cpuNum ?? '-' }} 核</el-tag>
            </div>
          </template>
          <div class="usage-row">
            <span class="usage-label">CPU 使用率</span>
            <el-progress
              class="usage-bar"
              :percentage="cpuUsed()"
              :color="usageColor(cpuUsed())"
              :stroke-width="10"
            />
          </div>
          <div class="usage-row">
            <span class="usage-label">系统使用率</span>
            <el-progress
              class="usage-bar"
              :percentage="pct(info?.cpu?.sys || 0, info?.cpu?.total || 0)"
              :color="usageColor(pct(info?.cpu?.sys || 0, info?.cpu?.total || 0))"
              :stroke-width="10"
            />
          </div>
          <div class="usage-row">
            <span class="usage-label">用户使用率</span>
            <el-progress
              class="usage-bar"
              :percentage="pct(info?.cpu?.used || 0, info?.cpu?.total || 0)"
              :color="usageColor(pct(info?.cpu?.used || 0, info?.cpu?.total || 0))"
              :stroke-width="10"
            />
          </div>
          <div class="usage-row">
            <span class="usage-label">当前等待率</span>
            <el-progress
              class="usage-bar"
              :percentage="pct(info?.cpu?.wait || 0, info?.cpu?.total || 0)"
              :color="usageColor(pct(info?.cpu?.wait || 0, info?.cpu?.total || 0))"
              :stroke-width="10"
            />
          </div>
          <div class="usage-row">
            <span class="usage-label">当前空闲率</span>
            <el-progress
              class="usage-bar"
              :percentage="pct(info?.cpu?.free || 0, info?.cpu?.total || 0)"
              :color="usageColor(pct(info?.cpu?.free || 0, info?.cpu?.total || 0))"
              :stroke-width="10"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 内存 -->
      <el-col :xs="24" :lg="8">
        <el-card class="tech-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><Lightning /></el-icon>内存</span>
            </div>
          </template>
          <div class="usage-row">
            <span class="usage-label">内存使用率</span>
            <el-progress
              class="usage-bar"
              :percentage="memUsed()"
              :color="usageColor(memUsed())"
              :stroke-width="10"
            />
          </div>
          <div class="info-lines">
            <div>总内存：{{ fmtBytes(info?.mem?.total) }}</div>
            <div>已用内存：{{ fmtBytes(info?.mem?.used) }}</div>
            <div>剩余内存：{{ fmtBytes(info?.mem?.free) }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- JVM -->
      <el-col :xs="24" :lg="8">
        <el-card class="tech-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title"><el-icon><Platform /></el-icon>JVM 信息</span>
              <el-tag size="small" type="primary" effect="plain">JDK {{ info?.jvm?.version || '-' }}</el-tag>
            </div>
          </template>
          <div class="usage-row">
            <span class="usage-label">JVM 使用率</span>
            <el-progress
              class="usage-bar"
              :percentage="jvmUsedPct()"
              :color="usageColor(jvmUsedPct())"
              :stroke-width="10"
            />
          </div>
          <div class="info-lines">
            <div>最大内存：{{ fmtBytes(info?.jvm?.max) }}</div>
            <div>已用内存：{{ fmtBytes(jvmUsed()) }}</div>
            <div>空闲内存：{{ fmtBytes(info?.jvm?.free) }}</div>
            <div>启动时间：{{ info?.jvm?.startTime ? new Date(info.jvm.startTime).toLocaleString('zh-CN') : '-' }}</div>
            <div>运行时长：{{ info?.jvm?.runTime ? fmtDuration(info.jvm.runTime) : '-' }}</div>
            <div class="break-all">Java 路径：{{ info?.jvm?.home || '-' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 服务器状态与磁盘 -->
    <el-card class="tech-card disk-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title"><el-icon><Cpu /></el-icon>服务器状态与磁盘信息</span>
        </div>
      </template>
      <el-descriptions :column="3" size="small" border class="sys-desc">
        <el-descriptions-item label="服务器名称">{{ info?.sys?.computerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="服务器 IP">{{ info?.sys?.computerIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ info?.sys?.osName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统架构">{{ info?.sys?.osArch || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目路径" :span="2">
          <span class="break-all">{{ info?.sys?.userDir || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="info?.sysFiles || []" size="small" class="disk-table">
        <el-table-column prop="dirName" label="盘符路径" min-width="120" />
        <el-table-column prop="sysTypeName" label="文件系统" min-width="110" />
        <el-table-column prop="typeName" label="盘符类型" min-width="140" show-overflow-tooltip />
        <el-table-column prop="total" label="总大小" width="100" align="right" />
        <el-table-column prop="free" label="可用大小" width="100" align="right" />
        <el-table-column prop="used" label="已用大小" width="100" align="right" />
        <el-table-column label="使用率" width="220">
          <template #default="{ row }">
            <div class="disk-usage">
              <el-progress
                class="usage-bar"
                :percentage="row.usage"
                :color="usageColor(row.usage)"
                :stroke-width="10"
              />
              <span class="disk-pct" :style="{ color: usageColor(row.usage) }">{{ row.usage }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="foot-bar">
      <el-button size="small" @click="loadData()">手动刷新</el-button>
    </div>
  </div>
</template>

<style scoped>
.page-alert {
  margin-bottom: 16px;
}
.tech-card {
  background: #ffffff;
  border-color: #ebeef5;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1d2129;
  font-weight: 600;
}
.card-title .el-icon {
  color: #409eff;
}
.usage-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.usage-label {
  width: 88px;
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
}
.usage-bar {
  flex: 1;
  margin: 0;
}
.info-lines {
  font-size: 13px;
  color: #606266;
  line-height: 2;
}
.disk-card {
  margin-top: 16px;
}
.sys-desc {
  margin-bottom: 16px;
}
.disk-table {
  width: 100%;
}
.disk-usage {
  display: flex;
  align-items: center;
  gap: 8px;
}
.disk-pct {
  font-size: 12px;
  width: 48px;
  text-align: right;
}
.break-all {
  word-break: break-all;
}
.foot-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
</style>
