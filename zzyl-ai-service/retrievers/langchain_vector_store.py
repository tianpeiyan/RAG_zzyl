from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime
import math
from pathlib import Path
import re
import shutil
from typing import Any

from config.settings import Settings
from retrievers.dashscope_multimodal_embeddings import DashScopeMultiModalEmbeddings
from retrievers.json_vector_store import JsonVectorStore
from retrievers.local_embeddings import LocalHashEmbeddings


LOW_SIGNAL_TERMS = {
    "养老",
    "老人",
    "院区",
    "服务",
    "系统",
    "功能",
    "康养",
    "哪些",
    "有哪",
    "包括",
    "列表",
    "什么",
    "怎么",
    "如何",
    "是否",
    "可以",
    "查看",
}

LOCATION_QUERY_TERMS = {"位置", "地址", "院址", "哪里", "哪儿", "在哪"}
LOCATION_EVIDENCE_TERMS = {"地址", "院址", "位于", "所在", "位置", "坐落", "路线"}
CONFIG_QUERY_TERMS = {"配置", "设置", "维护"}
CONFIG_EVIDENCE_TERMS = {"配置", "设置", "维护", "页面", "菜单", "入口"}
PROCESS_QUERY_TERMS = {"流程", "步骤", "办理", "处理"}
PROCESS_EVIDENCE_TERMS = {"流程", "步骤", "办理", "处理", "方式", "怎么办", "申请", "提交", "审核", "确认"}
DATA_QUERY_TERMS = {"数据", "指标", "信息"}
DATA_EVIDENCE_TERMS = {"数据", "指标", "信息", "状态", "趋势", "提醒", "告警"}
FEATURE_MODULE_QUERY_TERMS = {"功能模块"}
FEATURE_MODULE_EVIDENCE_TERMS = {"功能模块", "核心功能模块", "核心业务模块"}


@dataclass(frozen=True)
class RetrievedChunk:
    page_content: str
    metadata: dict[str, Any]
    score: float | None = None
    vector_score: float | None = None
    rerank_score: float | None = None
    answerability_score: float = 0.0
    keyword_score: float = 0.0
    final_score: float = 0.0
    matched_terms: list[str] | None = None
    selected: bool = False
    reason: str | None = None


