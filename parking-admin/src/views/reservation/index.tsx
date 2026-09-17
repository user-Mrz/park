import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Select, App as AntdApp } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getReservations, updateReservationStatus } from '@/api'

const STATUS_TAG: Record<number, { color: string; text: string }> = {
  0: { color: 'default', text: '待确认' },
  1: { color: 'warning', text: '进行中' },
  2: { color: 'success', text: '已完成' },
  3: { color: 'error', text: '已取消' },
  4: { color: 'default', text: '已过期' }
}

export default function Reservations() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [tableData, setTableData] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [filterStatus, setFilterStatus] = useState<number | undefined>(undefined)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const params: any = { page, pageSize }
      if (filterStatus !== undefined) params.status = filterStatus
      const res: any = await getReservations(params)
      setTableData(res.data.records)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, filterStatus])

  useEffect(() => {
    loadData()
  }, [loadData])

  const cancelReservation = async (row: any) => {
    await updateReservationStatus(row.id, 3)
    message.success('已取消')
    loadData()
  }

  const columns: ColumnsType<any> = [
    { title: '预约编号', dataIndex: 'reservationNo', width: 170 },
    { title: '用户ID', dataIndex: 'userId', width: 100 },
    { title: '车位ID', dataIndex: 'spotId', width: 100 },
    { title: '开始时间', dataIndex: 'startTime', width: 170 },
    { title: '结束时间', dataIndex: 'endTime', width: 170 },
    { title: '时长(h)', dataIndex: 'durationHours', width: 80 },
    { title: '金额(元)', dataIndex: 'totalAmount', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: number) => {
        const tag = STATUS_TAG[status] || { color: 'default', text: '未知' }
        return <Tag color={tag.color}>{tag.text}</Tag>
      }
    },
    {
      title: '操作',
      width: 120,
      render: (_: any, row: any) =>
        row.status === 1 ? (
          <Button size="small" type="primary" danger onClick={() => cancelReservation(row)}>
            取消预约
          </Button>
        ) : null
    }
  ]

  return (
    <div className="page-container">
      <Card
        title="预约管理"
        extra={
          <Select
            placeholder="筛选状态"
            allowClear
            style={{ width: 140 }}
            value={filterStatus}
            onChange={(v) => {
              setPage(1)
              setFilterStatus(v)
            }}
            options={[
              { label: '待确认', value: 0 },
              { label: '进行中', value: 1 },
              { label: '已完成', value: 2 },
              { label: '已取消', value: 3 },
              { label: '已过期', value: 4 }
            ]}
          />
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
    </div>
  )
}
