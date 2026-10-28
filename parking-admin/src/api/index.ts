import request from '@/utils/request'

// Dashboard
export function getDashboard() { return request.get('/admin/dashboard') }

// Users
export function getUsers(params: any) { return request.get('/admin/users', { params }) }
export function getUserDetail(id: number) { return request.get(`/admin/user/${id}`) }
export function updateUserStatus(id: number, status: number) { return request.put(`/admin/user/${id}/status?status=${status}`) }
/** 校园身份审核：identityStatus 1通过 2驳回 0待审核 */
export function auditUserIdentity(id: number, identityStatus: number, remark?: string) {
  const q = new URLSearchParams({ identityStatus: String(identityStatus) })
  if (remark) q.set('remark', remark)
  return request.put(`/admin/user/${id}/identity?${q.toString()}`)
}

// Campus departments（学院/部门管理）
export function getDepartments() { return request.get('/admin/departments') }
export function addDepartment(data: any) { return request.post('/admin/department', data) }
export function updateDepartment(id: number, data: any) { return request.put(`/admin/department/${id}`, data) }
export function updateDepartmentStatus(id: number, status: number) {
  return request.put(`/admin/department/${id}/status?status=${status}`)
}
export function deleteDepartment(id: number) { return request.delete(`/admin/department/${id}`) }
/** 公开接口：注册页与账号信息页的学院下拉数据 */
export function getPublicDepartments() { return request.get('/departments') }

// Visitor / Parent Reports
export function getVisitReports(params: any) { return request.get('/admin/visit-reports', { params }) }
export function getVisitReportStats() { return request.get('/admin/visit-reports/stats') }
export function approveVisitReport(id: number, spotId?: number, remark?: string) {
  const q = new URLSearchParams()
  if (spotId) q.set('spotId', String(spotId))
  if (remark) q.set('remark', remark)
  return request.put(`/admin/visit-report/${id}/approve?${q.toString()}`)
}
export function rejectVisitReport(id: number, remark?: string) {
  const q = new URLSearchParams()
  if (remark) q.set('remark', remark)
  return request.put(`/admin/visit-report/${id}/reject?${q.toString()}`)
}
export function checkInVisit(id: number) { return request.post(`/admin/visit-report/${id}/check-in`) }
export function checkOutVisit(id: number) { return request.post(`/admin/visit-report/${id}/check-out`) }

// Violations
export function getViolations(params: any) { return request.get('/admin/violations', { params }) }
export function getViolationStats() { return request.get('/admin/violations/stats') }
export function handleViolation(id: number, remark?: string) {
  const q = new URLSearchParams()
  if (remark) q.set('remark', remark)
  return request.put(`/admin/violation/${id}/handle?${q.toString()}`)
}

// Campus statistics & blacklist / whitelist
export function getCampusStats() { return request.get('/admin/campus-stats') }
/** 区间日统计：按自然日返回收入与入场车次（startDate / endDate 为 yyyy-MM-dd） */
export function getCampusDailyStats(params: { startDate: string; endDate: string }) {
  return request.get('/admin/campus-stats/daily', { params })
}
export function getBlacklist(params: any) { return request.get('/admin/blacklist', { params }) }
export function addBlacklist(data: any) { return request.post('/admin/blacklist', data) }
export function removeBlacklist(id: number) { return request.delete(`/admin/blacklist/${id}`) }
export function getWhitelist(params: any) { return request.get('/admin/whitelist', { params }) }
export function addWhitelist(data: any) { return request.post('/admin/whitelist', data) }
export function removeWhitelist(id: number) { return request.delete(`/admin/whitelist/${id}`) }

/**
 * 导出 CSV 报表（users / reservations / payments / violations / visits）
 * 通过原生 axios 获取 blob，避免响应拦截器把 CSV 当成 JSON 解析
 */
