<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  deleteDataset,
  getApiErrorMessage,
  listDatasets,
  uploadDataset,
} from '../api/datasets'

const router = useRouter()
const datasets = ref([])
const selectedFile = ref(null)
const fileInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const deletingId = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

async function loadDatasets() {
  loading.value = true
  errorMessage.value = ''
  try {
    datasets.value = await listDatasets()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '数据集列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] ?? null
  successMessage.value = ''
  errorMessage.value = ''
}

async function handleUpload() {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择 CSV 文件'
    return
  }

  uploading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await uploadDataset(selectedFile.value)
    successMessage.value = `${selectedFile.value.name} 上传成功`
    selectedFile.value = null
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    await loadDatasets()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'CSV 上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(dataset) {
  const confirmed = window.confirm(`确定删除数据集“${dataset.filename}”吗？此操作不可撤销。`)
  if (!confirmed) {
    return
  }

  deletingId.value = dataset.id
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await deleteDataset(dataset.id)
    successMessage.value = `${dataset.filename} 已删除`
    await loadDatasets()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '删除数据集失败')
  } finally {
    deletingId.value = null
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

onMounted(loadDatasets)
</script>

<template>
  <section class="page-heading">
    <div>
      <p class="eyebrow">DATASETS</p>
      <h1>数据集管理</h1>
      <p>上传 CSV 文件，查看数据结构和基础分析结果。</p>
    </div>
    <button class="button button-secondary" :disabled="loading" @click="loadDatasets">
      {{ loading ? '刷新中…' : '刷新列表' }}
    </button>
  </section>

  <section class="panel upload-panel">
    <div>
      <h2>上传 CSV</h2>
      <p class="muted">选择本地 CSV 文件，上传后会自动解析并保存。</p>
    </div>
    <div class="upload-actions">
      <label class="file-picker">
        <input ref="fileInput" type="file" accept=".csv,text/csv" @change="handleFileChange" />
        <span>{{ selectedFile?.name || '选择 CSV 文件' }}</span>
      </label>
      <button class="button button-primary" :disabled="uploading" @click="handleUpload">
        {{ uploading ? '上传中…' : '开始上传' }}
      </button>
    </div>
  </section>

  <p v-if="errorMessage" class="message message-error">{{ errorMessage }}</p>
  <p v-if="successMessage" class="message message-success">{{ successMessage }}</p>

  <section class="panel table-panel">
    <div class="panel-title-row">
      <div>
        <h2>全部数据集</h2>
        <p class="muted">共 {{ datasets.length }} 个</p>
      </div>
    </div>

    <div v-if="loading" class="empty-state">正在加载数据集…</div>
    <div v-else-if="datasets.length === 0" class="empty-state">
      暂无数据集，请先上传一个 CSV 文件。
    </div>
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>文件名</th>
            <th>行数</th>
            <th>列数</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dataset in datasets" :key="dataset.id">
            <td class="mono">{{ dataset.id }}</td>
            <td class="filename-cell">{{ dataset.filename }}</td>
            <td>{{ dataset.rows }}</td>
            <td>{{ dataset.columns }}</td>
            <td><span class="status-badge">{{ dataset.status }}</span></td>
            <td>{{ formatDate(dataset.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <button class="button button-small" @click="router.push(`/datasets/${dataset.id}`)">
                  查看
                </button>
                <button
                  class="button button-small button-danger"
                  :disabled="deletingId === dataset.id"
                  @click="handleDelete(dataset)"
                >
                  {{ deletingId === dataset.id ? '删除中…' : '删除' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
