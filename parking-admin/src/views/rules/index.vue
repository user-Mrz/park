<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getConfig } from '@/api'

/** 停车规则分组（键名与后端 SystemConfig 一致），仅展示用 */
interface RuleItem {
  key: string
  label: string
  suffix?: string
  hint?: string
}
interface RuleGroup {
  title: string
  icon: string
  iconColor?: string
  desc?: string
  items: RuleItem[]
}

const GROUPS: RuleGroup[] = [
  {
    title: '基础计费与预约',
    icon: 'Location',
    desc: '全局默认规则，未单独配置的场景按此执行',
    items: [
      { key: 'default_hourly_rate', label: '默认每小时费率', suffix: '元/小时' },
      { key: 'max_reservation_days', label: '最大预约天数', suffix: '天' },
      { key: 'min_reservation_hours', label: '最小预约小时数', suffix: '小时' },
      { key: 'grace_period_minutes', label: '预约宽限期', suffix: '分钟', hint: '预约开始后多久内到场仍可入场' }
    ]
  },
  {
    title: '校园身份费率与免费时长',
    icon: 'User',
    desc: '系数 <1 为折扣、>1 为加价；免费时长在出场结算时先行抵扣；未认证身份按访客处理',
    items: [
      { key: 'rate_student_discount', label: '学生费率系数', hint: '0.5 表示五折' },
      { key: 'rate_visitor_multiplier', label: '访客费率系数', hint: '1.5 表示加价 50%' },
      { key: 'rate_teacher_free_minutes', label: '教师免费时长', suffix: '分钟' },
      { key: 'rate_staff_free_minutes', label: '职工免费时长', suffix: '分钟' },
      { key: 'rate_student_free_minutes', label: '学生免费时长', suffix: '分钟' },
      { key: 'rate_visitor_free_minutes', label: '访客免费时长', suffix: '分钟' },
      { key: 'rate_parent_free_minutes', label: '家长接送免费时长', suffix: '分钟' }
    ]
  },
  {
    title: '每日预约次数上限',
    icon: 'Calendar',
    desc: '按身份限制每人每天可发起的预约数量，0 表示不限制',
    items: [
      { key: 'reserve_daily_limit_student', label: '学生', suffix: '次/日' },
      { key: 'reserve_daily_limit_teacher', label: '教师', suffix: '次/日' },
      { key: 'reserve_daily_limit_staff', label: '职工', suffix: '次/日' },
      { key: 'reserve_daily_limit_visitor', label: '访客/家长', suffix: '次/日' }
    ]
  },
  {
    title: '校园场景规则',
    icon: 'OfficeBuilding',
    desc: '夜间禁停、学期/假期模式与违规判定',
    items: [
      { key: 'rule_night_ban_start', label: '夜间禁停开始', hint: '24 小时制' },
      { key: 'rule_night_ban_end', label: '夜间禁停结束', hint: '24 小时制' },
      { key: 'semester_mode', label: '学期/假期模式' },
      { key: 'holiday_discount', label: '假期模式统一折扣', hint: '0.8 表示八折，仅假期模式生效' },
      {
        key: 'violation_overtime_grace_minutes',
        label: '超时违规宽限期',
        suffix: '分钟',
        hint: '超过预约/预计离场时间多久记为违规'
      }
    ]
  }
]

/** 违规规则（与后端 ViolationServiceImpl 一致） */
const VIOLATION_RULES = [
  {
    type: 'overtime',
    name: '超时占用',
    icon: 'Clock',
    color: '#e6a23c',
    points: '1 ~ 2 分',
    desc: '超过预约结束时间或预计离场时间，且超出宽限期仍未离场'
  },
  {
    type: 'illegal_zone',
    name: '违规区域入场',
    icon: 'Warning',
    color: '#f56c6c',
    points: '2 分',
    desc: '在非允许分区入场，或身份与车位允许身份不符时入场'
  },
  {
    type: 'occupied_others',
    name: '占用他人预约车位',
    icon: 'Van',
    color: '#f56c6c',
    points: '2 分',
    desc: '占用他人已预约且处于有效期的车位'
  }
]

