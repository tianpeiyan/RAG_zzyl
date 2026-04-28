<template>
  <div class="assistant-chat-page">
    <section v-if="evidenceMode" class="evidence-page">
      <div class="conversation-header">
        <div>
          <h2>命中详情</h2>
          <p>{{ evidenceDetail.question || '本次回答检索到的知识库片段' }}</p>
        </div>
        <t-button variant="outline" @click="closeEvidence">返回问答</t-button>
      </div>

      <div class="retrieval-summary">
        <span>采用 {{ selectedEvidenceSources.length }} 个片段</span>
        <span>候选 {{ evidenceTotalCandidates }} 个片段</span>
        <span>置信度 {{ evidenceConfidenceText }}</span>
      </div>

      <div v-if="selectedEvidenceSources.length > 0" class="source-group">
        <div class="source-group-title">已采用</div>
        <div
          v-for="source in selectedEvidenceSources"
          :key="sourceKey(source)"
          class="source-item selected"
        >
          <div class="source-title">
            {{ source.documentTitle || source.sourceFile || '知识库片段' }}
          </div>
          <div class="source-meta">
            <span>{{ source.sourceFile || '未知来源' }}</span>
            <span>
              综合分
              {{
                formatScore(source.scoreBreakdown?.finalScore ?? source.score)
              }}
            </span>
          </div>
          <div v-if="source.matchedTerms?.length" class="source-tags">
            <span
              v-for="term in source.matchedTerms"
              :key="`${sourceKey(source)}-${term}`"
            >
              {{ term }}
            </span>
          </div>
          <div class="source-snippet">
            {{ source.content || source.snippet || '暂无片段内容' }}
          </div>
        </div>
      </div>

      <div v-if="discardedEvidenceSources.length > 0" class="source-group">
        <div class="source-group-title">已丢弃</div>
        <div
          v-for="source in discardedEvidenceSources"
          :key="sourceKey(source)"
          class="source-item"
        >
          <div class="source-title">
            {{ source.documentTitle || source.sourceFile || '知识库片段' }}
          </div>
          <div class="source-meta">
            <span>{{ source.reason || '未被本次回答采用' }}</span>
            <span>
              综合分
              {{
                formatScore(source.scoreBreakdown?.finalScore ?? source.score)
              }}
            </span>
          </div>
          <div class="source-snippet">
            {{ source.content || source.snippet || '暂无片段内容' }}
          </div>
        </div>
      </div>

      <div v-if="!evidenceDetail.sources?.length" class="empty-state">
        暂未找到命中片段，请回到智能问答重新提问。
      </div>
    </section>

    <section v-else class="chat-shell">
      <aside class="history-panel">
        <div class="panel-title">会话历史</div>
        <div
          v-for="item in historySessions"
          :key="item.id"
          class="history-row"
          :class="{ active: item.id === sessionId }"
        >
          <button
            class="history-item"
            type="button"
            @click="switchSession(item.id)"
          >
            <span>{{ item.title }}</span>
            <small>{{ item.count }} 条</small>
          </button>
          <t-button
            size="small"
            variant="text"
            theme="danger"
            @click="removeSession(item.id)"
          >
            删除
          </t-button>
        </div>
        <t-button
          theme="primary"
          variant="outline"
          block
          @click="createSession"
        >
          新建会话
        </t-button>
      </aside>

      <main class="conversation-panel">
        <div class="conversation-header">
          <div>
            <h2>智能助手</h2>
            <p>该回答基于知识库生成</p>
          </div>
          <t-tag theme="success" variant="light">知识问答</t-tag>
        </div>

        <div ref="messageListRef" class="message-list">
          <div v-if="messages.length === 0" class="empty-state">
            <h3>今天想查什么？</h3>
            <p>护理规范、入住流程、系统操作都可以从这里开始。</p>
          </div>

          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="avatar">
              {{ message.role === 'user' ? '我' : 'AI' }}
            </div>
            <div class="message-bubble">
              <div class="message-content">{{ message.content }}</div>
              <div v-if="message.loading" class="message-extra">思考中...</div>
              <div v-if="message.error" class="message-error">
                <span>{{ message.error }}</span>
                <t-button
                  size="small"
                  variant="text"
                  theme="primary"
                  @click="retry(message)"
                >
                  重试
                </t-button>
              </div>
              <div
                v-if="
                  message.role === 'assistant' &&
                  !message.loading &&
                  !message.error
                "
                class="message-extra"
              >
                基于知识库生成
                <span v-if="message.questionType">
                  · {{ formatQuestionType(message.questionType) }}</span
                >
                <span v-if="message.confidence !== undefined">
                  · 置信度 {{ formatPercent(message.confidence) }}</span
                >
                <span v-if="message.hitCount !== undefined">
                  · 采用 {{ message.hitCount }} /
                  {{ totalCandidates(message) }} 个片段</span
                >
              </div>
              <div
                v-if="
                  message.role === 'assistant' &&
                  !message.loading &&
                  !message.error &&
                  hasRetrievalSources(message)
                "
                class="evidence-actions"
              >
                <t-button
                  size="small"
                  variant="outline"
                  theme="primary"
                  @click="openEvidence(message)"
                >
                  查看命中详情
                </t-button>
              </div>
            </div>
          </div>
        </div>

        <div class="recommend-panel">
          <button
            v-for="recommendQuestion in recommendQuestions"
            :key="recommendQuestion"
            type="button"
            :disabled="sending"
            @click="askRecommend(recommendQuestion)"
          >
            {{ recommendQuestion }}
          </button>
        </div>

        <div class="input-panel">
          <t-textarea
            v-model="question"
            placeholder="请输入问题"
            :maxlength="1000"
            :autosize="{ minRows: 2, maxRows: 5 }"
            @keydown="handleInputKeydown"
          />
          <t-button
            theme="primary"
            :loading="sending"
            :disabled="!canSend"
            @click="sendQuestion"
          >
            发送
          </t-button>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  AssistantChatResult,
  AssistantHistoryMessage,
  AssistantMessage,
  AssistantRetrievalTrace,
  AssistantSession,
  AssistantSource,
  chatWithAssistantStream,
  deleteAssistantSession,
  getAssistantMessages,
  getAssistantSessions
} from '@/api/assistant'

