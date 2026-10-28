<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfig, updateConfig } from '@/api'

interface FieldMeta {
  key: string
  label: string
  hint?: string
  type?: 'select'
  options?: { value: string; label: string }[]
}

interface GroupMeta {
  title: string
  desc?: string
  fields: FieldMeta[]
}

/** 校园配置分组（键名保持与后端一致，界面全部中文） */
const GROUPS: GroupMeta[] = [
  {
    title: '基础计费与预约',
    desc: '全局默认规则，未单独配置的场景按此执行',
    fields: [
      { key: 'default_hourly_rate', label: '默认每小时费率（元）', hint: '新增车位未填写单价时使用' },
      { key: 'max_reservation_days', label: '最大预约天数', hint: '单次预约最长天数' },
      { key: 'min_reservation_hours', label: '最小预约小时数', hint: '单次预约最短小时数' },
      { key: 'grace_period_minutes', label: '预约宽限期（分钟）', hint: '预约开始后多久内到场仍可入场' },
      { key: 'tidal_reserve_advance_days', label: '潮汐车位提前预约天数', hint: '潮汐车位开放前 N 天开始在用户端显示并允许预约' }
    ]
  },
  {
    title: '校园身份费率',
    desc: '按校园身份差异化计费：系数 <1 为折扣，>1 为加价；免费时长在出场结算时先行抵扣',
    fields: [
      { key: 'rate_student_discount', label: '学生费率系数', hint: '0.5 表示五折' },
      { key: 'rate_visitor_multiplier', label: '访客费率系数', hint: '1.5 表示加价 50%' },
      { key: 'rate_teacher_free_minutes', label: '教师免费停车时长（分钟）' },
      { key: 'rate_staff_free_minutes', label: '职工免费停车时长（分钟）' },
      { key: 'rate_student_free_minutes', label: '学生免费停车时长（分钟）' },
      { key: 'rate_visitor_free_minutes', label: '访客免费停车时长（分钟）' },
      { key: 'rate_parent_free_minutes', label: '家长接送免费停车时长（分钟）', hint: '家长临时停车默认 30 分钟免费' }
    ]
  },
  {
    title: '每日预约次数限制',
    desc: '按身份限制每人每天可发起的预约数量，填 0 表示不限制',
    fields: [
      { key: 'reserve_daily_limit_student', label: '学生每日预约上限（次）' },
      { key: 'reserve_daily_limit_teacher', label: '教师每日预约上限（次）' },
      { key: 'reserve_daily_limit_staff', label: '职工每日预约上限（次）' },
      { key: 'reserve_daily_limit_visitor', label: '访客/家长每日预约上限（次）' }
    ]
  },
  {
    title: '校园场景规则',
    desc: '夜间禁停、学期/假期模式与违规判定规则',
    fields: [
      { key: 'rule_night_ban_start', label: '夜间禁停开始时间', hint: '24 小时制，如 23:00' },
      { key: 'rule_night_ban_end', label: '夜间禁停结束时间', hint: '24 小时制，如 06:00' },
      {
        key: 'semester_mode',
        label: '学期 / 假期模式',
        hint: '假期模式下教学楼区、图书馆区不开放预约，并叠加假期折扣',
        type: 'select',
        options: [
          { value: '0', label: '学期模式（正常开放）' },
          { value: '1', label: '假期模式（关闭教学区/图书馆区预约）' }
        ]
      },
      { key: 'holiday_discount', label: '假期模式统一折扣', hint: '0.8 表示八折，仅假期模式生效' },
      { key: 'violation_overtime_grace_minutes', label: '超时违规宽限期（分钟）', hint: '超过预约/预计离校时间多久记为违规' }
    ]
  },
  {
    title: '黑白名单与自动拉黑',
    desc: '白名单豁免门禁与预约限制；违规累计达到阈值时自动加入黑名单（白名单车辆不参与自动拉黑）',
    fields: [
      {
        key: 'blacklist_auto_enabled',
        label: '启用违规自动拉黑',
        hint: '关闭后仅保留管理员手动维护的黑名单',
        type: 'select',
        options: [
          { value: '1', label: '启用' },
          { value: '0', label: '关闭' }
        ]
      },
      { key: 'blacklist_auto_violation_threshold', label: '触发阈值（次）', hint: '统计窗口内达到该违规次数后自动拉黑' },
      { key: 'blacklist_auto_window_days', label: '统计窗口（天）', hint: '默认为最近 30 天' },
      { key: 'blacklist_auto_duration_days', label: '拉黑持续天数', hint: '0 表示永久拉黑' }
    ]
  },
  {
    title: '月卡 / 长租计费',
    desc: '小程序端开通月卡的价格口径（单位：元）。有效期内指定车牌出场免按次计费，预约费不豁免',
    fields: [
      { key: 'price_monthly_pass', label: '月卡价格（元 / 30 天）', hint: '有效期 30 天，到期前续费自动叠加到期日' },
      { key: 'price_quarterly_pass', label: '季卡价格（元 / 90 天）', hint: '有效期 90 天' },
      { key: 'price_yearly_pass', label: '年卡价格（元 / 365 天）', hint: '有效期 365 天' }
    ]
  },
  {
    title: '微信订阅消息',
    desc: '模板 ID 需在微信公众平台申请后回填；留空则该类通知自动跳过，不影响主业务流程',
    fields: [
      { key: 'wx_template_reserve_remind', label: '预约前 15 分钟提醒模板 ID', hint: '用户授权订阅后，预约生效前 15 分钟推送' },
      { key: 'wx_template_balance_low', label: '余额不足提醒模板 ID', hint: '出场扣费后余额低于下方阈值时推送' },
      { key: 'wx_template_identity_result', label: '身份审核结果模板 ID', hint: '管理员审核通过或驳回身份后推送' },
      { key: 'wx_balance_warning_threshold', label: '余额不足提醒阈值（元）', hint: '扣费后余额低于该金额触发提醒，默认 5 元' }
    ]
  }
]

