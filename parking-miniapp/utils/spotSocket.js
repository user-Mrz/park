/**
 * 车位状态实时推送（小程序端单例）。
 *
 * - 页面 onShow 调 subscribe(handler)，onHide/onUnload 调 unsubscribe(handler)
 * - 首个订阅者触发连接，最后一个订阅者移除后断开，避免后台空转
 * - 断线按 2s → 4s → 8s → 16s → 30s 退避重连；服务端 25 秒 ping，客户端回 pong
 * - 保留 HTTP 轮询兜底：页面可通过 isConnected() / lastMessageTime() 判断是否跳过本轮请求
 *
 * 消息格式见后端 SpotRealtimeService：snapshot / patch / ping / error。
 */
import { BASE_URL } from './request.js'

const RECONNECT_DELAYS = [2000, 4000, 8000, 16000, 30000]
const MAX_RECONNECT_ATTEMPTS = 8

const subscribers = new Set()

let socket = null
let connected = false
let reconnectTimer = null
let reconnectAttempts = 0
let lastMessageAt = 0
let lastVersion = 0
let manuallyClosed = false

function buildUrl() {
  let token = ''
  try {
    token = wx.getStorageSync('token') || ''
  } catch (e) {
    token = ''
  }
  if (!token) return ''
  // BASE_URL 形如 http://host:8080/api，去掉 /api 后拼 /ws/spots
  const root = String(BASE_URL || '').replace(/^http/, 'ws').replace(/\/api$/, '')
  return root + '/ws/spots?token=' + encodeURIComponent(token)
}

function dispatch(message) {
  if (!message) return
  if (message.version && message.version <= lastVersion) return
  lastVersion = message.version
  subscribers.forEach(function(handler) {
    try {
      handler(message)
    } catch (e) {
      // 单个订阅者异常不影响其他订阅者
    }
  })
}

function scheduleReconnect() {
  if (manuallyClosed || subscribers.size === 0 || reconnectTimer) return
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
  const delay = RECONNECT_DELAYS[Math.min(reconnectAttempts, RECONNECT_DELAYS.length - 1)]
  reconnectAttempts += 1
  reconnectTimer = setTimeout(function() {
    reconnectTimer = null
    connect()
  }, delay)
}

function connect() {
  if (subscribers.size === 0) return
  if (socket) return
  const url = buildUrl()
  if (!url) return

  manuallyClosed = false
  try {
    socket = wx.connectSocket({ url: url })
  } catch (e) {
    socket = null
    scheduleReconnect()
    return
  }

  socket.onOpen(function() {
    connected = true
    reconnectAttempts = 0
    lastVersion = 0
  })

  socket.onMessage(function(res) {
    lastMessageAt = Date.now()
    let message
    try {
      message = JSON.parse(res.data)
    } catch (e) {
      return
    }
    if (!message) return
    if (message.type === 'ping') {
      try {
        socket.send({ data: JSON.stringify({ type: 'pong' }) })
      } catch (e) {
        // 连接已断开，交给 onClose 重连
      }
      return
    }
    if (message.type === 'error') {
      if (message.code === 4001) {
        // 登录过期：停止重连，清理登录态回登录页
        manuallyClosed = true
        try {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
        } catch (e) {
          // 存储不可用时忽略
        }
        wx.reLaunch({ url: '/pages/login/login' })
      }
      return
    }
    if (message.type === 'snapshot' || message.type === 'patch') {
      dispatch(message)
    }
  })

  socket.onClose(function() {
    connected = false
    socket = null
    if (!manuallyClosed) scheduleReconnect()
  })

  socket.onError(function() {
    connected = false
    // 通常随后会触发 onClose，由 onClose 负责重连
  })
}

function close() {
  manuallyClosed = true
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socket) {
    try {
      socket.close({ code: 1000, reason: 'page hidden' })
    } catch (e) {
      // 忽略
    }
    socket = null
  }
  connected = false
  reconnectAttempts = 0
}

function subscribe(handler) {
  if (typeof handler !== 'function') return function() {}
  subscribers.add(handler)
  if (!socket) connect()
  return function() {
    unsubscribe(handler)
  }
}

function unsubscribe(handler) {
  subscribers.delete(handler)
  if (subscribers.size === 0) close()
}

function isConnected() {
  return connected
}

function lastMessageTime() {
  return lastMessageAt
}

/** 主动要求服务端补发全量快照（页面重新可见、怀疑数据落后时调用） */
function resync() {
  if (socket && connected) {
    try {
      socket.send({ data: JSON.stringify({ type: 'resync' }) })
    } catch (e) {
      // 忽略
    }
  }
}

export default {
  subscribe,
  unsubscribe,
  isConnected,
  lastMessageTime,
  resync
}
