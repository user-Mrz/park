import request from '../../utils/request.js'
import { buildZoneList, spotsOfZone, OTHER_ZONE } from '../../utils/zones.js'

Page({
  data: {
    spots: [],          // 可选车位（空闲车位 + 本人预约占用的车位）
    zones: [],          // 区域列表（第一步选区域）
    zoneIndex: -1,
    selectedZone: null,
    spotIndex: -1,
    selectedSpot: null,
    zoneSpots: [],      // 当前区域下的车位（第二步选车位）
    plateNumber: '',
    reservationId: null,  // 预约ID，如果有活动预约则自动关联
    activeReservation: null,
    activeSpotCode: '',
    submitting: false,
    // ---- 车位搜索 ----
    spotKeyword: '',      // 搜索关键字（车位编号/区域/分区名）
    searchMatched: 0      // 当前关键字匹配到的车位数（用于显示提示）
  },

  onLoad() {
    this.loadPageData()
  },

  // 一次性并行加载: 车位列表 + 我的预约 + 用户信息(避免竞态导致未自动选中)
  loadPageData() {
    if (!wx.getStorageSync('token')) return
    Promise.all([
      request.get('/spot/list?page=1&pageSize=500'),
      request.get('/user/reservations?page=1&pageSize=10'),
      request.get('/user/info')
    ]).then(([spotRes, resvRes, infoRes]) => {
      const user = infoRes.data || {}
      const reservations = (resvRes.data && resvRes.data.records) || []
      // 取最近一条活跃预约(status=1)
      const active = reservations.find(r => r.status === 1)

      // 仅展示: 空闲车位(0) + 本人活跃预约占用的车位(2)。
      // 他人已预约的车位不再展示, 避免选中后入场被后端拒绝("reserved by another user")。
      const spots = ((spotRes.data && spotRes.data.records) || []).filter((s) => {
        if (s.status === 0) return true
        return s.status === 2 && active && String(s.id) === String(active.spotId)
      }).map(s => ({
        ...s,
        label: `${s.spotCode} · ¥${s.pricePerHour}/h`
      }))

      // 先按区域聚合：用户先选区域，再选该区域下的车位
      const zones = buildZoneList(spots)
      // 备份原始数据用于搜索过滤
      this.allSpots = spots
      this.allZones = zones

      const baseData = {
        spots,
        zones,
        zoneIndex: -1,
        selectedZone: null,
        zoneSpots: [],
        spotIndex: -1,
        selectedSpot: null,
        plateNumber: user.plateNumber || '',
        reservationId: active ? active.id : null,
        activeReservation: active || null,
        activeSpotCode: '',
        spotKeyword: '',
        searchMatched: 0
      }

      // 若有本人活跃预约, 自动定位到该车位所在区域并选中车位
      if (active) {
        const idx = spots.findIndex((s) => String(s.id) === String(active.spotId))
        if (idx >= 0) {
          const spot = spots[idx]
          const zoneCode = spot.zoneCode || OTHER_ZONE
          const zoneIdx = zones.findIndex((z) => z.code === zoneCode)
          const zoneSpots = spotsOfZone(spots, zoneCode)
          baseData.zoneIndex = zoneIdx
          baseData.selectedZone = zoneIdx >= 0 ? zones[zoneIdx] : null
          baseData.zoneSpots = zoneSpots
          baseData.spotIndex = zoneSpots.findIndex((s) => String(s.id) === String(spot.id))
          baseData.selectedSpot = spot
          baseData.activeSpotCode = spot.spotCode
        }
      }

      this.setData(baseData)
    }).catch(() => {})
  },

  /** 搜索关键字输入：实时过滤车位池和区域列表 */
  onKeywordInput(e) {
    const kw = e.detail.value
    this.setData({ spotKeyword: kw })
    this.applyKeywordFilter()
  },

  /** 点击搜索按钮：触发一次过滤（输入框已实时联动，这里做兜底） */
  onSpotSearch() {
    this.applyKeywordFilter()
  },

  /**
   * 关键字过滤：跨区域按车位编号 / 区域名 / 分区名 模糊匹配
   * 关键字为空时恢复原始 zones/spots；非空时 zones 只显示有匹配车位的区域，
   * 并在保留已选区域（若仍命中）的同时重建 zoneSpots。
   * 注意：保留活动预约的自动选中状态不被搜索破坏。
   */
  applyKeywordFilter() {
    const kw = String(this.data.spotKeyword || '').trim().toLowerCase()
    const allSpots = this.allSpots || []
    const allZones = this.allZones || []

    if (!kw) {
      const z = this.data.selectedZone
      const zoneSpots = z ? spotsOfZone(allSpots, z.code) : []
      this.setData({ zones: allZones, spots: allSpots, zoneSpots, searchMatched: 0 })
      return
    }

    const filtered = allSpots.filter((s) => {
      const code = String(s.spotCode || '').toLowerCase()
      const area = String(s.area || '').toLowerCase()
      const zoneName = String(s.zoneName || '').toLowerCase()
      return code.indexOf(kw) >= 0 || area.indexOf(kw) >= 0 || zoneName.indexOf(kw) >= 0
    })

    const zones = buildZoneList(filtered)

    const oldCode = this.data.selectedZone ? this.data.selectedZone.code : null
    const newZone = oldCode ? zones.find((z) => z.code === oldCode) : null
    const zoneSpots = newZone ? spotsOfZone(filtered, newZone.code) : []

    this.setData({
      spots: filtered,
      zones,
      zoneSpots,
      zoneIndex: newZone ? zones.findIndex((z) => z.code === newZone.code) : -1,
      selectedZone: newZone,
      spotIndex: -1,
      selectedSpot: null,
      searchMatched: filtered.length
    })
  },

  /** 第一步：选择停车区域 */
  onZoneChange(e) {
    const idx = Number(e.detail.value)
    const zone = this.data.zones[idx]
    if (!zone) return
    this.setData({
      zoneIndex: idx,
      selectedZone: zone,
      zoneSpots: spotsOfZone(this.data.spots, zone.code),
      // 换区域后原车位失效，需重新选择
      spotIndex: -1,
      selectedSpot: null
    })
  },

  /** 第二步：在所选区域内选择车位 */
  onSpotChange(e) {
    const idx = Number(e.detail.value)
    this.setData({ spotIndex: idx, selectedSpot: this.data.zoneSpots[idx] })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) this.setData({ [field]: e.detail.value })
  },

  handleEnter() {
    if (!this.data.selectedZone) { wx.showToast({ title: '请先选择停车区域', icon: 'none' }); return }
    if (!this.data.selectedSpot) { wx.showToast({ title: '请选择车位', icon: 'none' }); return }
    if (!this.data.plateNumber) { wx.showToast({ title: '请输入车牌号', icon: 'none' }); return }
    this.setData({ submitting: true })
    const reqData = {
      spotId: this.data.selectedSpot.id,
      plateNumber: this.data.plateNumber
    }
    // Attach reservationId if user has an active reservation for this spot
    if (this.data.reservationId && this.data.selectedSpot.id === this.data.activeReservation?.spotId) {
      reqData.reservationId = this.data.reservationId
    }
    request.post('/vehicle/enter', reqData).then(() => {
      wx.showToast({ title: '入场成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => {}).finally(() => this.setData({ submitting: false }))
  }
})
