<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
import type { EChartsType } from 'echarts/core'

echarts.use([
  PieChart,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer
])

const props = withDefaults(
  defineProps<{
    option: EChartsOption
    height?: number | string
  }>(),
  { height: 300 }
)

const domRef = ref<HTMLDivElement | null>(null)
let chart: EChartsType | null = null
let resizeObserver: ResizeObserver | null = null
let resizeRaf = 0

const onResize = () => chart?.resize()

onMounted(() => {
  if (!domRef.value) return
  chart = echarts.init(domRef.value)
  chart.setOption(props.option, true)
  window.addEventListener('resize', onResize)
  
  resizeObserver = new ResizeObserver(() => {
    if (resizeRaf) return
    resizeRaf = requestAnimationFrame(() => {
      resizeRaf = 0
      if (!domRef.value || !chart) return
      const { width, height } = domRef.value.getBoundingClientRect()
      if (width > 0 && height > 0) chart.resize()
    })
  })
  resizeObserver.observe(domRef.value)
})

watch(
  () => props.option,
  (opt) => {
    // option 每次数据刷新都是新对象，浅监听即可；lazyUpdate 把绘制推迟到下一帧，减少长任务
    chart?.setOption(opt, { notMerge: true, lazyUpdate: true })
  }
)

onBeforeUnmount(() => {
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf)
    resizeRaf = 0
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="domRef" :style="{ width: '100%', height: typeof height === 'number' ? `${height}px` : height }" />
</template>
