<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { formatDuration, formatMinutes } from '@/utils/duration'

const props = defineProps<{
  /** 入场时间（有值时按它实时计算，跨过整分钟自动更新） */
  entryTime?: string | null
  /** 没有入场时间时退化为使用服务端给的分钟数 */
  fallbackMinutes?: number | null
}>()

const text = ref(props.entryTime ? formatDuration(props.entryTime) : formatMinutes(props.fallbackMinutes))

let timer: number | null = null

const update = () => {
  const next = props.entryTime ? formatDuration(props.entryTime) : formatMinutes(props.fallbackMinutes)
  if (next !== text.value) text.value = next
}

const stop = () => {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

watch(
  () => [props.entryTime, props.fallbackMinutes],
  () => {
    stop()
    update()
    if (props.entryTime) {
      timer = window.setInterval(update, 1000)
    }
  },
  { immediate: true }
)

onBeforeUnmount(stop)
</script>

<template>
  <span>{{ text }}</span>
</template>
