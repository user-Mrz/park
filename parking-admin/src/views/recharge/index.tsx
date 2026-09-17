import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getRechargeRecords } from '@/api'

const STATUS_TAG: Record<number, { color: string; text: string }> = {
  0: { color: 'warning', text: '处理中' },
  1: { color: 'success', text: '成功' },
  2: { color: 'error', text: '失败' }
}

export default function Recharges() {
  const [loading, setLoading] = useState(false)
  const [tableData, setTableData] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getRechargeRecords({ page, pageSize })
      setTableData(res.data.records)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    loadData()
  }, [loadData])

  const columns: ColumnsType<any> = [
    { title: 'ID', dataIndex: 'id', width: 100 },
    { title: '用户ID', dataIndex: 'userId', width: 100 },
    { title: '订单号', dataIndex: 'orderNo', width: 180 },
    { title: '金额(元)', dataIndex: 'amount', width: 100 },
    { title: '支付方式', dataIndex: 'payMethod', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: number) => {
        const tag = STATUS_TAG[status] || { color: 'default', text: '未知' }
        return <Tag color={tag.color}>{tag.text}</Tag>
      }
    },
    { title: '充值时间', dataIndex: 'createdAt', width: 170 }
  ]

  return (
    <div className="page-container">
      <Card title="充值记录">
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
