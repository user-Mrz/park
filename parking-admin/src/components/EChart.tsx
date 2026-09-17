import { useEffect, useRef } from 'react'
import * as echarts from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
import type { EChartsType } from 'echarts/core'

echarts.use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

interface Props {
  option: EChartsOption
  style?: React.CSSProperties
  className?: string
}

/**
 * 轻量 ECharts 封装：挂载时初始化，option 变化时刷新，随窗口自动 resize。
 * 按需引入 Pie/Title/Tooltip/Legend，减小打包体积。
 */
export default function EChart({ option, style, className }: Props) {
  const domRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<EChartsType | null>(null)

  useEffect(() => {
    if (!domRef.current) return
    const chart = echarts.init(domRef.current)
    chartRef.current = chart
    const onResize = () => chart.resize()
    window.addEventListener('resize', onResize)
    return () => {
      window.removeEventListener('resize', onResize)
      chart.dispose()
      chartRef.current = null
    }
  }, [])

  useEffect(() => {
    chartRef.current?.setOption(option, true)
  }, [option])

  return <div ref={domRef} className={className} style={{ width: '100%', height: 300, ...style }} />
}
