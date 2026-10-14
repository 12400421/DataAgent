<script setup>
import { ref } from 'vue'

import { askDatasetQuestion, getApiErrorMessage } from '../api/datasets'

const props = defineProps({
  datasetId: {
    type: [Number, String],
    required: true,
  },
})

const question = ref('')
const answer = ref('')
const loading = ref(false)
const errorMessage = ref('')
const noticeMessage = ref('')

async function sendQuestion() {
  const normalizedQuestion = question.value.trim()
  if (!normalizedQuestion) {
    errorMessage.value = '请输入要询问的问题'
    return
  }

  loading.value = true
  answer.value = ''
  errorMessage.value = ''
  noticeMessage.value = ''

  try {
    const result = await askDatasetQuestion(props.datasetId, normalizedQuestion)
    if (!result.configured) {
      noticeMessage.value = result.message || 'AI 模型尚未配置'
      return
    }

    const responseAnswer = typeof result.answer === 'string' ? result.answer.trim() : ''
    if (!responseAnswer) {
      errorMessage.value = result.message || 'AI 未返回有效回答，请稍后重试'
      return
    }
    answer.value = responseAnswer
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'AI 问答失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="panel ai-question-panel">
    <div class="ai-question-heading">
      <p class="eyebrow">AI QUESTION</p>
      <h2>数据集 AI 问答</h2>
      <p class="muted">针对当前数据集的字段、质量和统计结果提出问题。</p>
    </div>

    <form class="question-form" @submit.prevent="sendQuestion">
      <label for="dataset-ai-question">问题</label>
      <textarea
        id="dataset-ai-question"
        v-model="question"
        class="question-input"
        rows="4"
        placeholder="例如：这个数据集有哪些字段？"
        :disabled="loading"
      ></textarea>
      <div class="question-actions">
        <span class="muted">AI 将结合当前数据集的分析结果进行回答。</span>
        <button class="button button-primary" type="submit" :disabled="loading || !question.trim()">
          {{ loading ? '发送中…' : '发送问题' }}
        </button>
      </div>
    </form>

    <p v-if="errorMessage" class="message message-error" role="alert">
      {{ errorMessage }}
    </p>
    <p v-if="noticeMessage" class="message message-info">
      {{ noticeMessage }}
    </p>
    <div v-if="loading" class="question-placeholder" aria-live="polite">
      AI 正在分析数据集并生成回答，请稍候…
    </div>
    <article v-else-if="answer" class="question-answer" aria-live="polite">
      <h3>AI 回答</h3>
      <pre>{{ answer }}</pre>
    </article>
    <div v-else-if="!errorMessage && !noticeMessage" class="question-placeholder">
      输入问题后点击“发送问题”查看 AI 回答。
    </div>
  </section>
</template>

<style scoped>
.ai-question-panel {
  padding: 24px;
}

.ai-question-heading h2 {
  margin: 4px 0 7px;
}

.question-form {
  display: grid;
  margin-top: 22px;
  gap: 10px;
}

.question-form label {
  color: #3a465b;
  font-size: 14px;
  font-weight: 700;
}

.question-input {
  width: 100%;
  resize: vertical;
  border: 1px solid #d7ddea;
  border-radius: 10px;
  background: #fff;
  color: #27334a;
  font: inherit;
  line-height: 1.6;
  padding: 12px 14px;
}

.question-input:focus {
  border-color: #335cff;
  outline: 3px solid rgba(51, 92, 255, 0.12);
}

.question-input:disabled {
  background: #f5f6f9;
  cursor: not-allowed;
}

.question-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.ai-question-panel > .message {
  margin: 20px 0 0;
}

.question-placeholder,
.question-answer {
  margin-top: 20px;
  border: 1px solid #e2e7f0;
  border-radius: 12px;
  background: #fafbfc;
  padding: 22px;
}

.question-placeholder {
  border-style: dashed;
  color: #7a8498;
  text-align: center;
}

.question-answer h3 {
  margin: 0 0 12px;
  color: #253149;
  font-size: 16px;
}

.question-answer pre {
  margin: 0;
  color: #29364d;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

@media (max-width: 680px) {
  .ai-question-panel {
    padding: 20px 16px;
  }

  .question-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
