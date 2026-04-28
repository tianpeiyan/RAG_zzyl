<template>
  <div class="assistant-kb-page">
    <section class="status-band">
      <div class="status-main">
        <span class="label">索引状态</span>
        <strong>{{ statusText }}</strong>
        <p>{{ indexStatus.message || '知识库运行状态正常' }}</p>
      </div>
      <div class="status-metrics">
        <div>
          <span>{{ indexStatus.documentCount || 0 }}</span>
          <small>文档</small>
        </div>
        <div>
          <span>{{ indexStatus.chunkCount || 0 }}</span>
          <small>分片</small>
        </div>
        <div>
          <span>{{ indexStatus.lastIndexedAt || '-' }}</span>
          <small>最近索引</small>
        </div>
      </div>
      <div class="status-actions">
        <t-button theme="primary" :loading="rebuilding" @click="handleRebuild">
          重建索引
        </t-button>
        <t-button variant="outline" :loading="loading" @click="loadData">
          刷新
        </t-button>
      </div>
    </section>

    <section class="content-grid">
      <div class="document-panel">
        <div class="section-title">
          <h2>知识库文档</h2>
          <span>{{ documents.length }} 份</span>
        </div>
        <t-table
          row-key="sourceFile"
          :data="documents"
          :columns="columns"
          :loading="loading"
          :pagination="{ pageSize: 10, total: documents.length }"
        >
          <template #status="{ row }">
            <t-tag :theme="statusTheme(row.status)" variant="light">
              {{ normalizeStatus(row.status) }}
            </t-tag>
          </template>
        </t-table>
      </div>

      <aside class="failure-panel">
        <div class="section-title">
          <h2>失败记录</h2>
          <span>{{ failedTasks.length }} 条</span>
        </div>
        <div v-if="failedTasks.length === 0" class="empty-fail">暂无失败记录</div>
        <div v-for="task in failedTasks" :key="task.taskId || task.sourceFile" class="fail-item">
          <strong>{{ task.sourceFile || task.taskId }}</strong>
          <p>{{ task.reason || '处理失败' }}</p>
          <small>{{ task.occurredAt }}</small>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  KbDocument,
  KbFailedTask,
  KbIndexStatus,
  getKbDocuments,
  getKbIndexStatus,
  rebuildKbIndex
} from '@/api/assistant'

const loading = ref(false)
const rebuilding = ref(false)
const documents = ref<KbDocument[]>([])
const indexStatus = ref<Partial<KbIndexStatus>>({})

const columns = [
  { colKey: 'title', title: '文档标题', minWidth: 180 },
  { colKey: 'category', title: '分类', width: 120 },
  { colKey: 'sourceFile', title: '来源文件', minWidth: 180 },
  { colKey: 'updatedAt', title: '更新时间', width: 170 },
  { colKey: 'chunkCount', title: '切片数', width: 100 },
  { colKey: 'status', title: '状态', width: 110, cell: 'status' }
]

const failedTasks = computed<KbFailedTask[]>(() => indexStatus.value.failedTasks || [])

const statusText = computed(() => normalizeStatus(indexStatus.value.status))

const loadData = async () => {
  loading.value = true
  try {
    const [statusRes, documentRes]: any[] = await Promise.all([
      getKbIndexStatus(),
      getKbDocuments()
    ])
    if (statusRes.code !== 200) throw new Error(statusRes.msg || '索引状态获取失败')
    if (documentRes.code !== 200) throw new Error(documentRes.msg || '文档列表获取失败')
    indexStatus.value = statusRes.data || {}
    documents.value = documentRes.data || []
  } catch (error: any) {
    MessagePlugin.error(error?.message || '知识库数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleRebuild = async () => {
  rebuilding.value = true
  try {
    const res: any = await rebuildKbIndex()
    if (res.code !== 200) throw new Error(res.msg || '重建索引失败')
    MessagePlugin.success(res.data?.message || '已触发索引重建')
    await loadData()
  } catch (error: any) {
    MessagePlugin.error(error?.message || '重建索引失败')
  } finally {
    rebuilding.value = false
  }
}

const normalizeStatus = (status?: string) => {
  const statusMap = {
    ready: '可用',
    indexing: '重建中',
    rebuilding: '重建中',
    failed: '失败',
    empty: '未初始化'
  }
  return statusMap[status] || status || '未知'
}

const statusTheme = (status?: string) => {
  if (['ready', 'success'].includes(status || '')) return 'success'
  if (['indexing', 'rebuilding'].includes(status || '')) return 'warning'
  if (['failed', 'error'].includes(status || '')) return 'danger'
  return 'default'
}

onMounted(loadData)
</script>

<style lang="less" scoped>
.assistant-kb-page {
  min-height: calc(100vh - 96px);
  padding: 20px;
  background: #f4f7fb;
}

.status-band {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(360px, 1.2fr) auto;
  gap: 20px;
  align-items: center;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
}

.status-main .label {
  display: block;
  margin-bottom: 6px;
  color: #6b7a90;
}

.status-main strong {
  font-size: 24px;
  color: #1f2d3d;
}

.status-main p {
  margin: 8px 0 0;
  color: #6b7a90;
}

.status-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-metrics div {
  min-height: 68px;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #e6edf6;
  border-radius: 8px;
}

.status-metrics span {
  display: block;
  overflow: hidden;
  color: #0052d9;
  font-size: 18px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-metrics small {
  color: #6b7a90;
}

.status-actions {
  display: flex;
  gap: 10px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  margin-top: 16px;
}

.document-panel,
.failure-panel {
  min-width: 0;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2d3d;
}

.section-title span {
  color: #6b7a90;
}

.failure-panel {
  align-self: start;
}

.empty-fail {
  display: flex;
  min-height: 160px;
  align-items: center;
  justify-content: center;
  color: #7c8da6;
  background: #f7f9fc;
  border-radius: 8px;
}

.fail-item {
  padding: 12px 0;
  border-bottom: 1px solid #edf1f7;
}

.fail-item:last-child {
  border-bottom: 0;
}

.fail-item strong {
  color: #1f2d3d;
}

.fail-item p {
  margin: 6px 0;
  color: #c9353f;
  line-height: 1.5;
}

.fail-item small {
  color: #7c8da6;
}

@media (max-width: 1100px) {
  .status-band,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .status-actions {
    justify-content: flex-start;
  }
}
</style>
