import { useCallback, useEffect, useState } from 'react'
import { Row, Col, Card, Table, Tag, Button, Input, Select, Modal, App as AntdApp } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getViolations, getViolationStats, handleViolation } from '@/api'

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

export default function Violations() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [keyword, setKeyword] = useState('')
  const [stats, setStats] = useState<any>({})
  const [target, setTarget] = useState<any>(null)
  const [remark, setRemark] = useState('')
  const [open, setOpen] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params: any = { page, pageSize, keyword }
      if (status !== undefined) params.status = status
      const [list, st]: any[] = await Promise.all([getViolations(params), getViolationStats()])
      setRows(list.data.records || [])
      setTotal(list.data.total || 0)
      setStats(st.data || {})
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => { load() }, [load])

  const submit = async () => {
    if (!target) return
    await handleViolation(target.id, remark)
    message.success('已处理')
    setOpen(false)
    load()
  }

  const columns: ColumnsType<any> = [
    { title: '车牌号', dataIndex: 'plateNumber', width: 130, render: (v: any) => <b>{v || '-'}</b> },
    {
      title: '类型', dataIndex: 'type', width: 120,
      render: (v: string) => (v === 'overtime' ? <Tag color="orange">超时占用</Tag> : <Tag>{v}</Tag>)
    },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    { title: '扣分', dataIndex: 'points', width: 80, align: 'center' },
    {
      title: '状态', dataIndex: 'status', width: 100, align: 'center',
      render: (v: number) => (v === 1 ? <Tag color="success">已处理</Tag> : <Tag color="error">未处理</Tag>)
    },
    { title: '记录时间', dataIndex: 'createdAt', width: 170, render: fmtTime },
    {
      title: '操作', width: 110, fixed: 'right', align: 'center',
      render: (_: any, row: any) =>
        row.status === 1 ? (
          <span style={{ color: '#c0c4cc' }}>—</span>
        ) : (
          <Button type="link" size="small" onClick={() => { setTarget(row); setRemark(''); setOpen(true) }}>
            处理
          </Button>
        )
    }
  ]

  const statCards = [
    { label: '违规总数', value: stats.total ?? 0, color: '#303133' },
    { label: '待处理', value: stats.pending ?? 0, color: '#F56C6C' },
    { label: '已处理', value: stats.handled ?? 0, color: '#67C23A' }
  ]

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {statCards.map((c) => (
          <Col xs={24} sm={8} key={c.label}>
            <Card bordered={false} className="stat-card stat-card-hover">
              <div style={{ color: '#909399', fontSize: 13, marginBottom: 6 }}>{c.label}</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: c.color }}>{c.value}</div>
            </Card>
          </Col>
        ))}
      </Row>

      <Card
        title="校园违规记录（出场时超时自动生成）"
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            <Select
              allowClear
              placeholder="状态筛选"
              style={{ width: 130 }}
              value={status}
              onChange={(v) => { setPage(1); setStatus(v) }}
              options={[{ value: 0, label: '未处理' }, { value: 1, label: '已处理' }]}
            />
            <Input.Search
              placeholder="搜索车牌"
              allowClear
              style={{ width: 200 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={() => { setPage(1); load() }}
            />
            <Button onClick={load}>刷新</Button>
          </div>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={rows}
          loading={loading}
          size="middle"
          pagination={{ current: page, pageSize, total, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setPage(p) }}
        />
      </Card>

      <Modal
        title="处理违规记录"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={submit}
        okText="确认处理"
      >
        <div style={{ marginBottom: 12, color: '#606266' }}>
          车牌：<b>{target?.plateNumber}</b>　{target?.description}
        </div>
        <Input.TextArea
          rows={3}
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          placeholder="处理意见（可选，例如：已电话通知车主）"
        />
      </Modal>
    </div>
  )
}
