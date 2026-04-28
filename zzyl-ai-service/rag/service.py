from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Any

from config.settings import Settings
from rag.intent import Intent, QuestionType, classify_intent
from rag.knowledge_base import IndexedDocument, KnowledgeBaseService, RebuildSummary
from rag.query_rewrite import QueryRewriteService
from retrievers import RetrievedChunk, VectorStoreService


class RagServiceError(RuntimeError):
    """Raised when the LangChain RAG service cannot complete a request."""


@dataclass(frozen=True)
class RagSource:
    document_title: str | None = None
    category: str | None = None
    source_file: str | None = None
    chunk_id: str | None = None
    score: float | None = None
    selected: bool = False
    reason: str | None = None
    matched_terms: list[str] | None = None
    score_breakdown: dict[str, float | None] | None = None
    snippet: str | None = None
    content: str | None = None


@dataclass(frozen=True)
class RetrievalTrace:
    total_candidates: int = 0
    selected_count: int = 0
    discarded_count: int = 0
    rewritten_query: str | None = None
    expanded_queries: list[str] | None = None
    required_terms: list[str] | None = None


@dataclass(frozen=True)
class RagChatResult:
    answer: str
    sources: list[RagSource]
    question_type: str | None = None
    confidence: float = 0.0
    retrieval_trace: RetrievalTrace | None = None
    refused: bool = False


RAG_ASSISTANT_SYSTEM_PROMPT = """你是面向中州养老院工作人员的 RAG 智能助手，负责基于知识库回答问题。

可回答范围：
1. 中州养老系统的页面入口、系统功能、操作流程、业务办理规则。
2. 养老院入住、退住、护理、收费、探望、餐食、医疗配套等运营服务问题。
3. 智能助手、知识库、索引、命中详情、置信度、参考依据等 RAG 问答使用与运维问题。

证据采用规则：
1. 只能依据“参考资料”回答，不使用常识补全知识库没有给出的业务事实。
2. 只要参考资料中存在能直接或等价回答用户意图的片段，就应提炼回答；不要因为用户措辞与原文标题不完全一致而拒答。
3. 多个片段同时出现时，仅采用最能覆盖用户问题对象和动作的片段；任何与用户问题对象无关的片段一律忽略，不得混入答案。
4. 历史对话只用于理解省略指代，不能覆盖或替代参考资料。
5. 参考资料缺少关键条件、步骤或事实时，明确说明“知识库暂未找到直接依据”，并指出缺少哪类依据；不要编造。
6.严格匹配用户提问的业务主体，用户仅询问退住相关内容时，只输出退住对应信息，完全剔除、禁止提及入住相关流程与规则；询问单一业务时，不得混入其他无关业务模块内容。
7.生成回答时，只围绕用户核心问题做精准回应，不做业务拓展、不补充同类其他业务流程、不做全流程兜底式说明。
8.若单条参考资料混合多个业务内容，仅抽取目标问题对应段落，其余内容直接废弃，绝不体现在最终回答中。

回答要求：
1. 保持简洁、专业、可执行，优先直接回答用户问题。
2. 事实型直接给结论；流程/处理型按步骤编号；列表型逐项列出；比较型按差异点说明；条件型先列条件再补充注意事项。
3. 不输出 Markdown 加粗符号或星号，不重复编号参考资料中的“流程如下”等引导语。
4. 如参考资料给出页面、按钮、路径、接口、索引动作等操作信息，保留这些关键信息。
5. 严格禁止输出参考资料中未提及的任何内容。每条输出必须能在参考资料中找到原文直接对应。输出前逐句自检：该句信息是否直接来自参考资料？如否，删除该句。"""


