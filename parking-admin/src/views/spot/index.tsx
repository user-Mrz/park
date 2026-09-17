import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Modal, Form, Input, InputNumber, Select, Space, App as AntdApp } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getSpots, addSpot, updateSpot, updateSpotStatus } from '@/api'

interface SpotForm {
  spotCode: string
  area: string
  zoneCode: string
  allowIdentities: string[]
  freeMinutes: number
  floor: string
  pricePerHour: number
  remark: string
}

/** 校园分区（value = zone_code，label = 中文地区名称） */
const ZONE_OPTIONS = [
  { value: 'teaching', label: '教学楼区' },
  { value: 'library', label: '图书馆区' },
  { value: 'dorm', label: '宿舍区' },
  { value: 'gym', label: '体育馆区' },
  { value: 'visitor', label: '访客与家长临时停车区' }
]

const ZONE_NAME: Record<string, string> = Object.fromEntries(ZONE_OPTIONS.map((z) => [z.value, z.label]))

/** 可停放身份（value = identity_type） */
const IDENTITY_OPTIONS = [
  { value: '1', label: '学生' },
  { value: '2', label: '教师' },
  { value: '3', label: '职工' },
  { value: '4', label: '访客' },
  { value: '5', label: '家长' }
]

const identityLabel = (code: string) => IDENTITY_OPTIONS.find((i) => i.value === code)?.label || code

const STATUS_TAG: Record<number, { color: string; text: string }> = {
  0: { color: 'success', text: '空闲' },
  1: { color: 'warning', text: '占用' },
  2: { color: 'processing', text: '预约' },
  3: { color: 'error', text: '维护' }
}