type ChatRole = 'user' | 'assistant'

interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  loading?: boolean
  error?: string
  question?: string
  traceId?: string
  hitCount?: number
  sources?: AssistantSource[]
  questionType?: string
  confidence?: number
  retrievalTrace?: AssistantRetrievalTrace
}

interface SessionSummary {
  id: string
  title: string
  count: number
}

const recommendQuestions = [
  '护理等级在哪里配置？',
  '老人入住办理需要哪些步骤？',
  '护理计划怎么创建？',
  '退住办理流程是什么？',
  '预约来访如何登记？',
  '智能床位能查看哪些数据？',
  '账单入账列表在哪里？',
  '知识库暂无依据时应该怎么处理？'
]

const sessionId = ref(`session-${Date.now()}`)
const question = ref('')
const sending = ref(false)
const messages = ref<ChatMessage[]>([])
const messageListRef = ref<HTMLElement>()
const historySessions = ref<SessionSummary[]>([
  { id: sessionId.value, title: '当前会话', count: 0 }
])
const route = useRoute()
const router = useRouter()
const EVIDENCE_STORAGE_KEY = 'assistant:evidence-detail'
const EVIDENCE_STORAGE_PREFIX = 'assistant:evidence-detail:'
const evidenceDetail = ref<{
  question?: string
  confidence?: number
  sources?: AssistantSource[]
  retrievalTrace?: AssistantRetrievalTrace
}>({})

