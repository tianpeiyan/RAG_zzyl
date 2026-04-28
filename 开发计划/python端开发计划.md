**Python 端开发清单**（每开发完一个任务，做好标记，防止重复开发）
新建独立目录：d:\code\test\zzyl-ai-service

1. 任务 P1：初始化 AI 服务工程

   目标：让服务可以本地启动并可扩展。

   执行项：

   - 建立目录：api/、config/、rag/、loaders/、retrievers/、models/、jobs/、tests/
   - 接入基础配置、日志、健康检查
   - 约定 .env 配置项

   产出：

   - 可启动服务
   - 配置模板

   验收指标：

   - 服务启动成功率 100%
   - 健康检查接口连续 10 次返回成功
   - 配置项缺失时能明确报错，不允许静默失败

2. 任务 P2：封装通义 LLM 与 Embedding

   目标：统一模型调用层，后续便于替换。

   执行项：

   - 封装聊天模型客户端
   - 封装 embedding 客户端
   - 统一超时、重试、异常处理

   产出：

   - models/llm_provider.py
   - models/embedding_provider.py

   验收指标：

   - 聊天调用测试成功率 100%
   - embedding 调用测试成功率 100%
   - 模型超时时返回标准错误对象

3. 任务 P3：实现知识文档读取

   目标：支持pdf/docx/md/txt四类文档。

   执行项：

   - 建立统一 Document 数据结构
   - 按文件类型实现 loader
   - 记录文件级解析状态

   产出：

   loaders/ 下多格式解析器
   验收指标：

   - 四类格式都至少通过 2 个样本
   - 首批文档解析成功率 >= 95%
   - 损坏文档会进入失败清单

4. 任务 P4：实现文本清洗与切片

   目标：提升召回质量。

   执行项：

   - 清理页眉页脚、空白、乱码
   - 按标题和段落切片
   - 配置 chunk_size、chunk_overlap

   产出：

   - rag/text_cleaner.py
   - rag/chunker.py

   验收指标：

   - 乱码切片占比 < 2%
   - 过长切片占比 < 5%
   - 抽查 10 个切片均可追溯到源文档

5. 任务 P5：接入 Chroma 持久化

   目标：完成索引落地。

   执行项：

   - 写入向量与元数据
   - 支持全量重建
   - 支持获取文档列表和索引状态

   产出：

   - rag/vector_store.py
   - jobs/reindex_job.py

   验收指标：

   - 全量重建成功率 100%
   - 文档数、切片数、向量条目可统计
   - 重建后随机 5 个问题能召回相关片段

6. 任务 P6：实现检索链路

   目标：让问题先找到对的内容。

   执行项：

   - 相似度检索
   - 关键词兜底召回
   - 最低分阈值与召回条数控制

   产出：

   retrievers/hybrid_retriever.py
   验收指标：

   - 30 条标准问题 Top-K 命中率 >= 80%
   - 无答案问题不会高频误召回

7. 任务 P7：实现回答生成与拒答

   目标：控制幻觉，优先可靠。

   执行项：

   - 设计“知识问答”提示词
   - 设计“系统导航”提示词
   - 增加低置信度拒答

   产出：

   - rag/chat_service.py
   - rag/prompts.py

   验收指标：

   - 30 条问题整体正确率 >= 70%，首轮即可
   - 无答案问题拒答率 >= 80%
   - 明显幻觉回答 <= 3 条

8. 任务 P8：开放内部接口

   目标：供 Java 统一接入。

   执行项：

   - POST /internal/chat
   - POST /internal/kb/reindex
   - GET /internal/kb/status
   - GET /internal/kb/documents

   产出：

   - 内部 API 文档
     验收指标：
   - 四个接口联调通过率 100%
   - 返回结构统一，错误码统一

9. 任务 P9：补齐日志与测试

   目标：可调优、可回溯。

   执行项：

   - 记录问题、召回片段、分数、耗时、回答状态
   - 为 loader、chunker、retriever、chat_service 写单测
   
   产出：
   
   - 日志规范
   - Python 单测
   
   验收指标：
   
   - 问答日志覆盖率 100%
   - 核心单测通过率 100%
