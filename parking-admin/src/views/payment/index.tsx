import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getPaymentRecords } from '@/api'

export default function Payments() {
  const [loading, setLoading] = useState(false)
  const [tableData, setTableData] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getPaymentRecords({ page, pageSize })
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
    { title: '停车记录ID', dataIndex: 'vehicleRecordId', width: 120 },
    { title: '扣费(元)', dataIndex: 'amount', width: 100 },
    { title: '扣前余额', dataIndex: 'balanceBefore', width: 100 },
    { title: '扣后余额', dataIndex: 'balanceAfter', width: 100 },
    {
      title: '类型',
      dataIndex: 'type',
      width: 100,
      render: (type: string) => (
        <Tag color={type === 'auto_deduct' ? 'default' : 'warning'}>
          {type === 'auto_deduct' ? '自动扣费' : '手动操作'}
        </Tag>
      )
    },
    { title: '扣费时间', dataIndex: 'createdAt', width: 170 }
  ]

  return (
    <div className="page-container">
      <Card title="扣费记录">
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