export async function exportCsv(type: string) {
  const token = localStorage.getItem('admin_token')
  const resp = await fetch(`/api/admin/export/${type}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!resp.ok) throw new Error(`导出失败（HTTP ${resp.status}）`)
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const stamp = new Date().toISOString().slice(0, 10)
  a.href = url
  a.download = `${type}_${stamp}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// Reservations
export function getReservations(params: any) { return request.get('/admin/reservations', { params }) }
export function updateReservationStatus(id: number, status: number) { return request.put(`/admin/reservation/${id}/status?status=${status}`) }

// Vehicle Records
export function getVehicleRecords(params: any) { return request.get('/admin/vehicle-records', { params }) }
export function updateVehicleRecord(id: number, data: any) { return request.put(`/admin/vehicle-record/${id}`, data) }
export function forceExit(id: number) { return request.post(`/admin/vehicle-record/${id}/force-exit`) }

// Spots
export function getSpots(params: any) { return request.get('/admin/spots', { params }) }
export function addSpot(data: any) { return request.post('/admin/spot', data) }
export function updateSpot(id: number, data: any) { return request.put(`/admin/spot/${id}`, data) }
export function updateSpotStatus(id: number, status: number) { return request.put(`/admin/spot/${id}/status?status=${status}`) }
export function deleteSpot(id: number) { return request.delete(`/admin/spot/${id}`) }
export function getSpotMap() { return request.get('/admin/spot-map') }

// 停车区域管理（归属「车位管理」菜单）
/** 区域列表：status 1启用 0停用，不传为全部；返回含各区域车位数 */
export function getParkingZones(params?: any) { return request.get('/admin/zones', { params }) }
export function addParkingZone(data: any) { return request.post('/admin/zone', data) }
export function updateParkingZone(id: string, data: any) { return request.put(`/admin/zone/${id}`, data) }
export function updateParkingZoneStatus(id: string, status: number) {
  return request.put(`/admin/zone/${id}/status?status=${status}`)
}
export function deleteParkingZone(id: string) { return request.delete(`/admin/zone/${id}`) }

// Server hardware monitor (参考若依服务器监控)
export function getServerMonitor() { return request.get('/admin/server-monitor') }

// Recharge & Payment Records
export function getRechargeRecords(params: any) { return request.get('/admin/recharge-records', { params }) }
export function getPaymentRecords(params: any) { return request.get('/admin/payment-records', { params }) }

// Monthly Pass（月卡 / 长租）
/** 月卡列表：status 1生效 2已过期 3已退款；passType 1月卡 2季卡 3年卡 */
export function getMonthlyPasses(params: any) { return request.get('/admin/monthly-passes', { params }) }
/** 月卡经营概览：生效中 / 7 天内到期 / 已过期 / 累计与本月长租收入 */
export function getMonthlyPassStats() { return request.get('/admin/monthly-passes/stats') }

// System Config
export function getConfig() { return request.get('/admin/config') }
export function updateConfig(data: any) { return request.put('/admin/config', data) }

// Tidal spots（潮汐车位 / 临时车位批次）
/** 潮汐车位批次（后端响应字段，zoneCode 固定为 'tidal'） */
export interface TidalBatch {
  id: string
  /** 批次名称，如「2026 春季开学临时停车区」 */
  batchName: string
  /** 显示区域名，如「东门广场临时停车区」 */
  area: string
  /** 固定为 'tidal' */
  zoneCode: string
  /** 车位编号前缀，如 'TD'（最终编号 TD-001…TD-0NN） */
  spotPrefix: string
  /** 该批次车位总数 */
  spotCount: number
  /** 楼层/位置说明 */
  floor: string
  pricePerHour: number
  /** 允许身份，逗号分隔如 '1,2,3,4,5'；空字符串 = 不限 */
  allowIdentities: string
  /** 后端算好的中文身份名，如「学生/教师/职工/访客/家长」 */
  zoneIdentityNames: string
  freeMinutes: number
  /** 开放时间 yyyy-MM-dd HH:mm:ss */
  openTime: string
  /** 结束时间 yyyy-MM-dd HH:mm:ss */
  closeTime: string
  /** = openTime - 提前预约天数，格式同 openTime */
  reserveFromTime: string
  /** upcoming 开放前·仅可预约 | open 开放中·可入场 | ended 已结束 */
  phase: string
  phaseLabel: string
  /** 距开放还有几天，已开放为 0 */
  daysUntilOpen: number
  canReserve: boolean
  canEnter: boolean
  /** 库中状态：0待开放 1开放中 2已结束 3已取消 */
  status: number
  /** 中文：待开放/开放中/已结束/已取消 */
  statusLabel: string
  vacantCount: number
  occupiedCount: number
  reservedCount: number
  remark: string
  createdAt: string
}

/** 潮汐批次下的车位（与后台车位对象同构，status：0空闲 1已占用 2已预约 3维护中） */
export interface TidalSpot {
  id: string
  spotCode: string
  status: number
  floor: string
}

/** 新建/修改潮汐批次的请求体（时间必须是 'yyyy-MM-dd HH:mm:ss' 字符串） */
export interface TidalBatchForm {
  batchName: string
  area: string
  spotPrefix: string
  spotCount: number
  floor: string
  pricePerHour: number
  /** 允许身份，逗号分隔；空字符串 = 不限身份 */
  allowIdentities: string
  freeMinutes: number
  openTime: string
  closeTime: string
  remark: string
}

/** 潮汐批次列表（Spring 分页对象，data 里为 records/total/current/size） */
export function getTidalBatches(params: any) { return request.get('/admin/tidal-batches', { params }) }
/** 批次详情，data = { batch, spots } */
export function getTidalBatch(id: string) { return request.get(`/admin/tidal-batch/${id}`) }
export function createTidalBatch(data: TidalBatchForm) { return request.post('/admin/tidal-batch', data) }
export function updateTidalBatch(id: string, data: TidalBatchForm) { return request.put(`/admin/tidal-batch/${id}`, data) }
/** 删除批次：仅当该批次所有车位空闲且无任何预约/停车历史，否则后端报错 */
export function deleteTidalBatch(id: string) { return request.delete(`/admin/tidal-batch/${id}`) }
/** 取消批次：存在活跃预约或在场车辆时后端报错 */
export function cancelTidalBatch(id: string) { return request.put(`/admin/tidal-batch/${id}/cancel`) }
/** 恢复批次：解除「已取消」，按开放/结束时间重算状态；重算为待开放/开放中会自动重新启用车位 */
export function restoreTidalBatch(id: string) { return request.put(`/admin/tidal-batch/${id}/restore`) }
/** 软删除批次：仅从管理端列表隐藏（status=4），车位与预约/停车记录全部保留 */
export function softDeleteTidalBatch(id: string) { return request.put(`/admin/tidal-batch/${id}/soft-delete`) }
/** 彻底删除批次：物理删除批次与其全部车位（忽略历史记录；存在在场/被预约车位时后端报错） */
export function forceDeleteTidalBatch(id: string) { return request.delete(`/admin/tidal-batch/${id}/force`) }

// Announcements（公告管理，支持附件）
/** 公告（后端响应字段；时间均为 'yyyy-MM-dd HH:mm:ss' 字符串） */
export interface Announcement {
  id: string
  title: string
  content: string
  /** 附件原始文件名；空字符串 = 无附件 */
  attachmentName: string
  /** 静态访问路径 /uploads/announcement/xxx.pdf；空字符串 = 无附件 */
  attachmentUrl: string
  /** 带原始文件名的下载地址 /api/announcement/{id}/attachment；空字符串 = 无附件 */
  attachmentDownloadUrl: string
  /** 附件字节数 */
  attachmentSize: number
  /** 小写扩展名，如 pdf / docx / zip / png */
  attachmentExt: string
  /** 0草稿 1已发布 */
  status: number
  /** 中文：草稿 / 已发布 */
  statusLabel: string
  publisher: string
  /** 未发布为空字符串 */
  publishTime: string
  viewCount: number
  createdAt: string
  updatedAt: string
}

/** 新建/修改公告的表单字段（附件单独通过 FormData 的 file 字段提交） */
export interface AnnouncementForm {
  title: string
  content: string
  /** 0草稿 1已发布 */
  status: number
}

/** 公告列表（Spring 分页对象，data 里为 records/total/current/size）；status 不传为全部 */
export function getAnnouncements(params: any) { return request.get('/admin/announcements', { params }) }
/** 公告详情 */
export function getAnnouncement(id: string) { return request.get(`/admin/announcement/${id}`) }
/** 新建公告：multipart/form-data（title/content/status + 可选 file），不要手动设置 Content-Type；
 *  附件最大 20MB，慢网络下可能超过默认 15s，故单独放宽超时 */
export function createAnnouncement(formData: FormData) { return request.post('/admin/announcement', formData, { timeout: 120000 }) }
/** 修改公告：multipart/form-data（字段同上，另可带 removeAttachment=true 表示删除现有附件） */
export function updateAnnouncement(id: string, formData: FormData) { return request.put(`/admin/announcement/${id}`, formData, { timeout: 120000 }) }
/** 发布/下架：status 1发布 0下架 */
export function updateAnnouncementStatus(id: string, status: number) { return request.put(`/admin/announcement/${id}/status?status=${status}`) }
/** 删除公告：后端会一并删除附件文件 */
export function deleteAnnouncement(id: string) { return request.delete(`/admin/announcement/${id}`) }

// Scheduled jobs（定时任务管理：创建 / 修改 / 删除 / 启停 / 立即执行）
export function getScheduledJobs(params: any) { return request.get('/admin/jobs', { params }) }
/** 可选的执行器白名单（key / name / description / defaultCron） */
export function getScheduledJobHandlers() { return request.get('/admin/job/handlers') }
export function createScheduledJob(data: any) { return request.post('/admin/job', data) }
export function updateScheduledJob(id: number, data: any) { return request.put(`/admin/job/${id}`, data) }
export function deleteScheduledJob(id: number) { return request.delete(`/admin/job/${id}`) }
export function updateScheduledJobStatus(id: number, status: number) {
  return request.put(`/admin/job/${id}/status?status=${status}`)
}
/** 立即执行一次（异步，不影响原 Cron 调度） */
export function runScheduledJob(id: number) { return request.post(`/admin/job/${id}/run`) }
