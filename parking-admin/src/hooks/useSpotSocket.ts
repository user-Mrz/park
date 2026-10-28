import { ref } from 'vue'

/**
 * 车位状态实时推送（管理端单例）。
 *
 * 管理端首页与车位地图共用同一个 WebSocket 连接，避免多组件重复建连。
 * 断线后按 2s → 4s → 8s → 16s → 30s 退避重连；服务端 25 秒发一次 ping，
 * 客户端回 pong 维持连接。轮询仍保留为兜底。
 */

export interface SpotSocketSpot {
  id: string
  spotCode: string
  area: string
  zoneCode: string
  allowIdentities: string
  status: number
}

export interface SpotSocketCounts {
  vacant: number
  occupied: number
  reserved: number
  maintenance: number
  total: number
}

export interface SpotSocketMessage {
  type: 'snapshot' | 'patch'
  version: number
  ts: number
  reason?: string
  spots?: SpotSocketSpot[]
  changes?: { id: string; status: number }[]
  counts?: SpotSocketCounts
}

type SpotMessageHandler = (message: SpotSocketMessage) => void

/** 重连退避序列（毫秒），达到上限后持续按 30 秒重试 */
const RECONNECT_DELAYS = [2000, 4000, 8000, 16000, 30000]
/** 连续失败超过该次数后停止重连，等页面重新订阅时再试 */
const MAX_RECONNECT_ATTEMPTS = 8

const connected = ref(false)
const lastMessageAt = ref(0)

const handlers = new Set<SpotMessageHandler>()
let socket: WebSocket | null = null
let reconnectTimer: number | null = null
let reconnectAttempts = 0
let lastVersion = 0

function buildUrl(): string {
  const token = localStorage.getItem('admin_token')
  if (!token) return ''
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${scheme}://${window.location.host}/ws/spots?token=${encodeURIComponent(token)}`
}

function dispatch(message: SpotSocketMessage) {
  // 丢弃乱序/重复消息
  if (message.version && message.version <= lastVersion) return
  lastVersion = message.version
  handlers.forEach((handler) => {
    try {
      handler(message)
    } catch (e) {
      // 单个订阅者异常不影响其他订阅者
    }
  })
}

function connect() {
  if (!localStorage.getItem('admin_token') || handlers.size === 0) return
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return

  const url = buildUrl()
  if (!url) return

  try {
    socket = new WebSocket(url)
  } catch (e) {
    scheduleReconnect()
    return
  }

  socket.onopen = () => {
    connected.value = true
    reconnectAttempts = 0
    lastVersion = 0
  }

  socket.onmessage = (event) => {
    lastMessageAt.value = Date.now()
    try {
      const message = JSON.parse(event.data)
      if (message.type === 'ping') {
        socket?.send(JSON.stringify({ type: 'pong' }))
        return
      }
      if (message.type === 'snapshot' || message.type === 'patch') {
        dispatch(message as SpotSocketMessage)
      }
    } catch (e) {
      // 非 JSON 消息忽略
    }
  }

  socket.onerror = () => {
    // 具体原因通过 onclose 处理
  }

  socket.onclose = (event) => {
    connected.value = false
    socket = null
    if (event.code === 4001) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      localStorage.removeItem('admin_my_menus')
      window.location.href = '/login'
      return
    }
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (handlers.size === 0 || reconnectTimer !== null) return
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
  const delay = RECONNECT_DELAYS[Math.min(reconnectAttempts, RECONNECT_DELAYS.length - 1)]
  reconnectAttempts++
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connect()
  }, delay)
}

function disconnect() {
  if (reconnectTimer !== null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = null
  }
  connected.value = false
  reconnectAttempts = 0
}

function subscribe(handler: SpotMessageHandler) {
  handlers.add(handler)
  if (!socket) connect()
}

function unsubscribe(handler: SpotMessageHandler) {
  handlers.delete(handler)
  if (handlers.size === 0) disconnect()
}

/** 主动要求服务端补发全量快照（页面重新可见、怀疑数据落后时调用） */
function resync() {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ type: 'resync' }))
  }
}

export function useSpotSocket() {
  return { connected, lastMessageAt, subscribe, unsubscribe, resync }
}
