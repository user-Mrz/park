import request from '@/utils/request'

// 超管账号管理（RBAC：账号只绑定角色，菜单权限由角色决定）
export function getAccounts(params: any) {
  return request.get('/admin/accounts', { params })
}
export function createAccount(data: any) {
  return request.post('/admin/account', data)
}
export function updateAccount(id: number | string, data: any) {
  return request.put(`/admin/account/${id}`, data)
}
export function deleteAccount(id: number | string) {
  return request.delete(`/admin/account/${id}`)
}
export function resetAccountPassword(id: number | string, data: any) {
  return request.put(`/admin/account/${id}/reset-password`, data)
}
export function updateAccountStatus(id: number | string, status: number) {
  return request.put(`/admin/account/${id}/status?status=${status}`)
}
export function getMyMenus() {
  return request.get('/admin/account/my-menus')
}

// 角色管理（RBAC：角色 -> 菜单权限）
export function getRoles(params?: any) {
  return request.get('/admin/roles', { params })
}
export function createRole(data: any) {
  return request.post('/admin/role', data)
}
export function updateRole(id: number | string, data: any) {
  return request.put(`/admin/role/${id}`, data)
}
export function deleteRole(id: number | string) {
  return request.delete(`/admin/role/${id}`)
}
export function getRoleMenus(id: number | string) {
  return request.get(`/admin/role/${id}/menus`)
}
export function assignRoleMenus(id: number | string, data: any) {
  return request.put(`/admin/role/${id}/menus`, data)
}

// 小程序用户端角色管理（RBAC：角色 -> 可见停车区域）
export function getUserRoles(params?: any) {
  return request.get('/admin/user-roles', { params })
}
export function updateUserRole(id: string, data: any) {
  return request.put(`/admin/user-role/${id}`, data)
}
export function getUserRolePerms(id: string) {
  return request.get(`/admin/user-role/${id}/perms`)
}
export function assignUserRolePerms(id: string, data: any) {
  return request.put(`/admin/user-role/${id}/perms`, data)
}
export function changeUserRole(id: string, data: any) {
  return request.put(`/admin/user/${id}/role`, data)
}

// 登录日志 / 操作日志
export function getLoginLogs(params: any) {
  return request.get('/admin/login-logs', { params })
}
export function getLoginLogDetail(id: string) {
  return request.get(`/admin/login-log/${id}`)
}
export function getOperationLogs(params: any) {
  return request.get('/admin/operation-logs', { params })
}
export function getOperationLogDetail(id: string) {
  return request.get(`/admin/operation-log/${id}`)
}

// 个人中心（任意角色的管理员操作自己的资料与密码，不受菜单权限限制）
export function getMyProfile() {
  return request.get('/admin/profile')
}
export function updateMyNickname(nickname: string) {
  return request.put('/admin/profile', { nickname })
}
export function changeMyPassword(oldPassword: string, newPassword: string) {
  return request.put('/admin/profile/password', { oldPassword, newPassword })
}
