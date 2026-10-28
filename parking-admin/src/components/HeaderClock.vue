<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

/**
 * 独立时钟组件：把每秒刷新隔离在组件内部，
 * 避免整个布局或数据看板因时钟走字而每秒重新 diff。
 */
const props = withDefaults(defineProps<{ mode?: 'datetime' | 'date' | 'time' }>(), {
  mode: 'datetime'
})

const WEEK = ['日', '一', '二', '三', '四', '五', '六']
const pad = (n: number) => String(n).padStart(2, '0')

const now = ref(new Date())
const timer = window.setInterval(() => {
  now.value = new Date()
}, 1000)

onBeforeUnmount(() => window.clearInterval(timer))

const text = computed(() => {
  const d = now.value
  if (props.mode === 'time') {
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (props.mode === 'date') {
    return `${date} 星期${WEEK[d.getDay()]}`
  }
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})
</script>

<template>
  <span>{{ text }}</span>
</template>
