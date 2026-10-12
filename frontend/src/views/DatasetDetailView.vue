<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import DatasetVisualizations from '../components/DatasetVisualizations.vue'
import {
  getApiErrorMessage,
  getDataset,
  getDatasetPreview,
  getDatasetProfile,
  getDatasetQuality,
  requestDatasetAiAnalysis,
} from '../api/datasets'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const dataset = ref(null)
const preview = ref(null)
const quality = ref(null)
const profile = ref(null)
const loading = ref(false)
const analysisLoading = ref(false)
const errorMessage = ref('')
const analysisError = ref('')
const aiLoading = ref(false)
const aiReport = ref('')
const aiNotice = ref('')
const aiError = ref('')

const previewColumns = computed(() => {
  if (preview.value?.columnNames?.length) {
    return preview.value.columnNames
  }
  const firstRow = preview.value?.preview?.[0]
  return firstRow ? Object.keys(firstRow) : []
})

async function loadDataset() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [detailResult, previewResult] = await Promise.all([
      getDataset(props.id),
      getDatasetPreview(props.id, 10),
    ])
    dataset.value = detailResult
    preview.value = previewResult
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '数据集详情加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAnalysis() {
  analysisLoading.value = true
  analysisError.value = ''
  const [qualityResult, profileResult] = await Promise.allSettled([
    getDatasetQuality(props.id),
    getDatasetProfile(props.id),
  ])

  if (qualityResult.status === 'fulfilled') {
    quality.value = qualityResult.value
  } else {
    analysisError.value = getApiErrorMessage(qualityResult.reason, '数据质量分析加载失败')
  }

  if (profileResult.status === 'fulfilled') {
    profile.value = profileResult.value
  } else {
    const message = getApiErrorMessage(profileResult.reason, '数据统计分析加载失败')
    analysisError.value = analysisError.value ? `${analysisError.value}；${message}` : message
  }
  analysisLoading.value = false
}

