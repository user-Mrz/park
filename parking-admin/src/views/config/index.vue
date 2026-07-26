<template>
  <div class="page-container">
    <el-card>
      <template #header><span>系统配置</span></template>
      <el-form label-width="200px" v-loading="loading">
        <el-form-item v-for="(value, key) in configs" :key="key" :label="getLabel(key)">
          <el-input v-model="configs[key]" :placeholder="getLabel(key)" style="width:300px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getConfig, updateConfig } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false), saving = ref(false)
const configs = reactive<Record<string, string>>({})

const labels: Record<string, string> = {
  default_hourly_rate: '默认每小时费率(元)',
  max_reservation_days: '最大预约天数',
  min_reservation_hours: '最小预约小时数',
  grace_period_minutes: '预约宽限期(分钟)'
}

function getLabel(key: string) { return labels[key] || key }

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await getConfig()
    Object.keys(configs).forEach(k => delete configs[k])
    Object.assign(configs, res.data)
  } finally { loading.value = false }
}

async function saveConfig() {
  saving.value = true
  try {
    await updateConfig(configs)
    ElMessage.success('配置已保存')
  } finally { saving.value = false }
}

onMounted(() => { loadConfig() })
</script>

<style scoped>
.page-container :deep(.el-card__header) { font-weight: bold; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
</style>
