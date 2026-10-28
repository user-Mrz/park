const { get } = require('../../utils/request.js')

/** 停车规则分组定义（键名与后端 SystemConfig 一致） */
const GROUPS = [
  {
    title: '基础计费与预约',
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
    desc: '系数 <1 为折扣、>1 为加价；免费时长在出场结算时先行抵扣；未认证按访客处理',
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
    desc: '夜间禁停、学期/假期模式与违规判定',
    items: [
      { key: 'rule_night_ban_start', label: '夜间禁停开始', hint: '24 小时制' },
      { key: 'rule_night_ban_end', label: '夜间禁停结束', hint: '24 小时制' },
      { key: 'semester_mode', label: '学期/假期模式' },
      { key: 'holiday_discount', label: '假期模式统一折扣', hint: '0.8 表示八折，仅假期模式生效' },
      { key: 'violation_overtime_grace_minutes', label: '超时违规宽限期', suffix: '分钟', hint: '超过预约/预计离场时间多久记为违规' }
    ]
  }
]

/** 违规规则（与后端 ViolationServiceImpl 一致） */
const VIOLATION_RULES = [
  { type: 'overtime', name: '超时占用', points: '1 ~ 2 分', desc: '超过预约结束时间或预计离场时间，且超出宽限期仍未离场' },
  { type: 'illegal_zone', name: '违规区域入场', points: '2 分', desc: '在非允许分区入场，或身份与车位允许身份不符时入场' },
  { type: 'occupied_others', name: '占用他人预约车位', points: '2 分', desc: '占用他人已预约且处于有效期的车位' }
]

function fmtValue(cfg, item) {
  const v = cfg[item.key]
  if (v === undefined || v === '') return '—'
  if (item.key === 'semester_mode') {
    return v === '1' ? '假期模式（关闭教学区/图书馆区预约）' : '学期模式（正常开放）'
  }
  return v + (item.suffix ? ' ' + item.suffix : '')
}

Page({
  data: {
    loading: true,
    groups: [],
    violations: [],
    grace: '?'
  },

  onLoad() {
    this.loadRules()
  },

  loadRules() {
    this.setData({ loading: true })
    get('/user/rules')
      .then((res) => {
        const cfg = res.data || {}
        const groups = GROUPS.map((g) => ({
          title: g.title,
          desc: g.desc,
          items: g.items
            .filter((it) => cfg[it.key] !== undefined)
            .map((it) => ({
              label: it.label,
              hint: it.hint || '',
              value: fmtValue(cfg, it)
            }))
        })).filter((g) => g.items.length > 0)

        const grace = cfg.violation_overtime_grace_minutes || '?'
        const violations = VIOLATION_RULES.map((r) => ({ ...r }))
        this.setData({ groups, violations, grace, loading: false })
      })
      .catch(() => {
        this.setData({ loading: false })
      })
  }
})