const canSend = computed(
  () => question.value.trim().length > 0 && !sending.value
)
const evidenceMode = computed(() => route.query.evidence === '1')
const selectedEvidenceSources = computed(() =>
  (evidenceDetail.value.sources || []).filter((source) => source.selected)
)
const discardedEvidenceSources = computed(() =>
  (evidenceDetail.value.sources || []).filter((source) => !source.selected)
)
const evidenceTotalCandidates = computed(
  () =>
    evidenceDetail.value.retrievalTrace?.totalCandidates ??
    evidenceDetail.value.sources?.length ??
    0
)
const evidenceConfidenceText = computed(() => {
  if (evidenceDetail.value.confidence === undefined) return '--'
  return formatPercent(evidenceDetail.value.confidence)
})

onMounted(() => {
  restoreEvidenceDetail()
  loadSessions()
})

watch(
  () => [route.query.evidence, route.query.evidenceId],
  () => {
    if (evidenceMode.value) restoreEvidenceDetail()
  }
)

const sendQuestion = async () => {
  const text = question.value.trim()
  if (!text || sending.value) return
  question.value = ''
  await appendQuestion(text)
}

const askRecommend = async (text: string) => {
  if (sending.value) return
  question.value = ''
  await appendQuestion(text)
}

const appendQuestion = async (text: string) => {
  const userMessage: ChatMessage = {
    id: createId(),
    role: 'user',
    content: text
  }
  const assistantMessage: ChatMessage = {
    id: createId(),
    role: 'assistant',
    content: '',
    loading: true,
    question: text
  }
  messages.value.push(userMessage, assistantMessage)
  updateSessionSummary(text)
  await scrollToBottom()
  await requestAnswer(assistantMessage, text)
}

const requestAnswer = async (assistantMessage: ChatMessage, text: string) => {
  sending.value = true
  assistantMessage.loading = true
  assistantMessage.error = ''
  assistantMessage.content = ''
  try {
    await chatWithAssistantStream(
      {
        sessionId: sessionId.value,
        question: text,
        history: buildHistory()
      },
      {
        onDelta: async (delta) => {
          assistantMessage.content += delta
          await scrollToBottom()
        },
        onDone: (data: AssistantChatResult) => {
          assistantMessage.content =
            assistantMessage.content || data.answer || '知识库暂无依据'
          assistantMessage.hitCount = data.hitCount
          assistantMessage.traceId = data.traceId
          assistantMessage.sources = data.sources || []
          assistantMessage.questionType = data.questionType
          assistantMessage.confidence = data.confidence
          assistantMessage.retrievalTrace = data.retrievalTrace
          if (data.sessionId) sessionId.value = data.sessionId
        },
        onError: (message) => {
          throw new Error(message)
        }
      }
    )
    await loadSessions(false)
  } catch (error: any) {
    assistantMessage.content = '回答生成失败'
    assistantMessage.error = error?.message || '智能助手暂不可用'
    MessagePlugin.error(assistantMessage.error)
  } finally {
    assistantMessage.loading = false
    sending.value = false
    updateSessionSummary()
    await scrollToBottom()
  }
}

const retry = async (message: ChatMessage) => {
  if (!message.question || sending.value) return
  await requestAnswer(message, message.question)
}

const handleInputKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Enter') return
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  sendQuestion()
}

const buildHistory = (): AssistantMessage[] => {
  return messages.value
    .filter((item) => item.content && !item.loading && !item.error)
    .slice(-10)
    .map((item) => ({
      role: item.role,
      content: item.content
    }))
}

const createSession = () => {
  sessionId.value = `session-${Date.now()}`
  messages.value = []
  if (!historySessions.value.some((item) => item.id === sessionId.value)) {
    historySessions.value.unshift({
      id: sessionId.value,
      title: '新会话',
      count: 0
    })
  }
}

const switchSession = async (id: string) => {
  sessionId.value = id
  await loadMessages(id)
}

const updateSessionSummary = (title?: string) => {
  const current = historySessions.value.find(
    (item) => item.id === sessionId.value
  )
  if (!current) return
  if (title && current.title === '当前会话') {
    current.title = title.length > 16 ? `${title.slice(0, 16)}...` : title
  }
  current.count = messages.value.filter((item) => item.role === 'user').length
}

const scrollToBottom = async () => {
  await nextTick()
  if (!messageListRef.value) return
  messageListRef.value.scrollTop = messageListRef.value.scrollHeight
}