export default function Spots() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [tableData, setTableData] = useState<any[]>([])
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(50)
  const [total, setTotal] = useState(0)
  const [dialogVisible, setDialogVisible] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [editId, setEditId] = useState<number>(0)
  const [form] = Form.useForm<SpotForm>()

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getSpots({ page, pageSize })
      setTableData(res.data.records)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    loadData()
  }, [loadData])

  const openAddDialog = () => {
    setIsEdit(false)
    setEditId(0)
    form.setFieldsValue({
      spotCode: '',
      area: '',
      zoneCode: 'teaching',
      allowIdentities: ['1', '2', '3'],
      freeMinutes: 0,
      floor: '',
      pricePerHour: 5,
      remark: ''
    })
    setDialogVisible(true)
  }

  const openEditDialog = (row: any) => {
    setIsEdit(true)
    setEditId(row.id)
    form.setFieldsValue({
      spotCode: row.spotCode,
      area: row.area,
      zoneCode: row.zoneCode || 'teaching',
      allowIdentities: String(row.allowIdentities || '1,2,3').split(',').map((s: string) => s.trim()).filter(Boolean),
      freeMinutes: row.freeMinutes ?? 0,
      floor: row.floor,
      pricePerHour: row.pricePerHour,
      remark: row.remark
    })
    setDialogVisible(true)
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      // 可停身份数组 -> 逗号分隔字符串（后端按身份类型判断可停区域）
      const payload = {
        ...values,
        allowIdentities: (values.allowIdentities || []).join(',')
      }
      if (isEdit) {
        await updateSpot(editId, payload)
      } else {
        await addSpot(payload)
      }
      message.success('保存成功')
      setDialogVisible(false)
      loadData()
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (row: any) => {
    const newStatus = row.status === 3 ? 0 : 3
    await updateSpotStatus(row.id, newStatus)
    message.success('操作成功')
    loadData()
  }

  const columns: ColumnsType<any> = [
    { title: '车位编号', dataIndex: 'spotCode', width: 110 },
    {
      title: '校园分区',
      dataIndex: 'zoneCode',
      width: 170,
      render: (v: string, row: any) => (
        <Space direction="vertical" size={0}>
          <Tag color="blue">{ZONE_NAME[v] || row.area || '未设置'}</Tag>
          <span style={{ fontSize: 12, color: '#909399' }}>{row.area || ''}</span>
        </Space>
      )
    },
    {
      title: '可停身份',
      dataIndex: 'allowIdentities',
      width: 170,
      render: (v: string) => {
        const codes = String(v || '').split(',').map((s) => s.trim()).filter(Boolean)
        if (codes.length === 0) return <Tag>所有身份</Tag>
        return <>{codes.map((c) => <Tag key={c} color="cyan">{identityLabel(c)}</Tag>)}</>
      }
    },
    {
      title: '区域免费时长',
      dataIndex: 'freeMinutes',
      width: 110,
      align: 'right',
      render: (v: number) => (v && v > 0 ? `${v} 分钟` : '按身份规则')
    },
    { title: '楼层', dataIndex: 'floor', width: 80 },
    { title: '单价(元/小时)', dataIndex: 'pricePerHour', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: number) => {
        const tag = STATUS_TAG[status] || { color: 'default', text: '未知' }
        return <Tag color={tag.color}>{tag.text}</Tag>
      }
    },
    { title: '备注', dataIndex: 'remark' },
    {
      title: '操作',
      width: 240,
      fixed: 'right',
      render: (_: any, row: any) => (
        <>
          <Button size="small" onClick={() => openEditDialog(row)}>
            编辑
          </Button>
          <Button
            size="small"
            type={row.status === 3 ? 'primary' : 'default'}
            style={
              row.status !== 3
                ? { marginLeft: 8, color: '#E6A23C', borderColor: '#E6A23C' }
                : { marginLeft: 8 }
            }
            onClick={() => toggleStatus(row)}
          >
            {row.status === 3 ? '恢复' : '维护'}
          </Button>
        </>
      )
    }
  ]

  return (
    <div className="page-container">
      <Card
        title="车位管理"
        extra={
          <Button type="primary" onClick={openAddDialog}>
            新增车位
          </Button>
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

      <Modal
        title={isEdit ? '编辑车位' : '新增车位'}
        open={dialogVisible}
        onCancel={() => setDialogVisible(false)}
        width={500}
        footer={[
          <Button key="cancel" onClick={() => setDialogVisible(false)}>
            取消
          </Button>,
          <Button key="save" type="primary" loading={saving} onClick={handleSave}>
            保存
          </Button>
        ]}
      >
        <Form form={form} labelCol={{ span: 7 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="spotCode" label="车位编号" rules={[{ required: true, message: '请输入车位编号' }]}>
            <Input placeholder="如 A-001" />
          </Form.Item>
          <Form.Item name="zoneCode" label="校园分区" rules={[{ required: true, message: '请选择校园分区' }]}>
            <Select options={ZONE_OPTIONS} placeholder="选择校园分区" />
          </Form.Item>
          <Form.Item name="area" label="区域名称" rules={[{ required: true, message: '请输入区域名称' }]}>
            <Input placeholder="如 教学楼区" />
          </Form.Item>
          <Form.Item
            name="allowIdentities"
            label="可停身份"
            rules={[{ required: true, message: '请选择可停放的身份' }]}
            extra={<span style={{ fontSize: 12, color: '#909399' }}>预约与入场都会按此限制校验</span>}
          >
            <Select mode="multiple" options={IDENTITY_OPTIONS} placeholder="可停放的身份（可多选）" />
          </Form.Item>
          <Form.Item
            name="freeMinutes"
            label="区域免费时长"
            extra={<span style={{ fontSize: 12, color: '#909399' }}>0 表示按身份规则（教师/学生/家长等各自免费时长）</span>}
          >
            <InputNumber min={0} max={1440} precision={0} style={{ width: '100%' }} placeholder="分钟，0=按身份规则" />
          </Form.Item>
          <Form.Item name="floor" label="楼层" rules={[{ required: true, message: '请输入楼层' }]}>
            <Input placeholder="如 B1" />
          </Form.Item>
          <Form.Item name="pricePerHour" label="单价(元/时)" rules={[{ required: true, message: '请输入单价' }]}>
            <InputNumber min={0} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="备注信息" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