const loading = ref(false)
const configs = ref<Record<string, string>>({})

const load = async () => {
  loading.value = true
  try {
    const res: any = await getConfig()
    configs.value = res.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)

const fmtValue = (item: RuleItem) => {
  const v = configs.value[item.key]
  if (v === undefined || v === '') return '—'
  if (item.key === 'semester_mode') {
    return v === '1' ? '假期模式（关闭教学区/图书馆区预约）' : '学期模式（正常开放）'
  }
  return `${v}${item.suffix ? ' ' + item.suffix : ''}`
}

/** 只展示后端确实返回了配置项的分组 */
const visibleGroups = computed(() =>
  GROUPS.map((g) => ({ ...g, items: g.items.filter((it) => configs.value[it.key] !== undefined) })).filter(
    (g) => g.items.length > 0
  )
)

const grace = computed(() => configs.value.violation_overtime_grace_minutes || '?')
</script>

<template>
  <div class="page-container">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="校园停车规则与违规规则"
      description="以下规则由管理员在「系统配置」中维护，保存后立即生效。学生/教师/职工/访客/家长按身份差异化计费与限制；违规记录将累计扣分，影响后续停车权限。"
      style="margin-bottom: 16px"
    />

    <div v-loading="loading">
      <!-- 停车规则 -->
      <el-card
        v-for="group in visibleGroups"
        :key="group.title"
        shadow="never"
        class="rule-card"
        style="margin-bottom: 16px"
      >
        <template #header>
          <span class="rule-card-title">
            <el-icon color="#409eff"><component :is="group.icon" /></el-icon>
            {{ group.title }}
          </span>
        </template>
        <div v-if="group.desc" class="rule-desc">{{ group.desc }}</div>
        <div class="rule-grid">
          <div v-for="it in group.items" :key="it.key" class="rule-item">
            <div class="rule-label">
              {{ it.label }}
              <span v-if="it.hint" class="rule-hint">· {{ it.hint }}</span>
            </div>
            <div class="rule-value">{{ fmtValue(it) }}</div>
          </div>
        </div>
      </el-card>

      <el-divider style="border-color: #ebeef5" />

      <!-- 违规规则 -->
      <el-card shadow="never" class="rule-card">
        <template #header>
          <span class="rule-card-title">
            <el-icon color="#f56c6c"><Warning /></el-icon>
            违规规则与扣分标准
          </span>
        </template>
        <div class="rule-desc">
          超时宽限期当前为 <span style="color: #e6a23c; font-weight: 700">{{ grace }}</span> 分钟；超 120 分钟记 2
          分，否则 1 分；违规记录需管理员处理后方可解除。
        </div>
        <el-empty v-if="VIOLATION_RULES.length === 0" description="暂无违规规则" />
        <div v-else class="violation-list">
          <div
            v-for="r in VIOLATION_RULES"
            :key="r.type"
            class="violation-item"
            :style="{ borderLeft: `4px solid ${r.color}` }"
          >
            <span class="violation-icon" :style="{ color: r.color }">
              <el-icon><component :is="r.icon" /></el-icon>
            </span>
            <div style="flex: 1; min-width: 0">
              <div class="violation-name">{{ r.name }}</div>
              <div class="violation-desc">{{ r.desc }}</div>
            </div>
            <el-tag type="danger" size="large">扣 {{ r.points }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.rule-card :deep(.el-card__header) {
  padding: 12px 16px;
}
.rule-card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #1d2129;
}
.rule-desc {
  font-size: 12px;
  color: #606266;
  margin-bottom: 14px;
}
.rule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.rule-item {
  background: #f5f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 14px;
}
.rule-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}
.rule-hint {
  color: #909399;
  margin-left: 4px;
  font-size: 12px;
}
.rule-value {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
}
.violation-list {
  display: grid;
  gap: 12px;
}
.violation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #f5f9ff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}
.violation-icon {
  font-size: 24px;
  display: inline-flex;
}
.violation-name {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.violation-desc {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}
</style>
