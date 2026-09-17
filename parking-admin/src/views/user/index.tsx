import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Row,
  Col,
  Card,
  Table,
  Tag,
  Button,
  Input,
  Select,
  Radio,
  Popconfirm,
  Drawer,
  Descriptions,
  Avatar,
  Typography,
  Spin,
  Empty,
  Space,
  Modal,
  App as AntdApp
} from 'antd'
import { SearchOutlined, UserOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { getUsers, getUserDetail, updateUserStatus, auditUserIdentity, getDashboard } from '@/api'

const { Text } = Typography

/** 校园身份类型 */
const IDENTITY_META: Record<number, { text: string; color: string }> = {
  1: { text: '学生', color: 'blue' },
  2: { text: '教师', color: 'green' },
  3: { text: '职工', color: 'purple' },
  4: { text: '访客', color: 'default' },
  5: { text: '家长', color: 'cyan' }
}

/** 身份认证状态 */
const AUDIT_META: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'processing' },
  1: { text: '已认证', color: 'success' },
  2: { text: '已驳回', color: 'error' }
}

const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

function maskOpenid(openid: any) {
  const s = String(openid || '')
  if (!s) return '未绑定微信'
  return s.length <= 10 ? s : s.slice(0, 6) + '****' + s.slice(-4)
}