class RagService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.vector_service = VectorStoreService(settings)
        self.knowledge_base = KnowledgeBaseService(settings, self.vector_service)
        self.query_rewriter = QueryRewriteService(settings)
        self.chain = self._build_chain()

    def chat(
        self,
        question: str,
        session_id: str,
        history: list[dict[str, str]] | None = None,
    ) -> RagChatResult:
        rewrite_result = self.query_rewriter.rewrite(question)
        retrieval_question = rewrite_result.rewritten_question or question
        intent = classify_intent(retrieval_question)
        history_messages = self._to_history_messages(history, question)
        if intent.intent == Intent.SMALL_TALK:
            return RagChatResult(
                answer=self._chat_without_retrieval(question, history_messages),
                sources=[],
                question_type=None,
                confidence=1.0,
                retrieval_trace=RetrievalTrace(),
            )
        if intent.intent == Intent.OUT_OF_SCOPE:
            return RagChatResult(
                answer="抱歉，我只能回答与中州养老系统使用流程、系统功能、养老院运营服务相关的问题。",
                sources=[],
                question_type=None,
                confidence=0.0,
                retrieval_trace=RetrievalTrace(),
                refused=True,
            )

        self.ensure_index()
        expanded_queries = [
            *rewrite_result.expanded_queries,
            *(intent.expanded_queries or []),
        ]
        candidates = self.vector_service.hybrid_search(
            retrieval_question,
            expanded_queries=expanded_queries,
            required_terms=intent.required_terms,
        )
        selected_chunks = [chunk for chunk in candidates if chunk.selected]
        retrieval_trace = RetrievalTrace(
            total_candidates=len(candidates),
            selected_count=len(selected_chunks),
            discarded_count=max(0, len(candidates) - len(selected_chunks)),
            rewritten_query=retrieval_question if retrieval_question != question else None,
            expanded_queries=[item for item in dict.fromkeys(expanded_queries) if item] or [question],
            required_terms=intent.required_terms or [],
        )
        if not candidates:
            return RagChatResult(
                answer="知识库中暂未检索到相关依据，请补充相关养老院文档后重建索引。",
                sources=[],
                question_type=(intent.question_type or QuestionType.FACT).value,
                confidence=0.0,
                retrieval_trace=retrieval_trace,
                refused=True,
            )

        confidence = self._confidence(selected_chunks)
        question_type = intent.question_type or QuestionType.FACT
        if confidence < 0.32 or not selected_chunks:
            return RagChatResult(
                answer="暂未找到能直接回答该问题的知识库依据，请补充更明确的业务文档后重建索引。",
                sources=[self._to_source(chunk) for chunk in candidates],
                question_type=question_type.value,
                confidence=confidence,
                retrieval_trace=retrieval_trace,
                refused=True,
            )

        context = self._format_chunks(selected_chunks)
        try:
            answer = self.chain.invoke(
                {
                    "input": question,
                    "context": context,
                    "history": history_messages,
                    "question_type": self._question_type_label(question_type),
                }
            )
            answer = self._normalize_answer(answer, question_type)
        except Exception:
            if not self.settings.llm_fallback_to_context:
                raise
            answer = self._answer_from_context(question_type, selected_chunks)
        return RagChatResult(
            answer=answer,
            sources=[self._to_source(chunk) for chunk in candidates],
            question_type=question_type.value,
            confidence=confidence,
            retrieval_trace=retrieval_trace,
        )

    def ensure_index(self) -> None:
        if not self.knowledge_base.index_is_current():
            self.knowledge_base.rebuild()

    def rebuild_index(self) -> RebuildSummary:
        return self.knowledge_base.rebuild()

    def documents(self) -> list[IndexedDocument]:
        return self.knowledge_base.documents()

    def chunk_count(self) -> int:
        return self.vector_service.count()

    def _build_chain(self) -> Any:
        from langchain_community.chat_models import ChatTongyi
        from langchain_core.output_parsers import StrOutputParser
        from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

        prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    RAG_ASSISTANT_SYSTEM_PROMPT
                    + "\n\n问题类型: {question_type}"
                    + "\n\n参考资料:\n{context}",
                ),
                ("system", "以下是历史对话记录:"),
                MessagesPlaceholder("history"),
                ("human", "{input}"),
            ]
        )
        chat_model = ChatTongyi(
            model=self.settings.bailian_chat_model,
            api_key=self.settings.bailian_api_key,
        )
        base_chain = prompt | chat_model | StrOutputParser()
        return base_chain

    def _format_chunks(self, chunks: list[RetrievedChunk]) -> str:
        if not chunks:
            return "无相关参考资料"

        formatted_chunks: list[str] = []
        for index, chunk in enumerate(chunks, start=1):
            source_file = chunk.metadata.get("sourceFile") or chunk.metadata.get("source")
            formatted_chunks.append(
                "\n".join(
                    [
                        f"[片段{index}]",
                        f"来源: {source_file or '未知'}",
                        f"标题: {chunk.metadata.get('documentTitle') or '未命名'}",
                        f"内容: {chunk.page_content}",
                    ]
                )
            )
        return "\n\n".join(formatted_chunks)

    def _to_source(self, chunk: RetrievedChunk) -> RagSource:
        return RagSource(
            document_title=chunk.metadata.get("documentTitle"),
            category=chunk.metadata.get("category"),
            source_file=chunk.metadata.get("sourceFile") or chunk.metadata.get("source"),
            chunk_id=chunk.metadata.get("chunkId"),
            score=chunk.score,
            selected=chunk.selected,
            reason=chunk.reason,
            matched_terms=chunk.matched_terms or [],
            score_breakdown={
                "vectorScore": chunk.vector_score,
                "rerankScore": chunk.rerank_score,
                "answerabilityScore": round(chunk.answerability_score, 4),
                "keywordScore": round(chunk.keyword_score, 4),
                "finalScore": round(chunk.final_score, 4),
            },
            snippet=self._snippet(chunk.page_content),
            content=chunk.page_content,
        )

    def _answer_from_context(self, question_type: QuestionType, chunks: list[RetrievedChunk]) -> str:
        if not chunks:
            return "知识库中暂未检索到相关依据，请先上传或重建相关文档索引。"

        if question_type == QuestionType.PROCESS:
            lines = ["根据知识库依据，流程可按以下步骤处理："]
        elif question_type == QuestionType.LIST:
            lines = ["根据知识库依据，可归纳为："]
        elif question_type == QuestionType.COMPARISON:
            lines = ["根据知识库依据，差异点如下："]
        elif question_type == QuestionType.CONDITION:
            lines = ["根据知识库依据，需要关注以下条件："]
        else:
            lines = ["根据知识库依据，答案如下："]

        extracted_lines: list[str] = []
        for chunk in chunks[:3]:
            content = self._extract_answer_body(chunk.page_content)
            extracted_lines.extend(self._format_answer_items(content, question_type))

        if not extracted_lines:
            extracted_lines = [self._trim_answer(self._clean_answer_text(self._extract_answer_body(chunks[0].page_content)))]

        if question_type in {QuestionType.PROCESS, QuestionType.LIST, QuestionType.CONDITION}:
            lines.extend(
                f"{index}. {item}"
                for index, item in enumerate(dict.fromkeys(extracted_lines), start=1)
                if item
            )
        else:
            lines.extend(item for item in dict.fromkeys(extracted_lines) if item)
        return self._normalize_answer("\n".join(lines), question_type)

    def _confidence(self, chunks: list[RetrievedChunk]) -> float:
        if not chunks:
            return 0.0
        top_score = max(chunk.final_score for chunk in chunks)
        avg_keyword = sum(chunk.keyword_score for chunk in chunks) / len(chunks)
        avg_answerability = sum(chunk.answerability_score for chunk in chunks) / len(chunks)
        matched_bonus = 0.08 if any(chunk.matched_terms for chunk in chunks) else 0.0
        return round(
            min(1.0, top_score * 0.55 + avg_answerability * 0.25 + avg_keyword * 0.2 + matched_bonus),
            2,
        )

    def _snippet(self, content: str) -> str:
        normalized = " ".join(content.split())
        return f"{normalized[:180]}..." if len(normalized) > 180 else normalized

    def _question_type_label(self, question_type: QuestionType) -> str:
        return {
            QuestionType.FACT: "事实型",
            QuestionType.PROCESS: "流程型",
            QuestionType.LIST: "列表型",
            QuestionType.COMPARISON: "比较型",
            QuestionType.CONDITION: "条件型",
        }[question_type]

    def _normalize_answer(self, answer: str, question_type: QuestionType) -> str:
        cleaned = self._clean_answer_text(answer)
        if question_type in {QuestionType.PROCESS, QuestionType.LIST, QuestionType.CONDITION}:
            cleaned = self._normalize_numbered_lines(cleaned)
        cleaned = re.sub(r"\n{3,}", "\n\n", cleaned).strip()
        return cleaned

    def _extract_answer_body(self, content: str) -> str:
        normalized = content.strip()
        if "答：" in normalized:
            return normalized.split("答：", 1)[1].strip()
        if "\nA:" in normalized:
            return normalized.split("\nA:", 1)[1].strip()
        return normalized

    def _format_answer_items(self, content: str, question_type: QuestionType) -> list[str]:
        text = self._clean_answer_text(content)
        if question_type == QuestionType.PROCESS:
            text = self._drop_process_lead_line(text)
        text = self._normalize_numbered_lines(text)
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        lines = [line for line in lines if not line.startswith("章节：")]
        if len(lines) == 1:
            lines = [item.strip() for item in re.split(r"[；;]\s*", lines[0]) if item.strip()]
        return [self._trim_answer(self._strip_list_marker(line)) for line in lines if line.strip()]

    def _clean_answer_text(self, text: str) -> str:
        cleaned = text.replace("\r\n", "\n").replace("\r", "\n")
        cleaned = re.sub(r"\*\*(.*?)\*\*", r"\1", cleaned)
        cleaned = cleaned.replace("**", "").replace("__", "")
        cleaned = re.sub(r"^\s{0,3}#{1,6}\s*", "", cleaned, flags=re.MULTILINE)
        cleaned = re.sub(r"[ \t]+", " ", cleaned)
        return cleaned.strip()

    def _drop_process_lead_line(self, text: str) -> str:
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        while lines and re.fullmatch(r"(?:[\d一二三四五六七八九十]+[.、]\s*)?.{0,16}流程(?:如下|是怎样的|为|包括)?[:：]?", lines[0]):
            lines.pop(0)
        return "\n".join(lines)

    def _normalize_numbered_lines(self, text: str) -> str:
        normalized = re.sub(r"\s+(\d+[.、])\s*", r"\n\1 ", text)
        normalized = re.sub(r"\s+([（(]?\d+[）)])\s*", r"\n\1 ", normalized)
        normalized = re.sub(r"\n+", "\n", normalized)
        return normalized.strip()

    def _strip_list_marker(self, text: str) -> str:
        return re.sub(r"^\s*(?:[-*•]\s*|\d+[.、]\s*|[（(]?\d+[）)]\s*)", "", text).strip()

    def _trim_answer(self, text: str) -> str:
        normalized = " ".join(text.split())
        return f"{normalized[:220]}..." if len(normalized) > 220 else normalized

    def _chat_without_retrieval(self, question: str, history_messages: list[Any] | None = None) -> str:
        try:
            from langchain_community.chat_models import ChatTongyi
            from langchain_core.output_parsers import StrOutputParser
            from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

            prompt = ChatPromptTemplate.from_messages(
                [
                    (
                        "system",
                        "你是面向中州养老院工作人员的智能助手。用户在进行寒暄时，请礼貌、简洁回应，"
                        "并自然提示可咨询中州养老系统功能、系统操作流程、入住、退住、收费、护理、探望等问题。",
                    ),
                    MessagesPlaceholder("history"),
                    ("human", "{input}"),
                ]
            )
            chat_model = ChatTongyi(
                model=self.settings.bailian_chat_model,
                api_key=self.settings.bailian_api_key,
            )
            return (prompt | chat_model | StrOutputParser()).invoke(
                {"input": question, "history": history_messages or []}
            )
        except Exception:
            return "您好，我是中州养老智能助手。您可以咨询系统功能、系统操作流程、入住、退住、收费、护理、探望等问题。"

    def _to_history_messages(
        self,
        history: list[dict[str, str]] | None,
        question: str,
    ) -> list[Any]:
        if not history:
            return []
        from langchain_core.messages import AIMessage, HumanMessage

        normalized = [
            item
            for item in history[-20:]
            if isinstance(item, dict) and item.get("role") in {"user", "assistant"} and item.get("content")
        ]
        if normalized and normalized[-1].get("role") == "user" and normalized[-1].get("content") == question:
            normalized = normalized[:-1]

        messages: list[Any] = []
        for item in normalized:
            content = str(item["content"])
            if item["role"] == "user":
                messages.append(HumanMessage(content=content))
            else:
                messages.append(AIMessage(content=content))
        return messages


_SERVICE_CACHE: dict[int, RagService] = {}


def get_rag_service(settings: Settings) -> RagService:
    cache_key = id(settings)
    if cache_key in _SERVICE_CACHE:
        return _SERVICE_CACHE[cache_key]
    try:
        service = RagService(settings)
    except ImportError as exc:
        raise RagServiceError(
            "LangChain 依赖未安装，请先安装 pyproject.toml 中的 langchain 相关依赖。"
        ) from exc
    except Exception as exc:
        raise RagServiceError(f"LangChain RAG 服务初始化失败: {exc}") from exc
    _SERVICE_CACHE[cache_key] = service
    return service


def clear_rag_service_cache() -> None:
    _SERVICE_CACHE.clear()