const loading = ref(false)
const saving = ref(false)
const configs = ref<Record<string, string>>({})

const loadConfig = async () => {
  loading.value = true
  try {
    const res: any = await getConfig()
    configs.value = res.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)

const saveConfig = async () => {
  saving.value = true
  try {
    await updateConfig(configs.value)
    ElMessage.success('配置已保存，校园规则立即生效')
  } finally {
    saving.value = false
  }
}

/** 只显示后端确实返回了的键，避免出现空字段 */
const visibleFields = (group: GroupMeta) =>
  group.fields.filter((f) => configs.value[f.key] !== undefined)

// 未在分组中登记的键（例如后续新增配置），统一放到「其他配置」，避免漏显示
const otherKeys = computed(() => {
  const known = new Set(GROUPS.flatMap((g) => g.fields.map((f) => f.key)))
  return Object.keys(configs.value).filter((k) => !known.has(k))
})

const setValue = (key: string, value: string) => {
  configs.value = { ...configs.value, [key]: value }
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>系统配置</template>

      <div v-loading="loading">
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="校园规则说明"
          description="此处配置与校园身份（学生/教师/职工/访客/家长）、校园分区、预约与出场结算直接关联，保存后立即生效；费率系数小于 1 表示折扣、大于 1 表示加价，免费时长在出场结算时从停放时长中先行扣减。"
          style="margin-bottom: 16px"
        />

        <template v-for="group in GROUPS" :key="group.title">
          <el-card v-if="visibleFields(group).length > 0" shadow="never" style="margin-bottom: 16px">
            <template #header>{{ group.title }}</template>

            <div v-if="group.desc" style="font-size: 12px; color: #606266; margin-bottom: 12px">
              {{ group.desc }}
            </div>

            <el-form label-width="200px" label-position="left" style="max-width: 720px">
              <el-form-item v-for="field in visibleFields(group)" :key="field.key" :label="field.label">
                <el-select
                  v-if="field.type === 'select'"
                  :model-value="configs[field.key]"
                  style="max-width: 320px"
                  @update:model-value="(v: any) => setValue(field.key, v)"
                >
                  <el-option
                    v-for="o in field.options || []"
                    :key="o.value"
                    :label="o.label"
                    :value="o.value"
                  />
                </el-select>
                <el-input
                  v-else
                  :model-value="configs[field.key]"
                  :placeholder="field.label"
                  style="max-width: 320px"
                  @update:model-value="(v: any) => setValue(field.key, v)"
                />
                <div v-if="field.hint" style="font-size: 12px; color: #606266; line-height: 1.5">
                  {{ field.hint }}
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <el-card v-if="otherKeys.length > 0" shadow="never" style="margin-bottom: 16px">
          <template #header>其他配置</template>
          <div style="font-size: 12px; color: #606266; margin-bottom: 12px">
            以下为尚未登记中文名称的配置项，显示原始配置键便于排查，修改后同样会保存生效。
          </div>
          <el-form label-width="220px" label-position="left" style="max-width: 720px">
            <el-form-item v-for="key in otherKeys" :key="key" :label="`未登记配置（${key}）`">
              <el-input
                :model-value="configs[key]"
                style="max-width: 320px"
                @update:model-value="(v: any) => setValue(key, v)"
              />
            </el-form-item>
          </el-form>
        </el-card>

        <el-divider />
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </div>
    </el-card>
  </div>
</template>
