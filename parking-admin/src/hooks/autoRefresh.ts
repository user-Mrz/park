/**
 * 自动刷新核心逻辑（纯 TypeScript，不依赖 React，便于单独测试）
 * React 侧封装见 useAutoRefresh.ts
 */

export interface AutoRefresherOptions {
  /** 实际加载函数，调用时传 true 表示"静默刷新"（不显示 loading） */
  load: (silent?: boolean) => unknown
  /** 轮询间隔（毫秒） */
  intervalMs: number
  /** 是否处于不可见状态（不可见时不请求），默认读取 document.hidden */
  isHidden?: () => boolean
  /** 每次成功刷新后回调，用于显示「最后更新」时间 */
  onUpdated?: (at: Date) => void
  /** 刷新失败回调（错误提示通常已由 request 拦截器统一处理） */
  onError?: (e: unknown) => void
}

export interface AutoRefresher {
  /** 开始轮询（重复调用无副作用） */
  start: () => void
  /** 停止轮询 */
  stop: () => void
  /** 立即刷新一次（标签页切回前台时用） */
  runNow: () => Promise<void>
  isRunning: () => boolean
}

/**
 * 创建自动刷新器：
 * - 每一次刷新都是「静默刷新」load(true)，避免表格每次都闪一下 loading
 * - 上一次请求未返回时跳过本轮，防止慢网络下请求堆积
 * - 页面不可见时跳过，不做无意义的轮询
 */
export function createAutoRefresher(options: AutoRefresherOptions): AutoRefresher {
  const {
    load,
    intervalMs,
    isHidden = () => typeof document !== 'undefined' && !!document.hidden,
    onUpdated,
    onError
  } = options

  let timer: ReturnType<typeof setInterval> | null = null
  let busy = false

  const refresh = async (): Promise<void> => {
    if (busy) return
    if (isHidden()) return
    busy = true
    try {
      await load(true)
      if (onUpdated) onUpdated(new Date())
    } catch (e) {
      if (onError) onError(e)
    } finally {
      busy = false
    }
  }

  return {
    start() {
      if (timer !== null) return
      timer = setInterval(refresh, intervalMs)
    },
    stop() {
      if (timer === null) return
      clearInterval(timer)
      timer = null
    },
    runNow: refresh,
    isRunning: () => timer !== null
  }
}
