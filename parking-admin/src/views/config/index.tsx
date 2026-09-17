import { useEffect, useMemo, useState } from 'react'
import { Card, Form, Input, Button, Spin, Select, Alert, Divider, App as AntdApp } from 'antd'
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
      { key: 'grace_period_minutes', label: '预约宽限期（分钟）', hint: '预约开始后多久内到场仍可入场' }
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
  }
]

export default function Config() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [configs, setConfigs] = useState<Record<string, string>>({})

  useEffect(() => {
    const loadConfig = async () => {
      setLoading(true)
      try {
        const res: any = await getConfig()
        setConfigs(res.data || {})
      } finally {
        setLoading(false)
      }
    }
    loadConfig()
  }, [])

  const saveConfig = async () => {
    setSaving(true)
    try {
      await updateConfig(configs)
      message.success('配置已保存，校园规则立即生效')
    } finally {
      setSaving(false)
    }
  }

  // 未在分组中登记的键（例如后续新增配置），统一放到「其他配置」，避免漏显示
  const otherKeys = useMemo(() => {
    const known = new Set(GROUPS.flatMap((g) => g.fields.map((f) => f.key)))
    return Object.keys(configs).filter((k) => !known.has(k))
  }, [configs])

  const setValue = (key: string, value: string) => {
    setConfigs((prev) => ({ ...prev, [key]: value }))
  }

  const renderField = (field: FieldMeta) => (
    <Form.Item
      key={field.key}
      label={field.label}
      extra={field.hint ? <span style={{ fontSize: 12, color: '#909399' }}>{field.hint}</span> : undefined}
    >
      {field.type === 'select' ? (
        <Select
          value={configs[field.key]}
          options={field.options}
          onChange={(v) => setValue(field.key, v)}
          style={{ maxWidth: 320 }}
        />
      ) : (
        <Input
          value={configs[field.key]}
          placeholder={field.label}
          onChange={(e) => setValue(field.key, e.target.value)}
          style={{ maxWidth: 320 }}
        />
      )}
    </Form.Item>
  )

  return (
    <div className="page-container">
      <Card title="系统配置">
        <Spin spinning={loading}>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message="校园规则说明"
            description="此处配置与校园身份（学生/教师/职工/访客/家长）、校园分区、预约与出场结算直接关联，保存后立即生效；费率系数小于 1 表示折扣、大于 1 表示加价，免费时长在出场结算时从停放时长中先行扣减。"
          />

          {GROUPS.map((group) => {
            const fields = group.fields.filter((f) => configs[f.key] !== undefined)
            if (fields.length === 0) return null
            return (
              <Card
                key={group.title}
                type="inner"
                title={group.title}
                style={{ marginBottom: 16 }}
              >
                {group.desc && (
                  <div style={{ fontSize: 12, color: '#909399', marginBottom: 12 }}>{group.desc}</div>
                )}
                <Form labelCol={{ span: 7 }} wrapperCol={{ span: 15 }} labelAlign="left" style={{ maxWidth: 720 }}>
                  {fields.map(renderField)}
                </Form>
              </Card>
            )
          })}

          {otherKeys.length > 0 && (
            <Card type="inner" title="其他配置" style={{ marginBottom: 16 }}>
              <Form labelCol={{ span: 7 }} wrapperCol={{ span: 15 }} labelAlign="left" style={{ maxWidth: 720 }}>
                {otherKeys.map((key) => (
                  <Form.Item key={key} label={key}>
                    <Input
                      value={configs[key]}
                      onChange={(e) => setValue(key, e.target.value)}
                      style={{ maxWidth: 320 }}
                    />
                  </Form.Item>
                ))}
              </Form>
            </Card>
          )}

          <Divider />
          <Button type="primary" onClick={saveConfig} loading={saving}>
            保存配置
          </Button>
        </Spin>
      </Card>
    </div>
  )
}
