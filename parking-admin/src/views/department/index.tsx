import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Input, InputNumber, Modal, Form, Space, Popconfirm, App as AntdApp } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import { getDepartments, addDepartment, updateDepartment, updateDepartmentStatus, deleteDepartment } from '@/api'

interface DeptForm {
  name: string
  code: string
  contact: string
  phone: string
  remark: string
  sortOrder: number
}

export default function Departments() {
  const { message } = AntdApp.useApp()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [rows, setRows] = useState<any[]>([])
  const [open, setOpen] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [editId, setEditId] = useState<number>(0)
  const [keyword, setKeyword] = useState('')
  const [form] = Form.useForm<DeptForm>()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getDepartments()
      setRows(res.data || [])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const openAdd = () => {
    setIsEdit(false)
    setEditId(0)
    form.setFieldsValue({ name: '', code: '', contact: '', phone: '', remark: '', sortOrder: 99 })
    setOpen(true)
  }

  const openEdit = (row: any) => {
    setIsEdit(true)
    setEditId(row.id)
    form.setFieldsValue({
      name: row.name,
      code: row.code || '',
      contact: row.contact || '',
      phone: row.phone || '',
      remark: row.remark || '',
      sortOrder: row.sortOrder ?? 99
    })
    setOpen(true)
  }

  const save = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      if (isEdit) {
        await updateDepartment(editId, values)
        message.success('已保存（该学院下用户的院系名称已同步更新）')
      } else {
        await addDepartment(values)
        message.success('已新增学院/部门')
      }
      setOpen(false)
      load()
    } finally {
      setSaving(false)
    }
  }

  const toggle = async (row: any) => {
    await updateDepartmentStatus(row.id, row.status === 1 ? 0 : 1)
    message.success(row.status === 1 ? '已停用（注册与资料页将不再显示）' : '已启用')
    load()
  }

  const remove = async (row: any) => {
    await deleteDepartment(row.id)
    message.success('已删除')
    load()
  }

  const filtered = rows.filter((r) => !keyword || String(r.name).includes(keyword) || String(r.code || '').includes(keyword.toUpperCase()))

  const columns: ColumnsType<any> = [
    {
      title: '学院 / 部门',
      dataIndex: 'name',
      render: (v: string, row: any) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 600 }}>{v}</span>
          {row.remark && <span style={{ fontSize: 12, color: '#909399' }}>{row.remark}</span>}
        </Space>
      )
    },
    { title: '学院代码', dataIndex: 'code', width: 110, render: (v: any) => v || '—' },
    { title: '联系人', dataIndex: 'contact', width: 120, render: (v: any) => v || '—' },
    { title: '联系电话', dataIndex: 'phone', width: 150, render: (v: any) => v || '—' },
    { title: '排序', dataIndex: 'sortOrder', width: 80, align: 'center' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      align: 'center',
      render: (v: number) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>)
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      align: 'center',
      render: (_: any, row: any) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Popconfirm title={row.status === 1 ? '停用后注册与资料页将不再显示该学院，确定？' : '确定启用该学院？'} onConfirm={() => toggle(row)}>
            <Button type="link" size="small">{row.status === 1 ? '停用' : '启用'}</Button>
          </Popconfirm>
          <Popconfirm title="确定删除该学院？若仍有用户属于该学院将无法删除" onConfirm={() => remove(row)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div>
      <Card
        title="学院 / 部门管理"
        extra={
          <Space>
            <Input.Search
              placeholder="搜索学院名称或代码"
              allowClear
              style={{ width: 220 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <Button onClick={load}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>新增学院</Button>
          </Space>
        }
      >
        <div style={{ fontSize: 12, color: '#909399', marginBottom: 12 }}>
          学生、教师、职工在注册与账号信息页只能从**启用**的学院/部门中选择；停用后不再出现在下拉列表，已有用户不受影响。
        </div>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filtered}
          loading={loading}
          size="middle"
          pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 个学院/部门` }}
        />
      </Card>

      <Modal
        title={isEdit ? '编辑学院 / 部门' : '新增学院 / 部门'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={save}
        confirmLoading={saving}
        width={520}
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="name" label="学院/部门" rules={[{ required: true, message: '请输入学院/部门名称' }]}>
            <Input placeholder="如 计算机与信息工程学院" />
          </Form.Item>
          <Form.Item name="code" label="学院代码">
            <Input placeholder="如 CS（选填）" />
          </Form.Item>
          <Form.Item name="contact" label="联系人">
            <Input placeholder="如 王辅导员（选填）" />
          </Form.Item>
          <Form.Item name="phone" label="联系电话">
            <Input placeholder="如 0731-88880001（选填）" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序号" extra={<span style={{ fontSize: 12, color: '#909399' }}>数字越小越靠前</span>}>
            <InputNumber min={0} max={999} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="备注信息（选填）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
