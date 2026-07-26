import request from '@/utils/request'

// Dashboard
export function getDashboard() { return request.get('/admin/dashboard') }

// Users
export function getUsers(params: any) { return request.get('/admin/users', { params }) }
export function getUserDetail(id: number) { return request.get(`/admin/user/${id}`) }
export function updateUserStatus(id: number, status: number) { return request.put(`/admin/user/${id}/status?status=${status}`) }

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

// Recharge & Payment Records
export function getRechargeRecords(params: any) { return request.get('/admin/recharge-records', { params }) }
export function getPaymentRecords(params: any) { return request.get('/admin/payment-records', { params }) }

// System Config
export function getConfig() { return request.get('/admin/config') }
export function updateConfig(data: any) { return request.put('/admin/config', data) }