export default function Users() {
  const { message } = AntdApp.useApp()
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(false)
  const [tableData, setTableData] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [filterStatus, setFilterStatus] = useState<number | undefined>(undefined)
  const [filterIdentity, setFilterIdentity] = useState<number | undefined>(undefined)
  // 支持从首页「待审核身份」卡片跳转过来时直接筛选：/users?identityStatus=0
  const [filterAudit, setFilterAudit] = useState<number | undefined>(() => {
    const v = searchParams.get('identityStatus')
    return v === null || v === '' ? undefined : Number(v)
  })
  const [auditTarget, setAuditTarget] = useState<any>(null)
  const [auditResult, setAuditResult] = useState<number>(1)
  const [auditRemark, setAuditRemark] = useState('')
  const [auditOpen, setAuditOpen] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [currentUser, setCurrentUser] = useState<any>(null)
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    disabledUsers: 0,
    pendingUsers: 0,
    totalBalance: '0.00'
  })

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const params: any = { page, pageSize, keyword }
      if (filterStatus !== undefined) {
        params.status = filterStatus
      }
      if (filterIdentity !== undefined) params.identityType = filterIdentity
      if (filterAudit !== undefined) params.identityStatus = filterAudit
      const res: any = await getUsers(params)
      setTableData(res.data.records)
      setTotal(res.data.total)

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
      setStats({
        totalUsers: allRes.data.total,
        activeUsers: activeRes.data.total,
        disabledUsers: disabledRes.data.total,
        pendingUsers: pendingRes.data.total,
        totalBalance: balance
      })
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword, filterStatus, filterIdentity, filterAudit])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleFilter = (status: number | undefined) => {
    setPage(1)
    setFilterStatus(status)
  }

  const showDetail = async (row: any) => {
    setDetailVisible(true)
    setDetailLoading(true)
    setCurrentUser(null)
    try {
      const res: any = await getUserDetail(row.id)
      setCurrentUser(res.data)
    } catch {
      /* 错误提示由 request 拦截器统一处理 */
    } finally {
      setDetailLoading(false)
    }
  }

  const toggleStatus = async (row: any) => {
    const newStatus = row.status === 1 ? 0 : 1
    await updateUserStatus(row.id, newStatus)
    message.success(newStatus === 1 ? '已启用该用户' : '已禁用该用户')
    loadData()
    // 详情抽屉打开时同步刷新
    if (detailVisible && currentUser && String(currentUser.id) === String(row.id)) {
      showDetail({ id: row.id })
    }
  }

  const openAudit = (row: any, result: number = 1) => {
    setAuditTarget(row)
    setAuditResult(result)
    setAuditRemark('')
    setAuditOpen(true)
  }

  const submitAudit = async () => {
    if (!auditTarget) return
    await auditUserIdentity(auditTarget.id, auditResult, auditRemark)
    const label = auditResult === 1 ? '已通过身份认证' : auditResult === 2 ? '已驳回身份认证' : '已重置为待审核'
    message.success(label)
    setAuditOpen(false)
    loadData()
    if (detailVisible && currentUser && String(currentUser.id) === String(auditTarget.id)) {
      showDetail({ id: auditTarget.id })
    }
  }

  const columns: ColumnsType<any> = [
    {
      title: '用户',
      dataIndex: 'nickname',
      width: 210,
      render: (_: any, row: any) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Avatar size={36} src={row.avatar || undefined} icon={<UserOutlined />} />
          <div style={{ minWidth: 0 }}>
            <div style={{ fontWeight: 600, color: '#303133' }}>{row.nickname || '（未设置昵称）'}</div>
            <Text type="secondary" style={{ fontSize: 12 }} copyable={{ text: String(row.id), tooltips: ['复制用户ID', '已复制'] }}>
              ID {String(row.id).slice(0, 8)}…
            </Text>
          </div>
        </div>
      )
    },
    {
      title: '校园身份',
      dataIndex: 'identityType',
      width: 220,
      render: (_: any, row: any) => {
        const id = IDENTITY_META[row.identityType] || { text: '未知', color: 'default' }
        const audit = AUDIT_META[row.identityStatus] || AUDIT_META[0]
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <Tag color={id.color}>{id.text}</Tag>
              <Tag color={audit.color}>{audit.text}</Tag>
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {row.userNo ? `学号/工号 ${row.userNo}` : '未填写学号/工号'}
              {row.department ? ` · ${row.department}` : ''}
            </Text>
          </div>
        )
      }
    },
    { title: '手机号', dataIndex: 'phone', width: 130, render: (v: any) => v || <Text type="secondary">未绑定</Text> },
    { title: '车牌号', dataIndex: 'plateNumber', width: 120, render: (v: any) => v || <Text type="secondary">未设置</Text> },
    {
      title: '余额',
      dataIndex: 'balance',
      width: 120,
      align: 'right',
      render: (v: any) => <span style={{ fontWeight: 600 }}>{fmtMoney(v)}</span>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      align: 'center',
      render: (status: number) => <Tag color={status === 1 ? 'success' : 'error'}>{status === 1 ? '正常' : '禁用'}</Tag>
    },
    { title: '注册时间', dataIndex: 'createdAt', width: 165, render: (v: any) => fmtTime(v) },
    {
      title: '操作',
      width: 250,
      fixed: 'right',
      align: 'center',
      render: (_: any, row: any) => (
        <Space size={2} wrap>
          <Button type="link" size="small" onClick={() => showDetail(row)}>
            查看信息
          </Button>
          {/* 身份审核入口对所有用户开放：待审核可直接通过/驳回，已认证可复核或重置 */}
          <Button
            type="link"
            size="small"
            danger={row.identityStatus === 0}
            onClick={() => openAudit(row, row.identityStatus === 0 ? 1 : 1)}
          >
            {row.identityStatus === 0 ? '身份审核' : '复核/驳回'}
          </Button>
          <Popconfirm
            title={row.status === 1 ? '确定禁用该用户？禁用后将取消其预约并结算在停车辆' : '确定启用该用户？'}
            onConfirm={() => toggleStatus(row)}
          >
            <Button type="link" size="small" danger={row.status === 1}>
              {row.status === 1 ? '禁用' : '启用'}
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const statCards = [
    { label: '用户总数', value: stats.totalUsers, cls: '', onClick: () => handleFilter(undefined) },
    { label: '正常用户', value: stats.activeUsers, cls: 'active', onClick: () => handleFilter(1) },
    { label: '禁用用户', value: stats.disabledUsers, cls: 'disabled', onClick: () => handleFilter(0) },
    {
      label: '待审核身份',
      value: stats.pendingUsers,
      cls: 'pending',
      onClick: () => { setPage(1); setFilterAudit(0) }
    },
    { label: '账户总余额', value: `¥${stats.totalBalance}`, cls: 'balance', onClick: undefined }
  ]

  const biz = currentUser?.stats || {}

  const bizCards = [
    { label: '停车次数', value: biz.vehicleRecordCount ?? 0, extra: biz.parkingNow ? `在场 ${biz.parkingNow} 辆` : '当前无在场车辆' },
    { label: '预约总数', value: biz.reservationCount ?? 0, extra: biz.activeReservationCount ? `进行中 ${biz.activeReservationCount} 个` : '无进行中预约' },
    { label: '累计充值', value: fmtMoney(biz.rechargeAmount), extra: `${biz.rechargeCount ?? 0} 笔` },
    { label: '累计消费', value: fmtMoney(biz.paymentAmount), extra: '停车扣费合计' }
  ]

  return (
    <div className="user-page">
      <Row gutter={16} className="stats-row" style={{ marginBottom: 16 }}>
        {statCards.map((card) => (
          <Col xs={12} sm={8} xl={4} key={card.label}>
            <div
              className={`stat-card ${card.cls}`}
              onClick={card.onClick}
              style={{
                background: '#fff',
                borderRadius: 8,
                padding: '20px 16px',
                textAlign: 'center',
                cursor: card.onClick ? 'pointer' : 'default',
                transition: 'all 0.2s',
                border: card.cls === 'pending' && stats.pendingUsers > 0 ? '2px solid #409EFF' : '2px solid transparent'
              }}
            >
              <div
                style={{
                  fontSize: 28,
                  fontWeight: 700,
                  color: card.cls === 'pending' && stats.pendingUsers > 0 ? '#409EFF' : '#303133',
                  marginBottom: 4
                }}
              >
                {card.value}
              </div>
              <div style={{ fontSize: 13, color: '#909399' }}>{card.label}</div>
            </div>
          </Col>
        ))}
      </Row>

      <Card
        title={
          <span>
            用户列表
            {stats.pendingUsers > 0 && (
              <Tag
                color="processing"
                style={{ marginLeft: 8, cursor: 'pointer' }}
                onClick={() => { setPage(1); setFilterAudit(0) }}
              >
                待审核身份 {stats.pendingUsers} 人
              </Tag>
            )}
            {filterIdentity !== undefined && (
              <Tag color="blue" style={{ marginLeft: 8 }}>
                {(IDENTITY_META[filterIdentity] || { text: '身份' }).text}
              </Tag>
            )}
            {filterAudit !== undefined && (
              <Tag color="processing" style={{ marginLeft: 8 }}>
                {(AUDIT_META[filterAudit] || AUDIT_META[0]).text}
              </Tag>
            )}
            {filterStatus === 1 && (
              <Tag color="success" style={{ marginLeft: 8 }}>
                正常
              </Tag>
            )}
            {filterStatus === 0 && (
              <Tag color="error" style={{ marginLeft: 8 }}>
                禁用
              </Tag>
            )}
            {(filterStatus !== undefined || filterIdentity !== undefined || filterAudit !== undefined) && (
              <Button
                type="link"
                size="small"
                onClick={() => { handleFilter(undefined); setFilterIdentity(undefined); setFilterAudit(undefined) }}
                style={{ marginLeft: 4 }}
              >
                清除筛选
              </Button>
            )}
          </span>
        }
        extra={
          <Space wrap>
            <Select
              allowClear
              placeholder="校园身份"
              style={{ width: 120 }}
              value={filterIdentity}
              onChange={(v) => { setPage(1); setFilterIdentity(v) }}
              options={Object.keys(IDENTITY_META).map((k) => ({ value: Number(k), label: IDENTITY_META[Number(k)].text }))}
            />
            <Select
              allowClear
              placeholder="认证状态"
              style={{ width: 120 }}
              value={filterAudit}
              onChange={(v) => { setPage(1); setFilterAudit(v) }}
              options={Object.keys(AUDIT_META).map((k) => ({ value: Number(k), label: AUDIT_META[Number(k)].text }))}
            />
            <Input.Search
              placeholder="搜索手机号/昵称/车牌/学号"
              allowClear
              style={{ width: 240 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={() => {
                setPage(1)
                loadData()
              }}
              enterButton={<SearchOutlined />}
            />
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={tableData}
          loading={loading}
          size="middle"
          pagination={{
            current: page,
            pageSize,
            total,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p) => setPage(p)
          }}
        />
      </Card>

      {/* 用户信息抽屉 */}
      <Drawer
        title="用户信息"
        width={620}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        destroyOnClose
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: '80px 0' }}>
            <Spin />
          </div>
        ) : !currentUser ? (
          <Empty description="未获取到用户信息" />
        ) : (
          <>
            {/* 头部：头像 + 昵称 + 状态 */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 16,
                padding: 16,
                background: '#f7f8fa',
                borderRadius: 10,
                marginBottom: 20
              }}
            >
              <Avatar size={64} src={currentUser.avatar || undefined} icon={<UserOutlined />} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 18, fontWeight: 700, color: '#303133' }}>
                  {currentUser.nickname || '（未设置昵称）'}
                </div>
                <div style={{ marginTop: 6, display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                  <Tag color={currentUser.status === 1 ? 'success' : 'error'}>
                    {currentUser.status === 1 ? '账号正常' : '账号已禁用'}
                  </Tag>
                  <Text type="secondary">{currentUser.phone || '未绑定手机号'}</Text>
                  {currentUser.plateNumber && <Tag color="blue">{currentUser.plateNumber}</Tag>}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: '#909399' }}>账户余额</div>
                <div style={{ fontSize: 20, fontWeight: 700, color: '#409EFF' }}>{fmtMoney(currentUser.balance)}</div>
              </div>
            </div>

            {/* 业务统计 */}
            <div style={{ fontSize: 15, fontWeight: 700, color: '#303133', marginBottom: 12 }}>业务统计</div>
            <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
              {bizCards.map((c) => (
                <Col span={12} key={c.label}>
                  <div style={{ background: '#fff', border: '1px solid #f0f0f0', borderRadius: 10, padding: '14px 16px' }}>
                    <div style={{ fontSize: 13, color: '#909399', marginBottom: 6 }}>{c.label}</div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: '#303133' }}>{c.value}</div>
                    <div style={{ fontSize: 12, color: '#c0c4cc', marginTop: 4 }}>{c.extra}</div>
                  </div>
                </Col>
              ))}
            </Row>

            {/* 账号资料 */}
            <div style={{ fontSize: 15, fontWeight: 700, color: '#303133', marginBottom: 12 }}>账号资料</div>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="用户 ID">
                <Text copyable style={{ fontSize: 12 }}>{String(currentUser.id)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="校园身份">
                <Space>
                  <Tag color={(IDENTITY_META[currentUser.identityType] || { color: 'default' }).color}>
                    {currentUser.identityLabel || (IDENTITY_META[currentUser.identityType] || { text: '未知' }).text}
                  </Tag>
                  {/* 认证状态以后端返回的 identityStatus 为准；缺失时显示“—”而不是误导性的“待审核” */}
                  {currentUser.identityStatus === undefined || currentUser.identityStatus === null ? (
                    <Tag>—</Tag>
                  ) : (
                    <Tag color={(AUDIT_META[currentUser.identityStatus] || AUDIT_META[0]).color}>
                      {currentUser.identityStatusLabel
                        || (AUDIT_META[currentUser.identityStatus] || AUDIT_META[0]).text}
                    </Tag>
                  )}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="学号 / 工号">{currentUser.userNo || '未填写'}</Descriptions.Item>
              <Descriptions.Item label="院系 / 部门">{currentUser.department || '-'}</Descriptions.Item>
              <Descriptions.Item label="班级">{currentUser.className || '-'}</Descriptions.Item>
              {currentUser.identityRemark && (
                <Descriptions.Item label="审核备注">{currentUser.identityRemark}</Descriptions.Item>
              )}
              <Descriptions.Item label="微信绑定">{maskOpenid(currentUser.openid)}</Descriptions.Item>
              <Descriptions.Item label="手机号">{currentUser.phone || '未绑定'}</Descriptions.Item>
              <Descriptions.Item label="车牌号">{currentUser.plateNumber || '未设置'}</Descriptions.Item>
              <Descriptions.Item label="头像">
                {currentUser.avatar ? <a href={currentUser.avatar} target="_blank" rel="noreferrer">查看原图</a> : '未上传'}
              </Descriptions.Item>
              <Descriptions.Item label="注册时间">{fmtTime(currentUser.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="资料更新时间">{fmtTime(currentUser.updatedAt)}</Descriptions.Item>
              <Descriptions.Item label="最近一次停车">
                {biz.lastEntryTime ? `${fmtTime(biz.lastEntryTime)}${biz.lastPlateNumber ? ' · ' + biz.lastPlateNumber : ''}` : '暂无停车记录'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 20, display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
              <Button onClick={() => setDetailVisible(false)}>关闭</Button>
              <Button onClick={() => openAudit(currentUser, 2)} danger>驳回认证</Button>
              <Button type="primary" onClick={() => openAudit(currentUser, 1)}>认证通过</Button>
              <Popconfirm
                title={currentUser.status === 1 ? '确定禁用该用户？' : '确定启用该用户？'}
                onConfirm={() => toggleStatus(currentUser)}
              >
                <Button type="primary" danger={currentUser.status === 1}>
                  {currentUser.status === 1 ? '禁用账号' : '启用账号'}
                </Button>
              </Popconfirm>
            </div>
          </>
        )}
      </Drawer>

      {/* 校园身份审核 */}
      <Modal
        title="校园身份认证审核"
        open={auditOpen}
        onCancel={() => setAuditOpen(false)}
        onOk={submitAudit}
        okText={auditResult === 1 ? '通过认证' : auditResult === 2 ? '驳回' : '重置为待审核'}
        okButtonProps={{ danger: auditResult === 2 }}
      >
        <div style={{ marginBottom: 12, color: '#606266' }}>
          用户：<b>{auditTarget?.nickname || '-'}</b>　身份：
          {(IDENTITY_META[auditTarget?.identityType] || { text: '未知' }).text}　学号/工号：
          <b>{auditTarget?.userNo || '未填写'}</b>
          <div style={{ marginTop: 4, fontSize: 12, color: '#909399' }}>
            {auditTarget?.department || '未填写院系'}{auditTarget?.className ? ` · ${auditTarget.className}` : ''}
            　当前状态：{(AUDIT_META[auditTarget?.identityStatus] || AUDIT_META[0]).text}
          </div>
        </div>
        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 13, color: '#606266', marginBottom: 6 }}>审核结果</div>
          <Radio.Group value={auditResult} onChange={(e) => setAuditResult(e.target.value)}>
            <Radio.Button value={1}>通过认证</Radio.Button>
            <Radio.Button value={2}>驳回</Radio.Button>
            <Radio.Button value={0}>重置为待审核</Radio.Button>
          </Radio.Group>
          <div style={{ fontSize: 12, color: '#909399', marginTop: 6 }}>
            通过后该用户可预约车位并享受校园优惠；驳回或重置后按访客规则计费。
          </div>
        </div>
        <Input.TextArea
          rows={3}
          value={auditRemark}
          onChange={(e) => setAuditRemark(e.target.value)}
          placeholder={auditResult === 1 ? '审核意见（可选，如：学号核验通过）' : '处理原因（建议填写）'}
        />
      </Modal>
    </div>
  )
}
