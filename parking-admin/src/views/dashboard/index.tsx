import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Row, Col, Card, Button, Empty, Table, Tag, Space, App as AntdApp } from 'antd'
import {
  UserOutlined,
  EnvironmentOutlined,
  CarOutlined,
  CheckCircleOutlined,
  CalendarOutlined,
  MoneyCollectOutlined,
  ToolOutlined,
  FileSearchOutlined,
  DownloadOutlined
} from '@ant-design/icons'
import { getDashboard, getSpotMap, getCampusStats, exportCsv } from '@/api'
import SpotMap from '@/components/SpotMap'
import EChart from '@/components/EChart'

const fmtNum = (v: any) => Number(v || 0).toLocaleString('zh-CN')
const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

interface StatCardDef {
  label: string
  value: string
  icon: ReactNode
  color: string
  bg: string
}

export default function Dashboard() {
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const [dash, setDash] = useState<any>({})
  const [mapData, setMapData] = useState<any>({ spots: [], stats: {} })
  const [campus, setCampus] = useState<any>({ totals: {}, byIdentity: [], byDepartment: [], byZone: [] })
  const [error, setError] = useState(false)
  const [exporting, setExporting] = useState('')

  const loadAll = useCallback(async () => {
    try {
      const [d, m, c]: any[] = await Promise.all([getDashboard(), getSpotMap(), getCampusStats()])
      setDash(d.data)
      setMapData({ spots: m.data?.spots || [], stats: m.data || {} })
      setCampus({
        totals: c.data?.totals || {},
        byIdentity: c.data?.byIdentity || [],
        byDepartment: c.data?.byDepartment || [],
        byZone: c.data?.byZone || []
      })
      setError(false)
    } catch (e) {
      setError(true)
    }
  }, [])

  useEffect(() => {
    loadAll()
  }, [loadAll])

  const doExport = async (type: string, label: string) => {
    setExporting(type)
    try {
      await exportCsv(type)
      message.success(`${label}报表已开始下载`)
    } catch (e: any) {
      message.error(e?.message || '导出失败')
    } finally {
      setExporting('')
    }
  }

  // 校园身份分布（环形图）
  const identityOption = useMemo<any>(() => {
    const colors: Record<number, string> = { 1: '#409EFF', 2: '#52C41A', 3: '#722ED1', 4: '#909399', 5: '#13C2C2' }
    const data = (campus.byIdentity || []).map((r: any) => ({
      name: r.identityLabel,
      value: Number(r.userCount) || 0,
      itemStyle: { color: colors[r.identityType] || '#909399' }
    }))
    const total = data.reduce((s: number, d: any) => s + d.value, 0)
    if (total === 0) {
      return { title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { fontSize: 14, color: '#c0c4cc', fontWeight: 400 } } }
    }
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12, color: '#606266' } },
      title: { text: String(total), subtext: '校园用户', left: 'center', top: '30%',
        textStyle: { fontSize: 22, fontWeight: 700, color: '#303133' }, subtextStyle: { fontSize: 12, color: '#909399' } },
      series: [{
        type: 'pie', radius: ['58%', '76%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: false }, data
      }]
    }
  }, [campus])

  // 各分区车位使用情况（堆叠柱状图）
  const zoneOption = useMemo<any>(() => {
    const zones = campus.byZone || []
    if (zones.length === 0) {
      return { title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { fontSize: 14, color: '#c0c4cc', fontWeight: 400 } } }
    }
    const names = zones.map((z: any) => z.zoneName)
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
      grid: { left: 10, right: 16, top: 20, bottom: 46, containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { fontSize: 11, interval: 0, width: 70, overflow: 'truncate' } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '空闲', type: 'bar', stack: 'total', itemStyle: { color: '#67C23A' }, data: zones.map((z: any) => z.vacantCount) },
        { name: '占用', type: 'bar', stack: 'total', itemStyle: { color: '#E6A23C' }, data: zones.map((z: any) => z.occupiedCount) },
        { name: '预约中', type: 'bar', stack: 'total', itemStyle: { color: '#409EFF' }, data: zones.map((z: any) => z.reservedCount) },
        { name: '维护', type: 'bar', stack: 'total', itemStyle: { color: '#F56C6C' }, data: zones.map((z: any) => z.maintenanceCount) }
      ]
    }
  }, [campus])

  const statCards: StatCardDef[] = [
    { label: '总用户数', value: fmtNum(dash.totalUsers), color: '#409EFF', bg: 'rgba(64,158,255,.1)', icon: <UserOutlined /> },
    { label: '总车位数', value: fmtNum(dash.totalSpots), color: '#722ED1', bg: 'rgba(114,46,209,.1)', icon: <EnvironmentOutlined /> },
    { label: '可用车位', value: fmtNum(dash.availableSpots), color: '#52C41A', bg: 'rgba(82,196,26,.1)', icon: <CheckCircleOutlined /> },
    { label: '在场车辆', value: fmtNum(dash.parkedVehicles), color: '#FA8C16', bg: 'rgba(250,140,22,.1)', icon: <CarOutlined /> },
    { label: '今日预约', value: fmtNum(dash.todayReservations), color: '#13C2C2', bg: 'rgba(19,194,194,.1)', icon: <CalendarOutlined /> },
    { label: '今日收入', value: fmtMoney(dash.todayRevenue), color: '#F5222D', bg: 'rgba(245,34,45,.08)', icon: <MoneyCollectOutlined /> }
  ]

  // 车位状态分布（环形图）
  const donutOption = useMemo<any>(() => {
    const s = mapData.stats || {}
    const total = Number(s.totalCount) || 0
    const data = [
      { value: Number(s.vacantCount) || 0, name: '空闲', itemStyle: { color: '#67C23A' } },
      { value: Number(s.occupiedCount) || 0, name: '占用', itemStyle: { color: '#E6A23C' } },
      { value: Number(s.reservedUnparkedCount) || 0, name: '预约未停车', itemStyle: { color: '#409EFF' } },
      { value: Number(s.reservedParkedCount) || 0, name: '预约已停车', itemStyle: { color: '#909399' } },
      { value: Number(s.maintenanceCount) || 0, name: '维修', itemStyle: { color: '#F56C6C' } }
    ].filter((d) => d.value > 0)
    if (total === 0) {
      return {
        title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { fontSize: 14, color: '#c0c4cc', fontWeight: 400 } }
      }
    }
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c} 个 ({d}%)' },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12, color: '#606266' } },
      title: {
        text: String(total),
        subtext: '车位总数',
        left: 'center',
        top: '31%',
        textStyle: { fontSize: 24, fontWeight: 700, color: '#303133' },
        subtextStyle: { fontSize: 12, color: '#909399' }
      },
      series: [
        {
          type: 'pie',
          radius: ['58%', '76%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 600 }, itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.12)' } },
          data
        }
      ]
    }
  }, [mapData])

  // 区域使用率
  const areaUsage = useMemo(() => {
    const groups: Record<string, any[]> = {}
    ;(mapData.spots || []).forEach((spot: any) => {
      const area = spot.area || '未分组'
      if (!groups[area]) groups[area] = []
      groups[area].push(spot)
    })
    return Object.keys(groups)
      .sort((a, b) => a.localeCompare(b, 'zh-CN'))
      .map((area) => {
        const list = groups[area]
        const total = list.length
        const vacant = list.filter((s: any) => Number(s.displayStatus) === 0).length
        const maintain = list.filter((s: any) => Number(s.displayStatus) === 4).length
        const busy = total - vacant - maintain
        const rate = total === 0 ? 0 : Math.round((busy / total) * 100)
        return { area, total, busy, vacant, maintain, rate }
      })
      .sort((a, b) => b.rate - a.rate)
  }, [mapData])

  const deptColumns = [
    { title: '院系 / 部门', dataIndex: 'department', ellipsis: true },
    { title: '用户数', dataIndex: 'userCount', width: 90, align: 'right' as const },
    { title: '已认证', dataIndex: 'verifiedCount', width: 90, align: 'right' as const },
    {
      title: '待审核', dataIndex: 'pendingCount', width: 90, align: 'right' as const,
      render: (v: number) => (v > 0 ? <Tag color="processing">{v}</Tag> : v)
    },
    { title: '预约次数', dataIndex: 'reservationCount', width: 100, align: 'right' as const },
    { title: '停车次数', dataIndex: 'vehicleRecordCount', width: 100, align: 'right' as const },
    {
      title: '消费金额', dataIndex: 'revenue', width: 130, align: 'right' as const,
      render: (v: any) => fmtMoney(v)
    }
  ]

  const zoneColumns = [
    { title: '校园分区', dataIndex: 'zoneName' },
    { title: '车位', dataIndex: 'spotCount', width: 70, align: 'right' as const },
    { title: '空闲', dataIndex: 'vacantCount', width: 70, align: 'right' as const },
    { title: '占用', dataIndex: 'occupiedCount', width: 70, align: 'right' as const },
    {
      title: '收入', dataIndex: 'revenue', width: 120, align: 'right' as const,
      render: (v: any) => fmtMoney(v)
    }
  ]

  const totalItems = [
    { label: '已认证用户', value: fmtNum(campus.totals.verifiedUsers), color: '#52C41A' },
    { label: '待审核身份', value: fmtNum(campus.totals.pendingUsers), color: '#E6A23C', to: '/users?identityStatus=0' },
    { label: '待审访客报备', value: fmtNum(campus.totals.pendingVisitReports), color: '#409EFF', to: '/visits' },
    { label: '待处理违规', value: fmtNum(campus.totals.pendingViolations), color: '#F56C6C', to: '/violations' },
    { label: '黑名单车辆', value: fmtNum(campus.totals.blacklist), color: '#722ED1', to: '/blacklist' },
    { label: '累计营收', value: fmtMoney(campus.totals.revenue), color: '#13C2C2' }
  ]

  return (
    <div>
      {/* 1. 顶部统计卡片 */}
      <Row gutter={[16, 16]}>
        {statCards.map((card) => (
          <Col xs={12} sm={8} xl={4} key={card.label}>
            <Card className="stat-card stat-card-hover" bordered={false}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ color: '#909399', fontSize: 13, marginBottom: 6, whiteSpace: 'nowrap' }}>{card.label}</div>
                  <div
                    style={{
                      fontSize: card.label === '今日收入' ? 20 : 24,
                      fontWeight: 700,
                      color: '#303133',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}
                  >
                    {card.value}
                  </div>
                </div>
                <div className="dash-icon-badge" style={{ backgroundColor: card.bg }}>
                  <span style={{ fontSize: 26, color: card.color }}>{card.icon}</span>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 2. 中排：今日概览 / 车位分布 / 区域使用率 */}
      <Row gutter={[16, 16]} align="stretch" style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card className="mid-card" title="今日概览" bordered={false}>
            <div className="metric-grid">
              <div className="metric-item">
                <div className="metric-label">今日收入</div>
                <div className="metric-value primary">{fmtMoney(dash.todayRevenue)}</div>
              </div>
              <div className="metric-item">
                <div className="metric-label">今日预约</div>
                <div className="metric-value">{fmtNum(dash.todayReservations)} 笔</div>
              </div>
              <div className="metric-item">
                <div className="metric-label">在场车辆</div>
                <div className="metric-value">{fmtNum(dash.parkedVehicles)} 辆</div>
              </div>
              <div className="metric-item">
                <div className="metric-label">可用车位</div>
                <div className="metric-value">{fmtNum(dash.availableSpots)} 个</div>
                <div className="metric-extra">总车位 {fmtNum(dash.totalSpots)} 个</div>
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card className="mid-card" title="车位状态分布" bordered={false}>
            {error ? <Empty style={{ marginTop: 40 }} description="加载失败" /> : <EChart option={donutOption} style={{ height: 280 }} />}
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card className="mid-card" title="区域使用率" bordered={false}>
            {areaUsage.length === 0 ? (
              <Empty style={{ marginTop: 30 }} description="暂无车位数据" />
            ) : (
              <>
                {areaUsage.map((u) => (
                  <div className="usage-area" key={u.area}>
                    <div className="usage-head">
                      <span className="usage-name">{u.area}</span>
                      <span className="usage-pct">
                        已用 {u.busy}/{u.total} · 使用率 {u.rate}%
                      </span>
                    </div>
                    <div className="usage-track">
                      <div className="usage-seg busy" style={{ width: `${u.rate}%` }} />
                      <div className="usage-seg maintain" style={{ width: `${u.total === 0 ? 0 : (u.maintain / u.total) * 100}%` }} />
                      <div className="usage-seg free" style={{ flex: 1 }} />
                    </div>
                  </div>
                ))}
                <div className="usage-note">
                  <span><span className="dot" style={{ background: '#E6A23C' }} />已用</span>
                  <span><span className="dot" style={{ background: '#F56C6C' }} />维修</span>
                  <span><span className="dot" style={{ background: '#67C23A' }} />空闲</span>
                </div>
              </>
            )}
          </Card>
        </Col>
      </Row>

      {/* 3. 校园统计 */}
      <Row gutter={[16, 16]} align="stretch" style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card className="mid-card" title="校园身份分布" bordered={false}>
            <EChart option={identityOption} style={{ height: 260 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="mid-card" title="校园分区车位使用" bordered={false}>
            <EChart option={zoneOption} style={{ height: 260 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="mid-card" title="校园管理概览" bordered={false}>
            <div className="metric-grid">
              {totalItems.map((t: any) => (
                <div
                  className="metric-item"
                  key={t.label}
                  onClick={() => t.to && navigate(t.to)}
                  style={{ cursor: t.to ? 'pointer' : 'default' }}
                  title={t.to ? '点击查看详情' : undefined}
                >
                  <div className="metric-label">
                    {t.label}
                    {t.to && <span style={{ color: '#c0c4cc', marginLeft: 4 }}>›</span>}
                  </div>
                  <div className="metric-value" style={{ color: t.color, fontSize: 18 }}>{t.value}</div>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} xl={15}>
          <Card
            className="mid-card"
            title="院系统计（用户 / 认证 / 预约 / 停车 / 消费）"
            bordered={false}
            extra={
              <Space wrap>
                <Button size="small" icon={<DownloadOutlined />} loading={exporting === 'users'}
                  onClick={() => doExport('users', '用户')}>导出用户</Button>
                <Button size="small" icon={<DownloadOutlined />} loading={exporting === 'reservations'}
                  onClick={() => doExport('reservations', '预约')}>导出预约</Button>
                <Button size="small" icon={<DownloadOutlined />} loading={exporting === 'payments'}
                  onClick={() => doExport('payments', '扣费')}>导出扣费</Button>
                <Button size="small" icon={<DownloadOutlined />} loading={exporting === 'violations'}
                  onClick={() => doExport('violations', '违规')}>导出违规</Button>
                <Button size="small" icon={<DownloadOutlined />} loading={exporting === 'visits'}
                  onClick={() => doExport('visits', '访客报备')}>导出报备</Button>
              </Space>
            }
          >
            {campus.byDepartment.length === 0 ? (
              <Empty description="暂无院系数据" />
            ) : (
              <Table
                size="small"
                rowKey="department"
                columns={deptColumns}
                dataSource={campus.byDepartment}
                pagination={{ pageSize: 6, size: 'small', showTotal: (t) => `共 ${t} 个院系` }}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Card className="mid-card" title="分区车位与收入" bordered={false}>
            <Table
              size="small"
              rowKey="zoneCode"
              columns={zoneColumns}
              dataSource={campus.byZone}
              pagination={false}
            />
          </Card>
        </Col>
      </Row>

      {/* 4. 底部快捷入口 + 车位状态地图 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card bordered={false} className="stat-card" style={{ paddingBottom: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 16, fontWeight: 700, color: '#303133' }}>车位状态地图</span>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                <Button type="primary" ghost icon={<EnvironmentOutlined />} onClick={() => navigate('/spots')}>管理车位</Button>
                <Button style={{ color: '#67C23A', borderColor: '#67C23A' }} icon={<CalendarOutlined />} onClick={() => navigate('/reservations')}>查看预约</Button>
                <Button style={{ color: '#E6A23C', borderColor: '#E6A23C' }} icon={<CarOutlined />} onClick={() => navigate('/vehicles')}>车辆进出</Button>
                <Button style={{ color: '#909399', borderColor: '#909399' }} icon={<ToolOutlined />} onClick={() => navigate('/config')}>系统配置</Button>
                <Button style={{ color: '#409EFF', borderColor: '#409EFF' }} icon={<FileSearchOutlined />} onClick={() => navigate('/users')}>用户管理</Button>
              </div>
            </div>
            <div style={{ marginTop: 16 }}>
              <SpotMap onChanged={loadAll} />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
