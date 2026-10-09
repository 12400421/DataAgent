<script setup>
import { computed } from 'vue'

import EChartCanvas from './EChartCanvas.vue'

const props = defineProps({
  quality: {
    type: Object,
    default: null,
  },
  profile: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const chartColors = ['#335cff', '#18a77a', '#f59e0b', '#8b5cf6']

const qualityColumns = computed(() =>
  Array.isArray(props.quality?.columnQualities) ? props.quality.columnQualities : [],
)

const hasQualityRows = computed(() => {
  const rows = Number(props.quality?.rows)
  return Number.isFinite(rows) && rows > 0
})

const profileColumns = computed(() =>
  Array.isArray(props.profile?.columnProfiles) ? props.profile.columnProfiles : [],
)

const missingRateOption = computed(() => ({
  animationDuration: 450,
  color: [chartColors[0]],
  grid: { top: 24, right: 24, bottom: 72, left: 56, containLabel: true },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter(parameters) {
      const item = parameters[0]
      return `${escapeHtml(item.name)}<br/>缺失率：${formatNumber(item.value)}%`
    },
  },
  xAxis: {
    type: 'category',
    data: qualityColumns.value.map((column) => column.name),
    axisLabel: { interval: 0, rotate: qualityColumns.value.length > 5 ? 30 : 0, formatter: shortLabel },
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 100,
    axisLabel: { formatter: '{value}%' },
  },
  series: [
    {
      name: '缺失率',
      type: 'bar',
      barMaxWidth: 44,
      data: qualityColumns.value.map((column) => {
        const rate = toFiniteNumber(column.missingRate)
        return rate === null ? 0 : Math.min(100, Math.max(0, rate * 100))
      }),
      itemStyle: { borderRadius: [5, 5, 0, 0] },
    },
  ],
}))

const numericCharts = computed(() =>
  profileColumns.value
    .filter((column) => column.type === 'INTEGER' || column.type === 'DECIMAL')
    .map((column) => {
      const statistics = column.statistics ?? {}
      const values = [statistics.min, statistics.max, statistics.mean, statistics.median].map(
        toFiniteNumber,
      )
      return {
        name: column.name,
        type: column.type,
        hasData: values.some((value) => value !== null),
        option: createNumericOption(values),
      }
    }),
)

const categoricalCharts = computed(() =>
  profileColumns.value
    .filter((column) => ['STRING', 'BOOLEAN', 'DATE'].includes(column.type))
    .map((column) => {
      const topValues = Array.isArray(column.statistics?.topValues)
        ? column.statistics.topValues
            .map((item) => ({ value: String(item.value), count: toFiniteNumber(item.count) }))
            .filter((item) => item.count !== null)
        : []
      return {
        name: column.name,
        type: column.type,
        hasData: topValues.length > 0,
        option: createTopValuesOption(topValues),
      }
    }),
)

function createNumericOption(values) {
  return {
    animationDuration: 450,
    color: [chartColors[1]],
    grid: { top: 32, right: 22, bottom: 42, left: 62, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => formatNumber(value),
    },
    xAxis: { type: 'category', data: ['min', 'max', 'mean', 'median'] },
    yAxis: { type: 'value', scale: true },
    series: [
      {
        name: '统计值',
        type: 'bar',
        barMaxWidth: 48,
        data: values,
        label: { show: true, position: 'top', formatter: ({ value }) => formatNumber(value) },
        itemStyle: { borderRadius: [5, 5, 0, 0] },
      },
    ],
  }
}

function createTopValuesOption(topValues) {
  return {
    animationDuration: 450,
    color: [chartColors[2]],
    grid: { top: 28, right: 22, bottom: 76, left: 54, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(parameters) {
        const item = parameters[0]
        return `${escapeHtml(item.name)}<br/>出现次数：${formatNumber(item.value)}`
      },
    },
    xAxis: {
      type: 'category',
      data: topValues.map((item) => item.value),
      axisLabel: { interval: 0, rotate: topValues.length > 4 ? 30 : 0, formatter: shortLabel },
    },
    yAxis: { type: 'value', minInterval: 1, min: 0 },
    series: [
      {
        name: '出现次数',
        type: 'bar',
        barMaxWidth: 42,
        data: topValues.map((item) => item.count),
        itemStyle: { borderRadius: [5, 5, 0, 0] },
      },
    ],
  }
}

function toFiniteNumber(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function formatNumber(value) {
  const number = toFiniteNumber(value)
  if (number === null) return '-'
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 4 }).format(number)
}

function shortLabel(value) {
  const label = String(value)
  return label.length > 12 ? `${label.slice(0, 12)}…` : label
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}
</script>

<template>
  <section class="visualization-section">
    <div class="visualization-heading">
      <div>
        <p class="eyebrow">VISUALIZATION</p>
        <h2>可视化分析</h2>
        <p>基于数据质量和统计分析结果生成。</p>
      </div>
    </div>

    <div v-if="loading && !quality && !profile" class="panel empty-state">
      正在加载可视化数据…
    </div>

    <template v-else>
      <article class="panel chart-panel">
        <div class="chart-card-header">
          <div>
            <h3>字段缺失率</h3>
            <p class="muted">各字段缺失值占总行数的比例</p>
          </div>
        </div>
        <EChartCanvas
          v-if="qualityColumns.length && hasQualityRows"
          :option="missingRateOption"
          height="340px"
          aria-label="字段缺失率柱状图"
        />
        <div v-else-if="!qualityColumns.length" class="chart-empty">
          暂无字段质量数据，无法生成缺失率图。
        </div>
        <div v-else class="chart-empty">当前数据集没有数据行，无法计算缺失率。</div>
      </article>

      <div class="chart-group-heading">
        <h3>数值字段统计</h3>
        <p class="muted">分别展示 INTEGER / DECIMAL 字段的 min、max、mean 和 median</p>
      </div>
      <div v-if="numericCharts.length" class="chart-grid">
        <article v-for="chart in numericCharts" :key="chart.name" class="panel chart-panel">
          <div class="chart-card-header">
            <h3>{{ chart.name }}</h3>
            <span class="type-badge">{{ chart.type }}</span>
          </div>
          <EChartCanvas
            v-if="chart.hasData"
            :option="chart.option"
            :aria-label="`${chart.name} 数值统计柱状图`"
          />
          <div v-else class="chart-empty">该字段暂无可绘制的数值统计。</div>
        </article>
      </div>
      <div v-else class="panel chart-empty">当前数据集没有数值字段。</div>

      <div class="chart-group-heading">
        <h3>分类字段 Top Values</h3>
        <p class="muted">分别展示 STRING / BOOLEAN / DATE 字段的高频值</p>
      </div>
      <div v-if="categoricalCharts.length" class="chart-grid">
        <article v-for="chart in categoricalCharts" :key="chart.name" class="panel chart-panel">
          <div class="chart-card-header">
            <h3>{{ chart.name }}</h3>
            <span class="type-badge">{{ chart.type }}</span>
          </div>
          <EChartCanvas
            v-if="chart.hasData"
            :option="chart.option"
            :aria-label="`${chart.name} 高频值柱状图`"
          />
          <div v-else class="chart-empty">该字段暂无非空值，无法生成 Top Values 图。</div>
        </article>
      </div>
      <div v-else class="panel chart-empty">当前数据集没有分类、布尔或日期字段。</div>
    </template>
  </section>
</template>
