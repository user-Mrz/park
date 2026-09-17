import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Select, Modal, App as AntdApp } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { getVehicleRecords, forceExit } from '@/api'

export default function Vehicles() {
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
      const res: any = await getVehicleRecords(params)
      setTableData(res.data.records)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, filterStatus])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleForceExit = (row: any) => {
    Modal.confirm({
      title: `确定强制离场车辆 ${row.plateNumber}？`,
      icon: <ExclamationCircleOutlined />,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await forceExit(row.id)
          message.success('已强制离场')
          loadData()
        } catch (e: any) {
          message.error(e.message || '强制离场失败')
        }
      }
    })
  }

  const columns: ColumnsType<any> = [
    { title: '记录ID', dataIndex: 'id', width: 100 },
    { title: '用户ID', dataIndex: 'userId', width: 100 },
    { title: '车牌号', dataIndex: 'plateNumber', width: 120 },
    { title: '车位ID', dataIndex: 'spotId', width: 100 },
    { title: '入场时间', dataIndex: 'entryTime', width: 170 },
    { title: '出场时间', dataIndex: 'exitTime', width: 170 },
    { title: '停留(分)', dataIndex: 'durationMinutes', width: 80 },
    { title: '费用(元)', dataIndex: 'totalFee', width: 100 },
    {
      title: '支付',
      dataIndex: 'paymentStatus',
      width: 80,
      render: (paymentStatus: number) => (
        <Tag color={paymentStatus === 1 ? 'success' : 'error'}>{paymentStatus === 1 ? '已付' : '未付'}</Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 0 ? 'warning' : 'default'}>{status === 0 ? '在场' : '已离场'}</Tag>
      )
    },
    {
      title: '操作',
      width: 100,
      render: (_: any, row: any) =>
        row.status === 0 ? (
          <Button size="small" type="primary" danger onClick={() => handleForceExit(row)}>
            强制离场
          </Button>
        ) : null
    }
  ]

  return (
    <div className="page-container">
      <Card
        title="车辆进出管理"
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
              { label: '在场', value: 0 },
              { label: '已离场', value: 1 }
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
