import { onBeforeUnmount, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { createAutoRefresher } from '@/hooks/autoRefresh'

export interface AutoRefreshOptions {
  /** 轮询间隔（毫秒），默认 30 秒 */
  intervalMs?: number
  /** 是否默认开启，默认 true */
  enabled?: boolean
}

export interface AutoRefreshState {
  /** 当前是否开启自动刷新（可在页面上用开关切换） */
  enabled: Ref<boolean>
  toggle: (v: boolean) => void
  /** 最近一次成功刷新的时间，用于页面显示「最后更新 HH:mm:ss」 */
  lastUpdated: Ref<Date | null>
  intervalSec: number
}

/**
 * 管理端自动刷新（Vue 3 封装，核心逻辑在 hooks/autoRefresh.ts）：
 * - 页面可见时按 intervalMs 轮询，调用 load(true) 做静默刷新
 * - 浏览器标签页切到后台时暂停，切回前台立即补一次
 * - 上一次请求未返回时跳过本轮
 *
 * 前端不再展示开关/「最后更新」组件，各页面直接调用即可：
 *   const loadData = async (silent = false) => { if (!silent) loading.value = true ... }
 *   useAutoRefresh(loadData, { intervalMs: 30000 })
 */
export function useAutoRefresh(
  load: (silent?: boolean) => unknown,
  options: AutoRefreshOptions = {}
): AutoRefreshState {
  const { intervalMs = 30000, enabled: enabledDefault = true } = options
  const enabled = ref(enabledDefault)
  const lastUpdated = ref<Date | null>(null)

  let refresher: ReturnType<typeof createAutoRefresher> | null = null

  const onVisibility = () => {
    if (!document.hidden) refresher?.runNow()
  }

  const start = () => {
    if (refresher) return
    refresher = createAutoRefresher({
      load: (silent) => load(silent),
      intervalMs,
      onUpdated: (at) => {
        lastUpdated.value = at
      }
    })
    refresher.start()
    document.addEventListener('visibilitychange', onVisibility)
  }

  const stop = () => {
    document.removeEventListener('visibilitychange', onVisibility)
    refresher?.stop()
    refresher = null
  }

  watch(
    enabled,
    (on) => {
      if (on) start()
      else stop()
    },
    { immediate: true }
  )

  onBeforeUnmount(stop)

  return {
    enabled,
    toggle: (v: boolean) => {
      enabled.value = v
    },
    lastUpdated,
    intervalSec: Math.round(intervalMs / 1000)
  }
}
