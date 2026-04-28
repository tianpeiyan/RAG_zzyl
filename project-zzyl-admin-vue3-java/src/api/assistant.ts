import { request } from '@/utils/request'

export interface AssistantMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AssistantChatParams {
  sessionId?: string
  question: string
  history?: AssistantMessage[]
}

export interface AssistantSource {
  documentTitle?: string
  category?: string
  sourceFile?: string
  chunkId?: string
  score?: number
  selected?: boolean
  reason?: string
  matchedTerms?: string[]
  scoreBreakdown?: {
    vectorScore?: number
    rerankScore?: number
    answerabilityScore?: number
    keywordScore?: number
    finalScore?: number
  }
  snippet?: string
  content?: string
}

export interface AssistantRetrievalTrace {
  totalCandidates?: number
  selectedCount?: number
  discardedCount?: number
  rewrittenQuery?: string
  expandedQueries?: string[]
  requiredTerms?: string[]
}

export interface AssistantChatResult {
  sessionId?: string
  answer: string
  status?: string
  refused?: boolean
  hitCount?: number
  traceId?: string
  sources?: AssistantSource[]
  questionType?: string
  confidence?: number
  retrievalTrace?: AssistantRetrievalTrace
}

export interface AssistantSession {
  sessionId: string
  title: string
  messageCount: number
  questionCount: number
  updatedAt: string
}

export interface AssistantHistoryMessage {
  id: number
  sessionId: string
  role: 'user' | 'assistant'
  content: string
  traceId?: string
  hitCount?: number
  refused?: boolean
  status?: string
  sources?: AssistantSource[]
  questionType?: string
  confidence?: number
  retrievalTrace?: AssistantRetrievalTrace
  createTime: string
}

export interface KbDocument {
  title: string
  category: string
  sourceFile: string
  updatedAt: string
  status: string
  chunkCount: number
  errorMessage?: string
}

export interface KbFailedTask {
  taskId: string
  sourceFile: string
  reason: string
  occurredAt: string
}

export interface KbIndexStatus {
  status: string
  rebuilding: boolean
  documentCount: number
  chunkCount: number
  lastIndexedAt: string
  message?: string
  failedTasks?: KbFailedTask[]
}

export interface KbRebuildResult {
  taskId?: string
  status: string
  message?: string
}

export function chatWithAssistant(params: AssistantChatParams) {
  return request.post({
    url: '/assistant/chat',
    data: params
  })
}

export interface AssistantStreamCallbacks {
  onDelta?: (delta: string) => void
  onDone?: (data: AssistantChatResult) => void
  onError?: (message: string) => void
}

export async function chatWithAssistantStream(
  params: AssistantChatParams,
  callbacks: AssistantStreamCallbacks
) {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const response = await fetch('/api/assistant/chat?stream=true', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      Accept: 'text/event-stream',
      ...(user?.token ? { Authorization: user.token } : {})
    },
    body: JSON.stringify(params)
  })

  if (!response.ok || !response.body) {
    throw new Error(`智能助手暂不可用，状态码 ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const handleEvent = (rawEvent: string) => {
    const dataLine = rawEvent
      .split('\n')
      .find((line) => line.startsWith('data:'))
    if (!dataLine) return
    const payload = JSON.parse(dataLine.slice(5).trim())
    if (payload.type === 'delta') callbacks.onDelta?.(payload.delta || '')
    if (payload.type === 'done') callbacks.onDone?.(payload.data || {})
    if (payload.type === 'error')
      callbacks.onError?.(payload.message || '智能助手暂不可用')
  }

  // eslint-disable-next-line no-constant-condition
  while (true) {
    // eslint-disable-next-line no-await-in-loop
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''
    events.filter(Boolean).forEach(handleEvent)
  }
  if (buffer.trim()) handleEvent(buffer)
}

export function getAssistantSessions() {
  return request.get({
    url: '/assistant/history/sessions'
  })
}

export function getAssistantMessages(sessionId: string) {
  return request.get({
    url: `/assistant/history/sessions/${sessionId}/messages`
  })
}

export function deleteAssistantSession(sessionId: string) {
  return request.delete({
    url: `/assistant/history/sessions/${sessionId}`
  })
}

export function getKbDocuments() {
  return request.get({
    url: '/assistant/kb/documents'
  })
}

export function getKbIndexStatus() {
  return request.get({
    url: '/assistant/kb/index/status'
  })
}

export function rebuildKbIndex() {
  return request.post({
    url: '/assistant/kb/index/rebuild'
  })
}