const loadSessions = async (showError = true) => {
  try {
    const res: any = await getAssistantSessions()
    if (res.code !== 200) {
      throw new Error(res.msg || '会话历史加载失败')
    }
    const sessions = ((res.data || []) as AssistantSession[]).map((item) => ({
      id: item.sessionId,
      title: normalizeTitle(item.title),
      count: item.questionCount || item.messageCount || 0
    }))
    if (sessions.length > 0) {
      historySessions.value = sessions
      if (!historySessions.value.some((item) => item.id === sessionId.value)) {
        sessionId.value = historySessions.value[0].id
        await loadMessages(sessionId.value)
      }
    }
  } catch (error: any) {
    if (showError) MessagePlugin.error(error?.message || '会话历史加载失败')
  }
}

const loadMessages = async (id: string) => {
  try {
    const res: any = await getAssistantMessages(id)
    if (res.code !== 200) {
      throw new Error(res.msg || '消息加载失败')
    }
    let lastQuestion = ''
    messages.value = ((res.data || []) as AssistantHistoryMessage[]).map(
      (item) => {
        if (item.role === 'user') lastQuestion = item.content
        return {
          id: String(item.id),
          role: item.role,
          content: item.content,
          question: item.role === 'assistant' ? lastQuestion : undefined,
          traceId: item.traceId,
          hitCount: item.hitCount,
          sources: item.sources || [],
          questionType: item.questionType,
          confidence: item.confidence,
          retrievalTrace: item.retrievalTrace
        }
      }
    )
    await scrollToBottom()
  } catch (error: any) {
    MessagePlugin.error(error?.message || '消息加载失败')
  }
}

const removeSession = async (id: string) => {
  try {
    const res: any = await deleteAssistantSession(id)
    if (res.code !== 200) {
      throw new Error(res.msg || '删除失败')
    }
    historySessions.value = historySessions.value.filter(
      (item) => item.id !== id
    )
    if (sessionId.value === id) {
      if (historySessions.value.length > 0) {
        sessionId.value = historySessions.value[0].id
        await loadMessages(sessionId.value)
      } else {
        createSession()
      }
    }
  } catch (error: any) {
    MessagePlugin.error(error?.message || '删除失败')
  }
}

const normalizeTitle = (title: string) => {
  if (!title) return '新会话'
  return title.length > 16 ? `${title.slice(0, 16)}...` : title
}

const hasRetrievalSources = (message: ChatMessage) =>
  (message.sources || []).length > 0

const openEvidence = (message: ChatMessage) => {
  const evidenceId = message.traceId || message.id
  const detail = {
    question: message.question,
    confidence: message.confidence,
    sources: message.sources || [],
    retrievalTrace: message.retrievalTrace
  }
  evidenceDetail.value = detail
  sessionStorage.setItem(
    `${EVIDENCE_STORAGE_PREFIX}${evidenceId}`,
    JSON.stringify(detail)
  )
  sessionStorage.setItem(EVIDENCE_STORAGE_KEY, JSON.stringify(detail))
  router.push({
    path: '/assistant/chat',
    query: { evidence: '1', evidenceId }
  })
}

const closeEvidence = () => {
  router.push('/assistant/chat')
}

const restoreEvidenceDetail = () => {
  const evidenceId = Array.isArray(route.query.evidenceId)
    ? route.query.evidenceId[0]
    : route.query.evidenceId
  const raw = evidenceId
    ? sessionStorage.getItem(`${EVIDENCE_STORAGE_PREFIX}${evidenceId}`)
    : sessionStorage.getItem(EVIDENCE_STORAGE_KEY)
  if (!raw) return
  try {
    evidenceDetail.value = JSON.parse(raw)
  } catch {
    evidenceDetail.value = {}
  }
}

const totalCandidates = (message: ChatMessage) =>
  message.retrievalTrace?.totalCandidates ?? (message.sources || []).length

