# 中州养老智能助手 README

本项目的智能助手面向中州养老院工作人员，提供基于知识库的业务问答、系统操作咨询、命中依据查看、会话历史和知识库索引管理。功能由前端管理端、Java 后端和 Python RAG 服务三部分组成。

## 功能范围

- 智能问答：回答中州养老系统页面入口、业务办理流程、护理/入住/退住/收费/探望等运营服务问题。
- 知识库溯源：每次回答返回采用片段、丢弃片段、综合分、置信度、命中词和检索链路信息。
- 会话历史：按登录用户保存会话，支持加载历史消息和删除会话。
- 知识库管理：查看文档列表、索引状态、失败记录，并支持手动重建索引。
- RAG 防幻觉：仅依据知识库片段回答；无直接依据时拒答并提示补充文档后重建索引。

## 目录结构

```text
project-zzyl-admin-vue3-java/
  src/api/assistant.ts                    # 前端智能助手 API 封装
  src/pages/assistant/chat/index.vue      # 智能问答、命中详情、会话历史页面
  src/pages/assistant/kb/index.vue        # 知识库状态和重建页面

zzyl/
  zzyl-web/src/main/java/com/zzyl/assistant/
    client/AssistantPythonClient.java     # Java 调用 Python AI 服务
    config/AssistantProperties.java       # zzyl.ai 配置
    service/impl/AssistantServiceImpl.java# 问答、SSE、历史、审计编排
    vo/                                   # 问答、来源、知识库状态响应对象
  zzyl-web/src/main/resources/mapper/
    AssistantHistoryMapper.xml            # 会话历史持久化

zzyl-ai-service/
  api/routes/internal.py                  # FastAPI 问答和知识库接口
  rag/service.py                          # RAG 主流程、提示词、拒答策略
  rag/knowledge_base.py                   # 知识库扫描、分片、索引状态
  rag/chunker.py                          # FAQ / Markdown / 文档分片
  rag/query_rewrite.py                    # 查询改写
  retrievers/                             # 混合检索、向量库、rerank
  knowledge-base/                         # 知识库源文件
```

## 架构流程

```text
管理端 Vue
  -> /api/assistant/chat?stream=true
  -> Java zzyl-web /assistant/chat
  -> AssistantServiceImpl 读取用户、历史、审计
  -> AssistantPythonClient /internal/chat
  -> Python FastAPI
  -> Query Rewrite + 意图识别
  -> BM25 + 向量混合检索
  -> rerank + 片段过滤
  -> Tongyi/Qwen 生成答案
  -> 返回 answer、sources、confidence、retrievalTrace
```

前端通过 SSE 模拟流式输出；Java 侧负责鉴权、会话历史、审计和服务降级提示；Python 侧负责知识库索引、检索增强生成和回答可信度控制。

## 环境要求

- JDK 11
- Maven
- Node.js 与 Yarn
- Python 3.12
- uv
- MySQL、Redis
- 阿里云百炼 DashScope API Key

## Python AI 服务配置

进入 Python 服务目录：

```bash
cd zzyl-ai-service
cp .env.example .env
```

Windows PowerShell 可使用：

```powershell
Copy-Item .env.example .env
```

至少需要配置：

```env
AI_SERVICE_KNOWLEDGE_BASE_DIR=./knowledge-base
AI_SERVICE_CHROMA_PERSIST_DIR=./data/chroma
AI_SERVICE_BAILIAN_API_KEY=replace-with-api-key
AI_SERVICE_BAILIAN_CHAT_MODEL=qwen-plus
AI_SERVICE_BAILIAN_EMBEDDING_MODEL=tongyi-embedding-vision-plus-2026-03-06
AI_SERVICE_BAILIAN_RERANK_MODEL=qwen3-vl-rerank
AI_SERVICE_INTERNAL_API_KEY=replace-with-internal-key
```

`AI_SERVICE_INTERNAL_API_KEY` 是 Python 服务的必填运行配置；Java 调用 Python 内部接口时会通过 `X-Internal-Api-Key` 请求头传递 `zzyl.ai.internal-api-key`。

启动服务：

```bash
uv run uvicorn main:app --host 127.0.0.1 --port 8000
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
```

## Java 后端配置

配置文件位于 `zzyl/zzyl-web/src/main/resources/application.yml`：

```yaml
zzyl:
  ai:
    base-url: http://127.0.0.1:8000
    internal-api-key: replace-with-internal-key
    connect-timeout-millis: 3000
    read-timeout-millis: 30000
    max-retries: 1
```

