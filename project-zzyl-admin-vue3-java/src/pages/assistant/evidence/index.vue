<template>
  <div class="assistant-evidence-page">
    <section class="evidence-header">
      <div>
        <h2>命中详情</h2>
        <p>{{ detail.question || '本次回答检索到的知识库片段' }}</p>
      </div>
      <t-button variant="outline" @click="goBack">返回问答</t-button>
    </section>

    <section class="summary-band">
      <div>
        <span>采用片段</span>
        <strong>{{ selectedSources.length }}</strong>
      </div>
      <div>
        <span>候选片段</span>
        <strong>{{ totalCandidates }}</strong>
      </div>
      <div>
        <span>置信度</span>
        <strong>{{ confidenceText }}</strong>
      </div>
    </section>

    <section v-if="selectedSources.length > 0" class="source-section">
      <div class="section-title">已采用</div>
      <article
        v-for="source in selectedSources"
        :key="sourceKey(source)"
        class="source-item selected"
      >
        <div class="source-title">
          {{ source.documentTitle || source.sourceFile || '知识库片段' }}
        </div>
        <div class="source-meta">
          <span>{{ source.sourceFile || '未知来源' }}</span>
          <span
            >综合分
            {{
              formatScore(source.scoreBreakdown?.finalScore ?? source.score)
            }}</span
          >
        </div>
        <div v-if="source.matchedTerms?.length" class="source-tags">
          <span
            v-for="term in source.matchedTerms"
            :key="`${sourceKey(source)}-${term}`"
          >
            {{ term }}
          </span>
        </div>
        <pre>{{ source.content || source.snippet || '暂无片段内容' }}</pre>
      </article>
    </section>

    <section v-if="discardedSources.length > 0" class="source-section">
      <div class="section-title">已丢弃</div>
      <article
        v-for="source in discardedSources"
        :key="sourceKey(source)"
        class="source-item"
      >
        <div class="source-title">
          {{ source.documentTitle || source.sourceFile || '知识库片段' }}
        </div>
        <div class="source-meta">
          <span>{{ source.reason || '未被本次回答采用' }}</span>
          <span
            >综合分
            {{
              formatScore(source.scoreBreakdown?.finalScore ?? source.score)
            }}</span
          >
        </div>
        <pre>{{ source.content || source.snippet || '暂无片段内容' }}</pre>
      </article>
    </section>

    <div v-if="!detail.sources?.length" class="empty-state">
      暂未找到命中片段，请回到智能问答重新提问。
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { AssistantRetrievalTrace, AssistantSource } from '@/api/assistant'

interface EvidenceDetail {
  question?: string
  confidence?: number
  sources?: AssistantSource[]
  retrievalTrace?: AssistantRetrievalTrace
}

const STORAGE_KEY = 'assistant:evidence-detail'
const router = useRouter()
const detail = ref<EvidenceDetail>({})

const selectedSources = computed(() =>
  (detail.value.sources || []).filter((source) => source.selected)
)
const discardedSources = computed(() =>
  (detail.value.sources || []).filter((source) => !source.selected)
)
const totalCandidates = computed(
  () =>
    detail.value.retrievalTrace?.totalCandidates ??
    detail.value.sources?.length ??
    0
)
const confidenceText = computed(() => {
  if (detail.value.confidence === undefined) return '--'
  return `${Math.round(detail.value.confidence * 100)}%`
})

const goBack = () => {
  router.push('/assistant/chat')
}

const formatScore = (value?: number) => {
  if (value === undefined || value === null) return '--'
  return value.toFixed(2)
}

const sourceKey = (source: AssistantSource) =>
  source.chunkId || `${source.sourceFile || 'source'}-${source.score || 0}`

onMounted(() => {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return
  try {
    detail.value = JSON.parse(raw)
  } catch {
    detail.value = {}
  }
})
</script>
