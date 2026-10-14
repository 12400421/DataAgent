import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30_000,
})

export async function listDatasets() {
  const response = await api.get('/datasets')
  return response.data
}

export async function uploadDataset(file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post('/datasets', formData)
  return response.data
}

export async function getDataset(id) {
  const response = await api.get(`/datasets/${id}`)
  return response.data
}

export async function getDatasetPreview(id, limit = 10) {
  const response = await api.get(`/datasets/${id}/preview`, {
    params: { limit },
  })
  return response.data
}

export async function getDatasetQuality(id) {
  const response = await api.get(`/datasets/${id}/quality`)
  return response.data
}

export async function getDatasetProfile(id) {
  const response = await api.get(`/datasets/${id}/profile`)
  return response.data
}

export async function requestDatasetAiAnalysis(id) {
  const response = await api.post(`/datasets/${id}/ai-analysis`, undefined, {
    timeout: 120_000,
  })
  return response.data
}

export async function askDatasetQuestion(id, question) {
  const response = await api.post(`/datasets/${id}/ai-question`, { question }, {
    timeout: 120_000,
  })
  return response.data
}

export async function deleteDataset(id) {
  await api.delete(`/datasets/${id}`)
}

export function getApiErrorMessage(error, fallback = '请求失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}
