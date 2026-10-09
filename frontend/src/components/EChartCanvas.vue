<script setup>
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
  height: {
    type: String,
    default: '320px',
  },
  ariaLabel: {
    type: String,
    default: '数据图表',
  },
})

const chartElement = ref(null)
let chart = null
let resizeObserver = null

function renderChart() {
  if (!chartElement.value) return

  if (!chart) {
    chart = echarts.init(chartElement.value, null, { renderer: 'canvas' })
  }
  chart.setOption(props.option, { notMerge: true })
}

async function updateChart() {
  await nextTick()
  renderChart()
}

onMounted(() => {
  renderChart()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartElement.value)
  } else {
    window.addEventListener('resize', resizeChart)
  }
})

watch(() => props.option, updateChart, { deep: true })

function resizeChart() {
  chart?.resize()
}

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div
    ref="chartElement"
    class="chart-canvas"
    :style="{ height }"
    role="img"
    :aria-label="ariaLabel"
  ></div>
</template>
