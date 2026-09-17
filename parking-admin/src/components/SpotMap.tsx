import { useCallback, useEffect, useMemo, useState } from 'react'
import { Drawer, Descriptions, Tag, Button, Empty, Popconfirm, Spin, App as AntdApp } from 'antd'
import { ReloadOutlined, CarOutlined, CalendarOutlined } from '@ant-design/icons'
import { getSpotMap, forceExit } from '@/api'

const STATUS_META: Record<number, { text: string; tag: string; color: string; bg: string }> = {
  0: { text: '空闲', tag: 'success', color: '#67C23A', bg: '#E8F5E9' },
  1: { text: '占用', tag: 'warning', color: '#E6A23C', bg: '#FEF3E2' },
  2: { text: '预约未停车', tag: 'processing', color: '#409EFF', bg: '#E3F2FD' },
  4: { text: '维修', tag: 'error', color: '#F56C6C', bg: '#FDEDED' },
  5: { text: '预约已停车', tag: 'default', color: '#909399', bg: '#F5F5F5' }
}

const LEGEND_ORDER = [0, 1, 2, 5, 4]

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const fmtMoney = (v: any) =>
  `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

/** 校园分区中文名 */
const ZONE_NAME: Record<string, string> = {
  teaching: '教学楼区',
  library: '图书馆区',
  dorm: '宿舍区',
  gym: '体育馆区',
  visitor: '访客与家长临时停车区'
}

const IDENTITY_NAME: Record<string, string> = { '1': '学生', '2': '教师', '3': '职工', '4': '访客', '5': '家长' }

/** 可停身份文案，如「学生/教师/职工」 */
function allowLabel(allow: any) {
  const codes = String(allow || '').split(',').map((s) => s.trim()).filter(Boolean)
  if (codes.length === 0) return '所有身份'
  return codes.map((c) => IDENTITY_NAME[c] || c).join('/')
}

function fmtDuration(minutes: any) {
  const m = Number(minutes)
  if (!m || m <= 0) return '不足 1 分钟'
  const d = Math.floor(m / 1440)
  const h = Math.floor((m % 1440) / 60)
  const mm = m % 60
  const parts: string[] = []
  if (d) parts.push(`${d} 天`)
  if (h) parts.push(`${h} 小时`)
  if (mm) parts.push(`${mm} 分钟`)
  return parts.join(' ')
}

interface Props {
  /** 数据变化后通知父组件刷新（例如首页统计） */
  onChanged?: () => void
}

export default function SpotMap({ onChanged }: Props) {
  const { message } = AntdApp.useApp()
  const [spots, setSpots] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedSpot, setSelectedSpot] = useState<any>(null)
  const [acting, setActing] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await getSpotMap()
      setSpots(res.data?.spots || [])
    } catch (e) {
      /* 错误提示由 request 拦截器处理 */
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const groupedSpots = useMemo(() => {
    const groups: Record<string, any[]> = {}
    spots.forEach((spot) => {
      const area = spot.area || '未分组'
      if (!groups[area]) groups[area] = []
      groups[area].push(spot)
    })
    return Object.keys(groups)
      .sort((a, b) => a.localeCompare(b, 'zh-CN'))
      .map((area) => {
        const list = groups[area].sort((a, b) => String(a.spotCode).localeCompare(String(b.spotCode)))
        const countBy: Record<number, number> = {}
        list.forEach((s) => {
          const st = Number(s.displayStatus)
          countBy[st] = (countBy[st] || 0) + 1
        })
        return { area, list, countBy, total: list.length }
      })
  }, [spots])

  const metaOf = (status: any) => STATUS_META[Number(status)] || STATUS_META[0]

  const openDetail = (spot: any) => {
    setSelectedSpot(spot)
    setDetailOpen(true)
  }

  const handleForceExit = async () => {
    const recordId = selectedSpot?.vehicle?.recordId
    if (!recordId) return
    setActing(true)
    try {
      await forceExit(recordId)
      message.success('已强制出场并结算')
      await loadData()
      setDetailOpen(false)
      onChanged?.()
    } finally {
      setActing(false)
    }
  }

  const vehicle = selectedSpot?.vehicle
  const reservation = selectedSpot?.reservation
  const selectedMeta = selectedSpot ? metaOf(selectedSpot.displayStatus) : STATUS_META[0]

  return (
    <div>
      {/* 图例 + 刷新 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 14 }}>
        <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', fontSize: 12, color: '#606266' }}>
          {LEGEND_ORDER.map((st) => {
            const m = STATUS_META[st]
            return (
              <span key={st} style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
                <span style={{ width: 10, height: 10, borderRadius: 2, backgroundColor: m.color, display: 'inline-block' }} />
                {m.text}
              </span>
            )
          })}
        </div>
        <Button size="small" icon={<ReloadOutlined />} loading={loading} onClick={loadData}>
          刷新
        </Button>
      </div>

      {groupedSpots.length === 0 ? (
        <Empty description="暂无车位数据" style={{ padding: '40px 0' }} />
      ) : (
        <div className="map-areas">
          {groupedSpots.map(({ area, list, countBy, total }) => (
            <div key={area} className="area-group">
              <div className="area-group-header">
                <span className="area-group-title">{area}</span>
                <div className="area-group-chips">
                  <span className="area-chip">
                    <span className="chip-dot" style={{ background: '#67C23A' }} />
                    空闲 {countBy[0] || 0}
                  </span>
                  <span className="area-chip">
                    <span className="chip-dot" style={{ background: '#E6A23C' }} />
                    占用 {countBy[1] || 0}
                  </span>
                  <span className="area-chip">
                    <span className="chip-dot" style={{ background: '#409EFF' }} />
                    预约 {countBy[2] || 0}
                  </span>
                  <span className="area-chip">
                    <span className="chip-dot" style={{ background: '#F56C6C' }} />
                    维修 {countBy[4] || 0}
                  </span>
                  <span className="area-chip">{total} 个车位</span>
                </div>
              </div>
              <div
                className="spots-grid"
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(84px, 1fr))',
                  gap: 8
                }}
              >
                {list.map((spot) => {
                  const m = metaOf(spot.displayStatus)
                  const plate = spot.vehicle?.plateNumber
                  return (
                    <div
                      key={spot.id}
                      className={`spot-item spot-${Number(spot.displayStatus)}`}
                      onClick={() => openDetail(spot)}
                      style={{
                        aspectRatio: '4 / 3',
                        borderRadius: 8,
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        alignItems: 'center',
                        cursor: 'pointer',
                        border: `1px solid ${m.color}33`,
                        backgroundColor: m.bg,
                        transition: 'box-shadow .2s, transform .2s',
                        padding: '2px 4px',
                        overflow: 'hidden'
                      }}
                      title={`${spot.spotCode} · ${m.text}${plate ? ' · ' + plate : ''}`}
                    >
                      <div style={{ fontSize: 12, fontWeight: 700, color: '#303133' }}>{spot.spotCode}</div>
                      {plate ? (
                        <div style={{ fontSize: 10, color: '#303133', marginTop: 2, fontWeight: 600 }}>{plate}</div>
                      ) : (
                        <div style={{ fontSize: 10, color: '#909399', marginTop: 2 }}>{spot.floor}</div>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 车位详情抽屉 */}
      <Drawer
        title={selectedSpot ? `车位详情 · ${selectedSpot.spotCode}` : '车位详情'}
        width={470}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        destroyOnClose
      >
        {!selectedSpot ? (
          <Empty />
        ) : (
          <>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: 16,
                background: '#f7f8fa',
                borderRadius: 10,
                marginBottom: 18
              }}
            >
              <div>
                <div style={{ fontSize: 20, fontWeight: 700, color: '#303133' }}>{selectedSpot.spotCode}</div>
                <div style={{ marginTop: 6, fontSize: 13, color: '#909399' }}>
                  {selectedSpot.area} · {selectedSpot.floor} · {fmtMoney(selectedSpot.pricePerHour)}/小时
                </div>
                <div style={{ marginTop: 6, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  <Tag color="blue">{ZONE_NAME[selectedSpot.zoneCode] || '未设置分区'}</Tag>
                  <Tag color="cyan">
                    可停：{allowLabel(selectedSpot.allowIdentities)}
                  </Tag>
                  {selectedSpot.freeMinutes > 0 && <Tag color="gold">区域免费 {selectedSpot.freeMinutes} 分钟</Tag>}
                </div>
              </div>
              <Tag color={selectedMeta.tag} style={{ fontSize: 14, padding: '2px 10px' }}>
                {selectedMeta.text}
              </Tag>
            </div>

            {/* 停放车辆 */}
            <div style={{ fontSize: 15, fontWeight: 700, color: '#303133', marginBottom: 10 }}>
              <CarOutlined style={{ marginRight: 6, color: '#E6A23C' }} />
              停放车辆
            </div>
            {vehicle ? (
              <>
                <div
                  style={{
                    fontSize: 24,
                    fontWeight: 700,
                    letterSpacing: 1,
                    color: '#303133',
                    textAlign: 'center',
                    padding: '12px 0',
                    background: '#FEF3E2',
                    border: '1px solid rgba(230,162,60,.25)',
                    borderRadius: 8,
                    marginBottom: 12
                  }}
                >
                  {vehicle.plateNumber || '未记录车牌'}
                </div>
                <Descriptions column={1} bordered size="small" style={{ marginBottom: 14 }}>
                  <Descriptions.Item label="入场时间">{fmtTime(vehicle.entryTime)}</Descriptions.Item>
                  <Descriptions.Item label="已停时长">{fmtDuration(vehicle.parkedMinutes)}</Descriptions.Item>
                  <Descriptions.Item label="预估费用">
                    <span style={{ color: '#F5222D', fontWeight: 600 }}>{fmtMoney(vehicle.estimatedFee)}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="车主">
                    {vehicle.nickname || '-'}
                    {vehicle.phone ? `（${vehicle.phone}）` : ''}
                  </Descriptions.Item>
                  <Descriptions.Item label="停车记录号">
                    <span style={{ fontSize: 12 }}>{vehicle.recordId}</span>
                  </Descriptions.Item>
                </Descriptions>
                <Popconfirm
                  title="强制出场"
                  description="将立即结算费用、释放车位，确定继续？"
                  onConfirm={handleForceExit}
                  okButtonProps={{ danger: true }}
                >
                  <Button danger block loading={acting}>
                    强制出场并结算
                  </Button>
                </Popconfirm>
              </>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该车位当前无车辆停放" style={{ margin: '8px 0 16px' }} />
            )}

            {/* 预约信息 */}
            <div style={{ fontSize: 15, fontWeight: 700, color: '#303133', margin: '18px 0 10px' }}>
              <CalendarOutlined style={{ marginRight: 6, color: '#409EFF' }} />
              预约信息
            </div>
            {reservation ? (
              <Descriptions column={1} bordered size="small">
                <Descriptions.Item label="预约编号">
                  <span style={{ fontSize: 12 }}>{reservation.reservationNo}</span>
                </Descriptions.Item>
                <Descriptions.Item label="预约时段">
                  {fmtTime(reservation.startTime)} ~ {fmtTime(reservation.endTime)}
                </Descriptions.Item>
                <Descriptions.Item label="预约时长">{Number(reservation.durationHours || 0)} 小时</Descriptions.Item>
                <Descriptions.Item label="预约金额">{fmtMoney(reservation.totalAmount)}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color="processing">进行中</Tag>
                </Descriptions.Item>
              </Descriptions>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该车位没有进行中的预约" />
            )}

            {loading && (
              <div style={{ textAlign: 'center', marginTop: 16 }}>
                <Spin size="small" />
              </div>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}
