import { useCallback, useEffect, useState } from 'react'
import {
  Row, Col, Card, Table, Tag, Button, Input, Select, Modal, Space, App as AntdApp, Popconfirm
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  getVisitReports, getVisitReportStats, approveVisitReport, rejectVisitReport,
  checkInVisit, checkOutVisit
} from '@/api'

const STATUS_META: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'processing' },
  1: { text: '已通过', color: 'success' },
  2: { text: '已驳回', color: 'error' },
  3: { text: '已入场', color: 'warning' },
  4: { text: '已完成', color: 'default' },
  5: { text: '已过期', color: 'default' },
  6: { text: '已撤销', color: 'default' }
}

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

export default function Visits() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [keyword, setKeyword] = useState('')
  const [stats, setStats] = useState<any>({})
  const [remarkOpen, setRemarkOpen] = useState(false)
  const [remarkType, setRemarkType] = useState<'approve' | 'reject'>('approve')
  const [target, setTarget] = useState<any>(null)
  const [remark, setRemark] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params: any = { page, pageSize, keyword }
      if (status !== undefined) params.status = status
      const [list, st]: any[] = await Promise.all([getVisitReports(params), getVisitReportStats()])
      setRows(list.data.records || [])
      setTotal(list.data.total || 0)
      setStats(st.data || {})
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => { load() }, [load])

  const openRemark = (row: any, type: 'approve' | 'reject') => {
    setTarget(row)
    setRemarkType(type)
    setRemark('')
    setRemarkOpen(true)
  }

  const submitRemark = async () => {
    if (!target) return
    if (remarkType === 'approve') {
      await approveVisitReport(target.id, undefined, remark)
      message.success('已通过审核，并自动分配访客区车位')
    } else {
      await rejectVisitReport(target.id, remark)
      message.success('已驳回该报备')
    }
    setRemarkOpen(false)
    load()
  }

  const doCheckIn = async (row: any) => {
    await checkInVisit(row.id)
    message.success('已登记入场')
    load()
  }

  const doCheckOut = async (row: any) => {
    await checkOutVisit(row.id)
    message.success('已出场结算（按身份免费时长与费率）')
    load()
  }

  const statCards = [
    { label: '待审核', value: stats.pending ?? 0, color: '#409EFF' },
    { label: '已通过', value: stats.approved ?? 0, color: '#67C23A' },
    { label: '在停访客/家长', value: stats.parked ?? 0, color: '#E6A23C' },
    { label: '已完成', value: stats.finished ?? 0, color: '#909399' },
    { label: '访客区空闲车位', value: stats.visitorZoneFree ?? 0, color: '#722ED1' },
    { label: '家长免费时长', value: `${stats.parentFreeMinutes ?? 30} 分钟`, color: '#13C2C2' }
  ]

  const columns: ColumnsType<any> = [
    {
      title: '类型', dataIndex: 'visitType', width: 90, align: 'center',
      render: (v: number) => (v === 2 ? <Tag color="cyan">家长接送</Tag> : <Tag color="blue">访客</Tag>)
    },
    { title: '来访人', dataIndex: 'visitorName', width: 110, render: (v: any) => v || '-' },
    {
      title: '来访车牌', dataIndex: 'plateNumber', width: 130,
      render: (v: any) => <span style={{ fontWeight: 600 }}>{v}</span>
    },
    { title: '事由', dataIndex: 'purpose', ellipsis: true, render: (v: any) => v || '-' },
    {
      title: '预计时段', width: 230,
      render: (_: any, r: any) => `${fmtTime(r.expectedStart)} ~ ${fmtTime(r.expectedEnd)}`
    },
    { title: '报备人', dataIndex: 'ownerName', width: 170, render: (v: any) => v || '-' },
    { title: '车位', dataIndex: 'spotCode', width: 90, render: (v: any) => v || '-' },
    {
      title: '状态', dataIndex: 'status', width: 100, align: 'center',
      render: (v: number) => {
        const m = STATUS_META[v] || { text: '未知', color: 'default' }
        return <Tag color={m.color}>{m.text}</Tag>
      }
    },
    {
      title: '操作', width: 210, fixed: 'right', align: 'center',
      render: (_: any, row: any) => {
        if (row.status === 0) {
          return (
            <Space size={4}>
              <Button type="link" size="small" onClick={() => openRemark(row, 'approve')}>通过</Button>
              <Button type="link" size="small" danger onClick={() => openRemark(row, 'reject')}>驳回</Button>
            </Space>
          )
        }
        if (row.status === 1) {
          return (
            <Popconfirm title="登记访客车辆入场？" onConfirm={() => doCheckIn(row)}>
              <Button type="link" size="small">登记入场</Button>
            </Popconfirm>
          )
        }
        if (row.status === 3) {
          return (
            <Popconfirm title="确认出场并结算费用？" onConfirm={() => doCheckOut(row)}>
              <Button type="link" size="small">出场结算</Button>
            </Popconfirm>
          )
        }
        return <span style={{ color: '#c0c4cc' }}>—</span>
      }
    }
  ]

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {statCards.map((c) => (
          <Col xs={12} sm={8} xl={4} key={c.label}>
            <Card bordered={false} className="stat-card stat-card-hover">
              <div style={{ color: '#909399', fontSize: 13, marginBottom: 6 }}>{c.label}</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: c.color }}>{c.value}</div>
            </Card>
          </Col>
        ))}
      </Row>

      <Card
        title="访客与家长车辆报备"
        extra={
          <Space>
            <Select
              allowClear
              placeholder="状态筛选"
              style={{ width: 140 }}
              value={status}
              onChange={(v) => { setPage(1); setStatus(v) }}
              options={Object.keys(STATUS_META).map((k) => ({ value: Number(k), label: STATUS_META[Number(k)].text }))}
            />
            <Input.Search
              placeholder="搜索车牌/来访人"
              allowClear
              style={{ width: 220 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={() => { setPage(1); load() }}
            />
            <Button onClick={load}>刷新</Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={rows}
          loading={loading}
          size="middle"
          scroll={{ x: 1400 }}
          pagination={{ current: page, pageSize, total, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setPage(p) }}
        />
      </Card>

      <Modal
        title={remarkType === 'approve' ? '通过报备（自动分配访客区车位）' : '驳回报备'}
        open={remarkOpen}
        onCancel={() => setRemarkOpen(false)}
        onOk={submitRemark}
        okText={remarkType === 'approve' ? '通过' : '驳回'}
        okButtonProps={{ danger: remarkType === 'reject' }}
      >
        <div style={{ marginBottom: 12, color: '#606266' }}>
          车牌：<b>{target?.plateNumber}</b>　类型：{target?.visitType === 2 ? '家长接送' : '访客'}
        </div>
        <Input.TextArea
          rows={3}
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          placeholder={remarkType === 'approve' ? '审核意见（可选）' : '驳回原因（建议填写）'}
        />
      </Modal>
    </div>
  )
}
