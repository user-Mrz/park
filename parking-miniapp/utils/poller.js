import { createTicker, clearTicker } from './duration.js'

/**
 * 静默轮询器：让小程序端与后端/管理端保持同步
 * （例如管理员在后台强制结算后，用户端无需手动刷新即可看到最新状态）
 *
 * - 每一次轮询都是"静默"的：失败不弹提示、不打断用户操作
 * - 上一次请求还没返回时跳过本轮，避免弱网下请求堆积
 * - 页面 onHide / onUnload 必须调用 stop()，避免后台空转
 */
export function createPoller(task, intervalMs) {
  let timer = null
  let busy = false

  const run = () => {
    if (busy) return Promise.resolve()
    busy = true
    return Promise.resolve()
      .then(() => task())
      .catch(() => { /* 轮询失败静默处理，等下一轮 */ })
      .then(() => { busy = false })
  }

  return {
    start() {
      if (timer) return
      timer = createTicker(run, intervalMs || 5000)
    },
    stop() {
      clearTicker(timer)
      timer = null
    },
    /** 立即执行一次（页面重新可见时用） */
    runNow: run,
    isRunning() {
      return !!timer
    }
  }
}