async function runAiAnalysis() {
  aiLoading.value = true
  aiReport.value = ''
  aiNotice.value = ''
  aiError.value = ''

  try {
    const result = await requestDatasetAiAnalysis(props.id)
    if (!result.configured) {
      aiNotice.value = result.message || 'AI 模型尚未配置'
      return
    }

    const report = typeof result.report === 'string' ? result.report.trim() : ''
    if (!report) {
      aiError.value = 'AI 分析未返回有效报告，请稍后重试'
      return
    }
    aiReport.value = report
  } catch (error) {
    aiError.value = getApiErrorMessage(error, 'AI 分析失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatPercent(value) {
  return value === null || value === undefined ? '-' : `${(value * 100).toFixed(2)}%`
}

function displayNumber(value) {
  return value === null || value === undefined ? '-' : value
}

function isNumeric(column) {
  return column.type === 'INTEGER' || column.type === 'DECIMAL'
}

onMounted(() => {
  loadDataset()
  loadAnalysis()
})
</script>

<template>
  <div class="detail-topbar">
    <RouterLink class="back-link" to="/">← 返回数据集列表</RouterLink>
    <button class="button button-secondary" :disabled="loading" @click="loadDataset">
      {{ loading ? '刷新中…' : '刷新详情' }}
    </button>
  </div>

  <p v-if="errorMessage" class="message message-error">{{ errorMessage }}</p>

  <div v-if="loading && !dataset" class="panel empty-state">正在加载数据集详情…</div>

  <template v-if="dataset">
    <section class="page-heading detail-heading">
      <div>
        <p class="eyebrow">DATASET #{{ dataset.id }}</p>
        <h1>{{ dataset.filename }}</h1>
        <p>查看数据结构、预览内容和分析结果。</p>
      </div>
      <span class="status-badge status-large">{{ dataset.status }}</span>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>数据行</span>
        <strong>{{ dataset.rows }}</strong>
      </article>
      <article class="metric-card">
        <span>字段列</span>
        <strong>{{ dataset.columns }}</strong>
      </article>
      <article class="metric-card">
        <span>文件大小</span>
        <strong>{{ formatBytes(dataset.fileSize) }}</strong>
      </article>
      <article class="metric-card metric-card-wide">
        <span>创建时间</span>
        <strong>{{ formatDate(dataset.createdAt) }}</strong>
        <small>更新于 {{ formatDate(dataset.updatedAt) }}</small>
      </article>
    </section>

    <section class="panel table-panel">
      <div class="panel-title-row">
        <div>
          <h2>数据预览</h2>
          <p class="muted">显示前 10 行</p>
        </div>
      </div>
      <div v-if="!preview?.preview?.length" class="empty-state">该数据集没有可预览的数据。</div>
      <div v-else class="table-scroll">
        <table>
          <thead>
            <tr>
              <th v-for="column in previewColumns" :key="column">{{ column }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in preview.preview" :key="index">
              <td v-for="column in previewColumns" :key="column">
                {{ row[column] === '' ? '(空)' : row[column] }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="analysis-heading">
      <div>
        <p class="eyebrow">ANALYSIS</p>
        <h2>分析结果</h2>
      </div>
      <button class="button button-secondary" :disabled="analysisLoading" @click="loadAnalysis">
        {{ analysisLoading ? '分析中…' : '重新分析' }}
      </button>
    </div>

    <p v-if="analysisError" class="message message-error">{{ analysisError }}</p>

    <section v-if="quality" class="panel table-panel">
      <div class="panel-title-row">
        <div>
          <h2>数据质量</h2>
          <p class="muted">重复行：{{ quality.duplicateRowCount }}</p>
        </div>
      </div>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>字段</th>
              <th>类型</th>
              <th>缺失值</th>
              <th>缺失率</th>
              <th>唯一值</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="column in quality.columnQualities" :key="column.name">
              <td class="filename-cell">{{ column.name }}</td>
              <td><span class="type-badge">{{ column.type }}</span></td>
              <td>{{ column.missingCount }}</td>
              <td>{{ formatPercent(column.missingRate) }}</td>
              <td>{{ column.uniqueCount }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="profile" class="panel table-panel">
      <div class="panel-title-row">
        <div>
          <h2>统计分析</h2>
          <p class="muted">数值统计与高频值分布</p>
        </div>
      </div>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>字段</th>
              <th>类型</th>
              <th>统计结果</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="column in profile.columnProfiles" :key="column.name">
              <td class="filename-cell">{{ column.name }}</td>
              <td><span class="type-badge">{{ column.type }}</span></td>
              <td>
                <div v-if="isNumeric(column)" class="stat-list">
                  <span>count <strong>{{ displayNumber(column.statistics.count) }}</strong></span>
                  <span>min <strong>{{ displayNumber(column.statistics.min) }}</strong></span>
                  <span>max <strong>{{ displayNumber(column.statistics.max) }}</strong></span>
                  <span>mean <strong>{{ displayNumber(column.statistics.mean) }}</strong></span>
                  <span>median <strong>{{ displayNumber(column.statistics.median) }}</strong></span>
                </div>
                <div v-else>
                  <p class="inline-stat">唯一值 <strong>{{ column.statistics.uniqueCount }}</strong></p>
                  <div v-if="column.statistics.topValues?.length" class="tag-list">
                    <span
                      v-for="item in column.statistics.topValues"
                      :key="item.value"
                      class="value-tag"
                    >
                      {{ item.value }} <strong>{{ item.count }}</strong>
                    </span>
                  </div>
                  <span v-else class="muted">暂无非空值</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <DatasetVisualizations :quality="quality" :profile="profile" :loading="analysisLoading" />

    <section class="panel ai-analysis-panel">
      <div class="ai-analysis-header">
        <div>
          <p class="eyebrow">AI ANALYSIS</p>
          <h2>AI 智能分析</h2>
          <p class="muted">根据数据集基本信息、质量分析和统计分析生成综合报告。</p>
        </div>
        <button class="button button-primary" :disabled="aiLoading" @click="runAiAnalysis">
          {{ aiLoading ? 'AI 分析中…' : '开始 AI 分析' }}
        </button>
      </div>

      <p v-if="aiNotice" class="message message-info">{{ aiNotice }}</p>
      <p v-if="aiError" class="message message-error">{{ aiError }}</p>
      <div v-if="aiLoading" class="ai-analysis-placeholder">
        正在整理现有分析结果并请求 AI 模型，请稍候…
      </div>
      <article v-else-if="aiReport" class="ai-report" aria-live="polite">
        <pre>{{ aiReport }}</pre>
      </article>
      <div v-else-if="!aiNotice && !aiError" class="ai-analysis-placeholder">
        点击“开始 AI 分析”生成数据概况、质量问题、统计结论和分析建议。
      </div>
    </section>
  </template>
</template>