启动 Java 后端前，需要保证 MySQL、Redis 和 Python AI 服务可用。

## 前端使用

进入前端目录：

```bash
cd project-zzyl-admin-vue3-java
yarn install --frozen-lockfile
yarn dev
```

登录管理端后使用：

- 智能问答页面：发送问题、查看流式答案、打开命中详情。
- 知识库页面：查看索引状态、文档列表和失败记录，必要时点击“重建索引”。

## 知识库维护

知识库源文件放在 `zzyl-ai-service/knowledge-base/`，当前支持：

- `.md`
- `.txt`
- `.pdf`
- `.docx`

分片规则：

- FAQ 文档按一问一答切分。
- Markdown 系统手册按标题章节切分，并保留章节路径。
- 其他文本类文档按配置的 `chunk_size` 和 `chunk_overlap` 切分。

新增、删除或修改知识库文件后，需要重建索引：

```bash
curl -X POST http://127.0.0.1:8000/assistant/kb/index/rebuild
```

也可以在管理端知识库页面点击“重建索引”。

## 主要接口

前端访问 Java 后端接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/assistant/chat` | 非流式问答 |
| POST | `/assistant/chat?stream=true` | SSE 流式问答 |
| GET | `/assistant/history/sessions` | 会话列表 |
| GET | `/assistant/history/sessions/{sessionId}/messages` | 会话消息 |
| DELETE | `/assistant/history/sessions/{sessionId}` | 删除会话 |
| GET | `/assistant/kb/documents` | 知识库文档 |
| GET | `/assistant/kb/index/status` | 索引状态 |
| POST | `/assistant/kb/index/rebuild` | 重建索引 |

Java 调用 Python 内部接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/internal/chat` | RAG 问答 |
| GET | `/internal/kb/documents` | 文档列表 |
| GET | `/internal/kb/status` | 索引状态 |
| POST | `/internal/kb/reindex` | 重建索引 |

## 回答可信度策略

智能助手不会直接把模型当作事实来源。Python RAG 服务会先做查询改写和意图识别，再通过混合检索召回候选片段，结合关键词覆盖、向量分、rerank 分、可回答性分和最终分筛选片段。

当命中不足或置信度过低时，助手会返回“知识库暂未找到直接依据”一类拒答结果，并在 `sources` 和 `retrievalTrace` 中保留候选片段、丢弃原因和检索过程，便于定位是文档缺失、索引过期还是问题不在业务范围内。

## 测试

Python RAG 服务测试：

```bash
cd zzyl-ai-service
uv run --with ".[test]" pytest
```

Java 智能助手相关测试：

```bash
cd zzyl
mvn -pl zzyl-web -DskipTests=false -Dtest=AssistantHistoryServiceTest test
```

前端构建检查：

```bash
cd project-zzyl-admin-vue3-java
yarn build
```

验证重点：

- 问答结果能根据知识库返回答案或合理拒答。
- 回答包含 `sources`、`confidence`、`retrievalTrace` 等命中详情。
- 历史会话按用户隔离保存。
- 知识库状态能识别索引正常、过期和文档读取失败。
- 新增知识库文件后，重建索引能更新文档数和分片数。

## 常见问题

### AI 服务未启动或地址不可达

检查 Python 服务是否在 `zzyl.ai.base-url` 配置的地址运行，并确认端口未被占用。

### 知识库索引已过期

通常是 `knowledge-base/` 下文件发生变化，但 Chroma 索引未同步。进入知识库页面点击“重建索引”，或调用 `/assistant/kb/index/rebuild`。

### 回答提示暂无依据

先在命中详情查看候选片段和丢弃原因。常见原因是知识库缺少对应业务文档、用户问题不在中州养老业务范围内、片段关键词覆盖不足或可回答性分数过低。

### 配置加载失败并提示缺少 AI_SERVICE_INTERNAL_API_KEY

复制 `.env.example` 后补齐 `AI_SERVICE_INTERNAL_API_KEY`。Java 侧也应配置 `zzyl.ai.internal-api-key`，便于内部调用链路统一传递服务间认证头。

### 模型或检索服务异常

检查百炼 API Key、模型名称、网络访问和 Chroma 持久化目录。若 Chroma 数据损坏且配置允许自动重置，服务会尝试重建索引；否则需要清理持久化目录后重新构建。
