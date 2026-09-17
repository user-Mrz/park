import { useCallback, useEffect, useState } from 'react'
import {
  Row, Col, Card, Table, Tag, Button, Input, Modal, DatePicker, Space, App as AntdApp, Popconfirm, Empty
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { getBlacklist, addBlacklist, removeBlacklist } from '@/api'

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

export default function Blacklist() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [open, setOpen] = useState(false)
  const [plateNumber, setPlateNumber] = useState('')
  const [reason, setReason] = useState('')
  const [until, setUntil] = useState<any>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getBlacklist({ page, pageSize, keyword })
      setRows(res.data.records || [])
      setTotal(res.data.total || 0)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, keyword])

  useEffect(() => { load() }, [load])

  const submit = async () => {
    if (!plateNumber.trim()) {
      message.warning('请填写车牌号')
      return
    }
    await addBlacklist({
      plateNumber: plateNumber.trim().toUpperCase(),
      reason,
      untilTime: until ? until.format('YYYY-MM-DD HH:mm:ss') : null
    })
    message.success('已加入黑名单（该车牌将无法预约与报备）')
    setOpen(false)
    setPlateNumber('')
    setReason('')
    setUntil(null)
    load()
  }

  const remove = async (row: any) => {
    await removeBlacklist(row.id)
    message.success('已移出黑名单')
    load()
  }

  const columns: ColumnsType<any> = [
    { title: '车牌号', dataIndex: 'plateNumber', width: 150, render: (v: any) => <b>{v || '（按用户）'}</b> },
    { title: '用户ID', dataIndex: 'userId', width: 200, render: (v: any) => (v ? String(v) : '—') },
    { title: '原因', dataIndex: 'reason', ellipsis: true, render: (v: any) => v || '-' },
    {
      title: '有效期', dataIndex: 'untilTime', width: 190,
      render: (v: any) => (v ? fmtTime(v) : <Tag color="red">永久</Tag>)
    },
    { title: '加入时间', dataIndex: 'createdAt', width: 180, render: fmtTime },
    {
      title: '操作', width: 110, fixed: 'right', align: 'center',
      render: (_: any, row: any) => (
        <Popconfirm title="确定移出黑名单？" onConfirm={() => remove(row)}>
          <Button type="link" size="small" danger>移出</Button>
        </Popconfirm>
      )
    }
  ]

  return (
    <div>
      <Card
        title="校园车辆黑名单（命中后无法预约车位与提交访客报备）"
        extra={
          <Space>
            <Input.Search
              placeholder="搜索车牌或原因"
              allowClear
              style={{ width: 220 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={() => { setPage(1); load() }}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>加入黑名单</Button>
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
          locale={{ emptyText: <Empty description="暂无黑名单记录" /> }}
          pagination={{ current: page, pageSize, total, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setPage(p) }}
        />
      </Card>

      <Modal title="加入黑名单" open={open} onCancel={() => setOpen(false)} onOk={submit} okText="确认加入">
        <div style={{ marginBottom: 12 }}>
          <Input
            placeholder="车牌号，如 粤B12345"
            value={plateNumber}
            onChange={(e) => setPlateNumber(e.target.value)}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <Input.TextArea
            rows={2}
            placeholder="加入原因，如：多次超时占用、占用他人预约车位"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </div>
        <DatePicker
          showTime
          style={{ width: '100%' }}
          placeholder="有效期截止（留空表示永久）"
          value={until}
          onChange={setUntil}
          disabledDate={(d) => d && d < dayjs().startOf('day')}
        />
      </Modal>
    </div>
  )
}