class VectorStoreService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.embedding = self._create_embedding()
        self.vector_store = self._create_vector_store()
        self.using_json_store = isinstance(self.vector_store, JsonVectorStore)

    def get_retriever(self) -> Any:
        if self.using_json_store:
            raise RuntimeError("JsonVectorStore does not expose a LangChain retriever")
        return self.vector_store.as_retriever(
            search_kwargs={"k": self.settings.retrieval_top_k}
        )

    def add_documents(self, documents: list[Any]) -> None:
        batch_size = max(1, self.settings.embedding_batch_size)
        for start in range(0, len(documents), batch_size):
            batch = documents[start : start + batch_size]
            if batch:
                self.vector_store.add_documents(batch)

    def similarity_search(self, query: str) -> list[RetrievedChunk]:
        search_k = max(self.settings.retrieval_top_k, self.settings.rerank_candidate_k)
        return self._rerank(
            query,
            self._vector_search(query, search_k),
            self.settings.retrieval_top_k,
        )[: self.settings.retrieval_top_k]

    def hybrid_search(
        self,
        query: str,
        expanded_queries: list[str] | None = None,
        required_terms: list[str] | None = None,
    ) -> list[RetrievedChunk]:
        queries = [item for item in dict.fromkeys([query, *(expanded_queries or [])]) if item]
        search_k = max(self.settings.retrieval_top_k, self.settings.rerank_candidate_k)
        required = list(dict.fromkeys([term for term in required_terms or [] if term]))

        merged: dict[str, RetrievedChunk] = {}
        for current_query in queries:
            for chunk in self._vector_search(current_query, search_k):
                key = self._chunk_key(chunk)
                previous = merged.get(key)
                if previous is None or (chunk.score or 0.0) > (previous.vector_score or 0.0):
                    merged[key] = RetrievedChunk(
                        page_content=chunk.page_content,
                        metadata=chunk.metadata,
                        score=chunk.score,
                        vector_score=chunk.score,
                    )

        keyword_terms = self._query_terms(" ".join([query, *queries, *required]))
        for chunk in self._keyword_candidates(keyword_terms, required):
            key = self._chunk_key(chunk)
            previous = merged.get(key)
            if previous is None:
                merged[key] = chunk
            else:
                merged[key] = RetrievedChunk(
                    page_content=previous.page_content,
                    metadata=previous.metadata,
                    score=previous.score,
                    vector_score=previous.vector_score,
                    keyword_score=max(previous.keyword_score, chunk.keyword_score),
                    final_score=previous.final_score,
                    matched_terms=sorted(
                        set(previous.matched_terms or []) | set(chunk.matched_terms or []),
                        key=len,
                        reverse=True,
                    ),
                )

        scored = [
            self._score_hybrid_chunk(chunk, keyword_terms, required)
            for chunk in merged.values()
        ]
        scored.sort(key=lambda item: item.final_score, reverse=True)
        reranked = self._rerank_hybrid_candidates(query, scored)
        answerable = [
            self._score_answerability(chunk, query, keyword_terms, required)
            for chunk in reranked
        ]
        answerable.sort(key=lambda item: item.final_score, reverse=True)
        return self._mark_selection(answerable, query, required)

    def _vector_search(self, query: str, search_k: int) -> list[RetrievedChunk]:
        if self.using_json_store:
            results = self.vector_store.similarity_search(
                query,
                k=search_k,
                min_score=self.settings.retrieval_min_score,
            )
            chunks = [
                RetrievedChunk(
                    page_content=doc.page_content,
                    metadata=dict(doc.metadata),
                    score=float(score),
                    vector_score=float(score),
                )
                for doc, score in results
            ]
            return chunks

        try:
            results = self.vector_store.similarity_search_with_relevance_scores(
                query,
                k=search_k,
            )
        except Exception:
            docs = self.get_retriever().invoke(query)
            chunks = [
                RetrievedChunk(
                    page_content=doc.page_content,
                    metadata=dict(doc.metadata),
                    score=None,
                    vector_score=None,
                )
                for doc in docs
            ]
            return chunks

        chunks: list[RetrievedChunk] = []
        for doc, score in results:
            if score is not None and score < self.settings.retrieval_min_score:
                continue
            chunks.append(
                RetrievedChunk(
                    page_content=doc.page_content,
                    metadata=dict(doc.metadata),
                    score=float(score) if score is not None else None,
                    vector_score=float(score) if score is not None else None,
                )
            )
        return chunks

    def count(self) -> int:
        if self.using_json_store:
            return self.vector_store.count()
        collection = getattr(self.vector_store, "_collection", None)
        if collection is None:
            return 0
        try:
            return int(collection.count())
        except Exception:
            return 0

    def indexed_document_hashes(self) -> dict[str, str]:
        metadatas = self._all_metadatas()
        hashes: dict[str, str] = {}
        for metadata in metadatas:
            source_file = metadata.get("sourceFile") or metadata.get("source")
            content_hash = metadata.get("contentHash")
            if source_file and content_hash:
                hashes[str(source_file)] = str(content_hash)
        return hashes

    def reset(self) -> None:
        if self.using_json_store:
            self.vector_store.reset()
            return
        try:
            self.vector_store.delete_collection()
        except Exception:
            pass
        self.vector_store = self._create_vector_store()
        self.using_json_store = isinstance(self.vector_store, JsonVectorStore)

    def _create_embedding(self) -> Any:
        if self.settings.embedding_provider.lower() == "local":
            return LocalHashEmbeddings(self.settings.local_embedding_dimensions)

        if self.settings.embedding_provider.lower() in {"dashscope-multimodal", "multimodal"}:
            return DashScopeMultiModalEmbeddings(
                model=self.settings.bailian_embedding_model,
                api_key=self.settings.bailian_api_key,
                dimension=self.settings.bailian_embedding_dimension,
            )

        from langchain_community.embeddings import DashScopeEmbeddings

        return DashScopeEmbeddings(
            model=self.settings.bailian_embedding_model,
            dashscope_api_key=self.settings.bailian_api_key,
        )

    def _rerank(
        self,
        query: str,
        chunks: list[RetrievedChunk],
        top_n: int | None = None,
    ) -> list[RetrievedChunk]:
        if not chunks:
            return []
        if not self.settings.rerank_enabled:
            return chunks[: (top_n or self.settings.retrieval_top_k)]
        from retrievers.rerank import DashScopeReranker

        return DashScopeReranker(self.settings).rerank(query, chunks, top_n=top_n)

    def _rerank_hybrid_candidates(
        self,
        query: str,
        chunks: list[RetrievedChunk],
    ) -> list[RetrievedChunk]:
        if not chunks:
            return []
        candidate_limit = min(
            len(chunks),
            max(self.settings.rerank_candidate_k, self.settings.retrieval_top_k),
        )
        reranked = self._rerank(query, chunks[:candidate_limit], candidate_limit)
        reranked_keys = {self._chunk_key(chunk) for chunk in reranked}
        remaining = [chunk for chunk in chunks if self._chunk_key(chunk) not in reranked_keys]

        if any(chunk.rerank_score is not None for chunk in reranked):
            reranked = [self._apply_rerank_score(chunk) for chunk in reranked]
            reranked.sort(key=lambda item: item.final_score, reverse=True)

        return [*reranked, *remaining]

    def _apply_rerank_score(self, chunk: RetrievedChunk) -> RetrievedChunk:
        rerank_score = chunk.rerank_score
        if rerank_score is None:
            return chunk
        normalized_rerank = max(0.0, min(1.0, rerank_score))
        vector_score = chunk.vector_score if chunk.vector_score is not None else 0.0
        final_score = min(
            1.0,
            normalized_rerank * 0.6 + vector_score * 0.25 + chunk.keyword_score * 0.15,
        )
        return replace(chunk, score=final_score, final_score=final_score)

    def _keyword_candidates(
        self,
        keyword_terms: list[str],
        required_terms: list[str],
    ) -> list[RetrievedChunk]:
        if not keyword_terms:
            return []
        documents = self._all_documents()
        if not documents:
            return []

        doc_tokens = [self._query_terms(document.page_content) for document in documents]
        doc_count = len(doc_tokens)
        avg_len = sum(len(tokens) for tokens in doc_tokens) / max(1, doc_count)
        document_frequency: dict[str, int] = {}
        for tokens in doc_tokens:
            unique_tokens = set(tokens)
            for term in keyword_terms:
                if term in unique_tokens:
                    document_frequency[term] = document_frequency.get(term, 0) + 1

        scored: list[RetrievedChunk] = []
        for document, tokens in zip(documents, doc_tokens, strict=True):
            token_counts = {token: tokens.count(token) for token in set(tokens)}
            matched_terms = [term for term in keyword_terms if term in token_counts or term in document.page_content]
            required_matches = self._required_matches(document.page_content, required_terms)
            if not matched_terms and not required_matches:
                continue
            bm25 = 0.0
            for term in set(matched_terms + required_matches):
                frequency = token_counts.get(term, document.page_content.count(term))
                if frequency <= 0:
                    continue
                df = document_frequency.get(term, 1)
                idf = math.log(1 + (doc_count - df + 0.5) / (df + 0.5))
                denominator = frequency + 1.5 * (1 - 0.75 + 0.75 * len(tokens) / max(1.0, avg_len))
                bm25 += idf * frequency * 2.5 / denominator
            coverage = self._required_coverage(document.page_content, required_terms, document.metadata)
            keyword_score = min(1.0, (bm25 / 4.0) + coverage * 0.45)
            scored.append(
                RetrievedChunk(
                    page_content=document.page_content,
                    metadata=document.metadata,
                    score=None,
                    vector_score=None,
                    keyword_score=keyword_score,
                    matched_terms=sorted(set(matched_terms + required_matches), key=len, reverse=True),
                )
            )
        scored.sort(key=lambda item: item.keyword_score, reverse=True)
        return scored[: max(self.settings.rerank_candidate_k * 2, self.settings.retrieval_top_k)]

    def _score_hybrid_chunk(
        self,
        chunk: RetrievedChunk,
        keyword_terms: list[str],
        required_terms: list[str],
    ) -> RetrievedChunk:
        matched_terms = sorted(
            set(chunk.matched_terms or []) | {term for term in keyword_terms if term in chunk.page_content},
            key=len,
            reverse=True,
        )
        specific_matched_terms = [
            term for term in matched_terms if not self._is_low_signal_term(term)
        ]
        required_matches = self._required_matches(chunk.page_content, required_terms)
        coverage = self._required_coverage(chunk.page_content, required_terms, chunk.metadata)
        keyword_score = max(
            chunk.keyword_score,
            min(
                1.0,
                len(set(specific_matched_terms)) / max(1, len(set(keyword_terms))) + coverage * 0.45,
            ),
        )
        if matched_terms and not specific_matched_terms:
            keyword_score *= 0.35
        vector_score = chunk.vector_score if chunk.vector_score is not None else 0.0
        final_score = min(1.0, vector_score * 0.4 + keyword_score * 0.6)
        return RetrievedChunk(
            page_content=chunk.page_content,
            metadata=chunk.metadata,
            score=final_score,
            vector_score=chunk.vector_score,
            rerank_score=chunk.rerank_score,
            answerability_score=chunk.answerability_score,
            keyword_score=keyword_score,
            final_score=final_score,
            matched_terms=specific_matched_terms or matched_terms,
        )

    def _mark_selection(
        self,
        chunks: list[RetrievedChunk],
        query: str,
        required_terms: list[str],
    ) -> list[RetrievedChunk]:
        selected_count = 0
        marked: list[RetrievedChunk] = []
        final_min_score = self._final_min_score(query)
        for chunk in chunks:
            required_coverage = self._required_coverage(chunk.page_content, required_terms, chunk.metadata)
            specific_matched_terms = [
                term for term in chunk.matched_terms or [] if not self._is_low_signal_term(term)
            ]
            strong_keyword_hit = chunk.keyword_score >= 0.16 and bool(specific_matched_terms)
            strong_vector_hit = (chunk.vector_score or 0.0) >= max(0.45, self.settings.retrieval_min_score)
            strong_rerank_hit = (chunk.rerank_score or 0.0) >= final_min_score
            enough_required_terms = not required_terms or required_coverage >= 0.72
            answerable = chunk.answerability_score >= self.settings.answerability_min_score
            selectable = (
                enough_required_terms
                and answerable
                and chunk.final_score >= final_min_score
                and (strong_keyword_hit or strong_vector_hit or strong_rerank_hit)
            )
            selected = selectable and selected_count < self.settings.retrieval_top_k
            if selected:
                selected_count += 1
                if strong_rerank_hit:
                    reason = "重排判断可回答且综合得分较高"
                elif strong_keyword_hit:
                    reason = "关键问题词覆盖且片段可回答"
                else:
                    reason = "向量相似度较高且片段可回答"
            elif not enough_required_terms:
                reason = "关键问题词覆盖不足"
            elif not answerable:
                reason = "片段不能直接回答问题"
            elif not selectable:
                reason = "最终采用阈值不足"
            else:
                reason = "超过本次回答采用片段上限"
            marked.append(
                RetrievedChunk(
                    page_content=chunk.page_content,
                    metadata=chunk.metadata,
                    score=chunk.final_score,
                    vector_score=chunk.vector_score,
                    rerank_score=chunk.rerank_score,
                    answerability_score=chunk.answerability_score,
                    keyword_score=chunk.keyword_score,
                    final_score=chunk.final_score,
                    matched_terms=chunk.matched_terms,
                    selected=selected,
                    reason=reason,
                )
            )
        return marked

    def _score_answerability(
        self,
        chunk: RetrievedChunk,
        query: str,
        keyword_terms: list[str],
        required_terms: list[str],
    ) -> RetrievedChunk:
        content = chunk.page_content
        required_coverage = self._required_coverage(content, required_terms, chunk.metadata)
        specific_terms = [
            term
            for term in keyword_terms
            if not self._is_low_signal_term(term) and self._term_matches(content, term)
        ]
        specific_coverage = len(set(specific_terms)) / max(
            1,
            len({term for term in keyword_terms if not self._is_low_signal_term(term)}),
        )
        qa_bonus = 0.18 * specific_coverage if "问：" in content and "答：" in content and len(self._answer_part(content)) >= 6 else 0.0
        action_bonus = 0.16 * specific_coverage if self._question_action_is_covered(query, content) else 0.0
        rerank_bonus = min(0.25, max(0.0, chunk.rerank_score or 0.0) * 0.25)
        feature_summary_bonus = self._feature_module_summary_bonus(query, content)
        answerability_score = min(
            1.0,
            required_coverage * 0.48
            + specific_coverage * 0.22
            + qa_bonus
            + action_bonus
            + rerank_bonus
            + feature_summary_bonus * 0.4,
        )
        final_score = min(
            1.0,
            chunk.final_score * 0.72 + answerability_score * 0.28 + feature_summary_bonus,
        )
        return replace(
            chunk,
            answerability_score=round(answerability_score, 4),
            final_score=round(final_score, 4),
            score=round(final_score, 4),
        )

    def _final_min_score(self, query: str) -> float:
        compact = "".join(query.strip().split())
        cjk_count = len(re.findall(r"[\u4e00-\u9fff]", compact))
        if cjk_count and cjk_count <= 6:
            return self.settings.short_query_final_context_min_score
        return self.settings.final_context_min_score

    def _answer_part(self, content: str) -> str:
        if "答：" not in content:
            return content
        return content.split("答：", 1)[1].strip()

    def _question_action_is_covered(self, query: str, content: str) -> bool:
        compact_query = "".join(query.lower().split())
        if any(term in compact_query for term in CONFIG_QUERY_TERMS):
            return any(term in content for term in CONFIG_EVIDENCE_TERMS)
        if any(term in compact_query for term in LOCATION_QUERY_TERMS):
            return any(term in content for term in LOCATION_EVIDENCE_TERMS)
        if any(term in compact_query for term in PROCESS_QUERY_TERMS):
            return any(term in content for term in PROCESS_EVIDENCE_TERMS)
        if any(term in compact_query for term in DATA_QUERY_TERMS):
            return any(term in content for term in DATA_EVIDENCE_TERMS)
        if any(term in compact_query for term in FEATURE_MODULE_QUERY_TERMS):
            return any(term in content for term in FEATURE_MODULE_EVIDENCE_TERMS)
        return True

    def _feature_module_summary_bonus(self, query: str, content: str) -> float:
        compact_query = "".join(query.lower().split())
        if not any(term in compact_query for term in FEATURE_MODULE_QUERY_TERMS):
            return 0.0
        if "系统概述" in content and any(term in content for term in {"核心业务模块", "核心功能模块"}):
            return 0.28
        return 0.0

    def _all_documents(self) -> list[Any]:
        from langchain_core.documents import Document

        if self.using_json_store:
            return [
                Document(
                    page_content=record.get("page_content", ""),
                    metadata=dict(record.get("metadata") or {}),
                )
                for record in getattr(self.vector_store, "records", [])
            ]

        collection = getattr(self.vector_store, "_collection", None)
        if collection is None:
            return []
        try:
            payload = collection.get(include=["documents", "metadatas"])
        except Exception:
            return []
        documents = payload.get("documents") or []
        metadatas = payload.get("metadatas") or []
        return [
            Document(page_content=document or "", metadata=dict(metadata or {}))
            for document, metadata in zip(documents, metadatas, strict=False)
        ]

    def _all_metadatas(self) -> list[dict[str, Any]]:
        if self.using_json_store:
            return [
                dict(record.get("metadata") or {})
                for record in getattr(self.vector_store, "records", [])
            ]

        collection = getattr(self.vector_store, "_collection", None)
        if collection is None:
            return []
        try:
            payload = collection.get(include=["metadatas"])
        except Exception:
            return []
        return [dict(metadata or {}) for metadata in payload.get("metadatas") or []]

    def _chunk_key(self, chunk: RetrievedChunk) -> str:
        chunk_id = chunk.metadata.get("chunkId")
        if chunk_id:
            return str(chunk_id)
        source = chunk.metadata.get("sourceFile") or chunk.metadata.get("source") or ""
        return f"{source}:{hash(chunk.page_content)}"

    def _query_terms(self, text: str) -> list[str]:
        compact = "".join(text.lower().split())
        ascii_terms = re.findall(r"[a-z0-9_]+", compact)
        cjk_terms = re.findall(r"[\u4e00-\u9fff]{2,}", compact)
        known_terms = [
            "核心功能模块",
            "核心业务模块",
            "功能模块",
            "护理等级",
            "护理计划",
            "智能床位",
            "退住办理",
            "入住办理",
            "账单入账",
            "退住",
            "入住",
            "退费",
            "床位",
            "流程",
            "步骤",
            "办理",
            "配置",
            "设置",
            "入口",
            "页面",
            "菜单",
            "维护",
            "哪些",
            "数据",
            "依据",
            "知识库",
            "查看",
            "条件",
            "要求",
            "区别",
            "规则",
        ]
        bigrams = [compact[index : index + 2] for index in range(max(0, len(compact) - 1))]
        terms = [term for term in known_terms if term in compact]
        terms.extend(ascii_terms)
        terms.extend(cjk_terms)
        terms.extend(bigrams)
        return [term for term in dict.fromkeys(terms) if term]

    def _required_matches(self, content: str, required_terms: list[str]) -> list[str]:
        matches: list[str] = []
        for term in required_terms:
            if self._term_matches(content, term):
                matches.append(term)
        return matches

    def _required_coverage(self, content: str, required_terms: list[str], metadata: dict[str, Any] | None = None) -> float:
        if not required_terms:
            return 1.0
        matches = self._required_matches(content, required_terms)
        raw_coverage = len(set(matches)) / max(1, len(set(required_terms)))

        section_title = self._section_title(metadata) if metadata else None
        if not section_title:
            return raw_coverage

        if any(self._term_matches(section_title, term) for term in required_terms):
            return raw_coverage

        title_matches = [term for term in required_terms if self._term_matches(section_title, term)]
        body_only = [term for term in matches if term not in title_matches]
        weighted = len(set(title_matches)) + len(set(body_only)) * 0.5
        return weighted / max(1, len(set(required_terms)))

    @staticmethod
    def _section_title(metadata: dict[str, Any]) -> str | None:
        return metadata.get("sectionTitle") or metadata.get("question") or None

    def _term_matches(self, content: str, term: str, strict: bool = False) -> bool:
        if term in content:
            return True
        if strict:
            return False
        if term in LOCATION_QUERY_TERMS:
            return any(item in content for item in LOCATION_EVIDENCE_TERMS)
        if term in CONFIG_QUERY_TERMS:
            return any(item in content for item in CONFIG_EVIDENCE_TERMS)
        if term in PROCESS_QUERY_TERMS:
            return any(item in content for item in PROCESS_EVIDENCE_TERMS)
        if term in DATA_QUERY_TERMS:
            return any(item in content for item in DATA_EVIDENCE_TERMS)
        if term in FEATURE_MODULE_QUERY_TERMS:
            return any(item in content for item in FEATURE_MODULE_EVIDENCE_TERMS)
        return False

    def _is_low_signal_term(self, term: str) -> bool:
        if term in LOW_SIGNAL_TERMS:
            return True
        if len(term) <= 1:
            return True
        if len(term) == 2 and term in {"养老", "老人", "服务", "哪些", "什么", "怎么"}:
            return True
        return False

    def _create_vector_store(self) -> Any:
        if isinstance(self.embedding, LocalHashEmbeddings):
            return JsonVectorStore(
                self.settings.chroma_persist_dir,
                self.embedding,
            )

        from langchain_chroma import Chroma

        try:
            return Chroma(
                collection_name=self.settings.collection_name,
                embedding_function=self.embedding,
                persist_directory=str(self.settings.chroma_persist_dir),
            )
        except Exception:
            if not self.settings.auto_reset_chroma_on_error:
                raise
            self._quarantine_persist_dir()
            try:
                return Chroma(
                    collection_name=self.settings.collection_name,
                    embedding_function=self.embedding,
                    persist_directory=str(self.settings.chroma_persist_dir),
                )
            except Exception:
                if isinstance(self.embedding, LocalHashEmbeddings):
                    return JsonVectorStore(
                        self.settings.chroma_persist_dir,
                        self.embedding,
                    )
                raise

    def _quarantine_persist_dir(self) -> None:
        persist_dir = self.settings.chroma_persist_dir
        if not persist_dir.exists():
            return
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        backup_dir = Path(f"{persist_dir}.broken-{timestamp}")
        try:
            shutil.move(str(persist_dir), str(backup_dir))
        except OSError:
            return
        persist_dir.mkdir(parents=True, exist_ok=True)
