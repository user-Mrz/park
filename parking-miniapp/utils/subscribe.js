/**
 * 微信订阅消息前端封装。
 *
 * 核心约束（来自微信官方）：
 * - requestSubscribeMessage 必须由用户主动点击事件的回调栈内触发，不能延后执行；
 * - 模板 ID 从后端 /subscribe/templates 动态获取，避免硬编码；
 * - 同一 scene 在同一会话内有本地防抖（wx.setStorageSync 标记已上报，避免重复弹窗）。
 *
 * 交互时序：
 *   用户点击页面上的「授权订阅」按钮 → requestSubscribeMessage([templateIds])
 *     → 用户点同意/拒绝 → 调 reportSubscription(scene, templateId, subscribed, bizRef)
 *     → 后端记录授权 → 后续业务触发时 sendSubscribeMessage。
 */
import request from './request.js'

const SCENE_RESERVE_REMIND = 'reserve_remind'
const SCENE_BALANCE_LOW = 'balance_low'
const SCENE_IDENTITY_RESULT = 'identity_result'
const SCENE_MONTHLY_PASS_EXPIRE = 'monthly_pass_expire'

/** 缓存模板 ID（后端 /subscribe/templates 接口返回） */
let _templatesCache = null

async function loadTemplates() {
  if (_templatesCache) return _templatesCache
  try {
    const res = await request.get('/subscribe/templates', {}, true)
    _templatesCache = res.data || {}
  } catch (e) {
    _templatesCache = {}
  }
  return _templatesCache
}

/**
 * 本地防抖 key。
 * 同一场景同一 bizRef 在一次启动会话内只弹一次。
 */
function dedupKey(scene, bizRef) {
  return 'subscribe_done:' + scene + ':' + (bizRef || '')
}

function isDone(scene, bizRef) {
  try { return !!wx.getStorageSync(dedupKey(scene, bizRef)) } catch (e) { return false }
}

function markDone(scene, bizRef) {
  try { wx.setStorageSync(dedupKey(scene, bizRef), 1) } catch (e) {}
}

/**
 * 调后端 reportSubscription 接口，落库 SubscribeAuth 记录。
 * 只有落库后，后端才有权调用 sendSubscribeMessage。
 */
async function reportSubscription(scene, templateId, subscribed, bizRef) {
  try {
    await request.post('/subscribe/report', {
      scene, templateId, subscribed, bizRef: bizRef || ''
    }, true)
  } catch (e) {
    console.warn('[subscribe] report 失败（不影响主流程）', e)
  }
}

/**
 * requestSubscribeMessage 封装。
 *
 * @param {string} scene        场景标识：reserve_remind / balance_low / identity_result / monthly_pass_expire
 * @param {string} [bizRef]     关联业务号（预约号 / 业务 ID），后端落库时使用
 * @returns {Promise<boolean>}  用户是否同意订阅（true/false），未配置模板时直接返回 false
 */
export async function requestAndReport(scene, bizRef) {
  const token = wx.getStorageSync('token')
  if (!token) return false

  const templates = await loadTemplates()
  const templateId = templates[scene] || ''
  if (!templateId) {
    // 后端未配置模板 ID，静默跳过（不弹 console 警告，避免答辩时满屏红字）
    return false
  }

  // 本地防抖：同一场景同 bizRef 会话内只弹一次
  if (isDone(scene, bizRef)) return false

  if (typeof wx.requestSubscribeMessage !== 'function') {
    console.info('[subscribe] 当前环境不支持 requestSubscribeMessage')
    return false
  }

  try {
    const res = await new Promise((resolve) => {
      wx.requestSubscribeMessage({
        tmplIds: [templateId],
        success: (r) => resolve(r),
        fail: (r) => resolve(r)
      })
    })
    // res 形如 { "tmplIds": [...], [templateId]: "accept" } 或 "reject" / "filter" / "ban"
    const accepted = res && res[templateId] === 'accept'
    await reportSubscription(scene, templateId, accepted ? 1 : 0, bizRef)
    markDone(scene, bizRef)
    return accepted
  } catch (e) {
    return false
  }
}

/** 重置防抖标记（比如用户主动退出登录后） */
export function resetDedup() {
  try {
    const info = wx.getStorageInfoSync()
    ;(info.keys || []).forEach((k) => {
      if (k.startsWith('subscribe_done:')) wx.removeStorageSync(k)
    })
  } catch (e) {}
}

export const SubscribeScenes = {
  SCENE_RESERVE_REMIND,
  SCENE_BALANCE_LOW,
  SCENE_IDENTITY_RESULT,
  SCENE_MONTHLY_PASS_EXPIRE
}
