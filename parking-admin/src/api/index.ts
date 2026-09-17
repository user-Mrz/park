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

// Campus statistics & blacklist
export function getCampusStats() { return request.get('/admin/campus-stats') }
export function getBlacklist(params: any) { return request.get('/admin/blacklist', { params }) }
export function addBlacklist(data: any) { return request.post('/admin/blacklist', data) }
export function removeBlacklist(id: number) { return request.delete(`/admin/blacklist/${id}`) }

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
export function getSpotMap() { return request.get('/admin/spot-map') }

// Recharge & Payment Records
export function getRechargeRecords(params: any) { return request.get('/admin/recharge-records', { params }) }
export function getPaymentRecords(params: any) { return request.get('/admin/payment-records', { params }) }

// System Config
export function getConfig() { return request.get('/admin/config') }
export function updateConfig(data: any) { return request.put('/admin/config', data) }