const formatQuestionType = (type: string) => {
  const labels: Record<string, string> = {
    fact: '事实型',
    process: '流程型',
    list: '列表型',
    comparison: '比较型',
    condition: '条件型'
  }
  return labels[type] || type
}

const formatPercent = (value: number) => `${Math.round(value * 100)}%`

const formatScore = (value?: number) => {
  if (value === undefined || value === null) return '--'
  return value.toFixed(2)
}

const sourceKey = (source: AssistantSource) =>
  source.chunkId || `${source.sourceFile || 'source'}-${source.score || 0}`

const createId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`
</script>

<style lang="less" scoped>
.assistant-chat-page {
  min-height: calc(100vh - 96px);
  padding: 20px;
  background: #f4f7fb;
}

.chat-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 136px);
}

.history-panel,
.conversation-panel {
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
}

.history-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  background: #f7f9fc;
  border: 1px solid transparent;
  border-radius: 6px;
}

.history-item {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  height: 42px;
  padding: 8px 10px;
  color: #30445f;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.history-row.active {
  color: #0052d9;
  background: #eef5ff;
  border-color: #9cc2ff;
}

.history-row.active .history-item {
  color: #0052d9;
}

.history-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item small {
  flex: none;
  color: #7c8da6;
}

.conversation-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.conversation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5eaf3;
}

.conversation-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2d3d;
}

.conversation-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7a90;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 18px 20px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  height: 100%;
  min-height: 240px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6b7a90;
  text-align: center;
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1f2d3d;
}

.empty-state p {
  margin: 0;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  display: flex;
  width: 36px;
  height: 36px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-weight: 600;
  background: #0f7b6c;
  border-radius: 50%;
}

.message-row.assistant .avatar {
  background: #395886;
}

.message-bubble {
  max-width: min(720px, 78%);
  padding: 12px 14px;
  background: #f5f8fc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.message-row.user .message-bubble {
  color: #ffffff;
  background: #0052d9;
  border-color: #0052d9;
}

.message-content {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-extra {
  margin-top: 8px;
  font-size: 12px;
  color: #7c8da6;
}

.message-row.user .message-extra {
  color: rgba(255, 255, 255, 0.76);
}

.retrieval-panel {
  margin-top: 10px;
  font-size: 12px;
  color: #30445f;
  border-top: 1px solid #e2e8f0;
}

.retrieval-panel summary {
  padding-top: 8px;
  color: #0052d9;
  cursor: pointer;
  user-select: none;
}

.retrieval-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 8px;
  color: #6b7a90;
}

.source-group {
  margin-top: 10px;
}

.source-group-title {
  margin-bottom: 6px;
  font-weight: 600;
  color: #1f2d3d;
}

.source-item {
  padding: 8px;
  margin-bottom: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.source-item.selected {
  border-color: #9cc2ff;
}

.source-title {
  overflow: hidden;
  font-weight: 600;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  color: #7c8da6;
}

.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.source-tags span {
  padding: 1px 6px;
  color: #0052d9;
  background: #eef5ff;
  border-radius: 4px;
}

.source-snippet {
  margin-top: 6px;
  color: #4f5f75;
  line-height: 1.6;
  word-break: break-word;
}

.message-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  color: #c9353f;
}

.recommend-panel {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  overflow-x: auto;
  border-top: 1px solid #e5eaf3;
}

.recommend-panel button {
  flex: none;
  min-height: 32px;
  padding: 0 12px;
  color: #30445f;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #d8e1ee;
  border-radius: 16px;
}

.recommend-panel button:hover {
  color: #0052d9;
  border-color: #7aa7f7;
}

.recommend-panel button:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.input-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 12px;
  align-items: end;
  padding: 16px 20px 20px;
  border-top: 1px solid #e5eaf3;
}

@media (max-width: 900px) {
  .assistant-chat-page {
    padding: 12px;
  }

  .chat-shell {
    height: auto;
    grid-template-columns: 1fr;
  }

  .history-panel {
    display: none;
  }

  .conversation-panel {
    min-height: calc(100vh - 118px);
  }

  .message-bubble {
    max-width: 86%;
  }
}
</style>
