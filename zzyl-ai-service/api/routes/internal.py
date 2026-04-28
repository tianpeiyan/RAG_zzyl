from __future__ import annotations

import uuid
import json
import time
from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from config.settings import Settings
from rag import RagServiceError, get_rag_service
from rag.knowledge_base import list_knowledge_base_documents

router = APIRouter()


class AssistantMessage(BaseModel):
    role: str = Field(..., description="消息角色: user 或 assistant")
    content: str = Field(..., description="消息内容")


class AssistantChatParams(BaseModel):
    sessionId: str | None = Field(None, description="会话ID")
    question: str = Field(..., description="用户问题")
    history: list[AssistantMessage] | None = Field(None, description="历史消息")


class AssistantSource(BaseModel):
    documentTitle: str | None = None
    category: str | None = None
    sourceFile: str | None = None
    chunkId: str | None = None
    score: float | None = None
    selected: bool = False
    reason: str | None = None
    matchedTerms: list[str] | None = None
    scoreBreakdown: dict[str, float | None] | None = None
    snippet: str | None = None
    content: str | None = None


class AssistantRetrievalTrace(BaseModel):
    totalCandidates: int = 0
    selectedCount: int = 0
    discardedCount: int = 0
    rewrittenQuery: str | None = None
    expandedQueries: list[str] | None = None
    requiredTerms: list[str] | None = None


class AssistantChatResult(BaseModel):
    sessionId: str | None = None
    answer: str = ""
    status: str = "success"
    refused: bool = False
    hitCount: int = 0
    traceId: str | None = None
    sources: list[AssistantSource] | None = None
    questionType: str | None = None
    confidence: float = 0.0
    retrievalTrace: AssistantRetrievalTrace | None = None


class KbDocument(BaseModel):
    title: str = ""
    category: str = ""
    sourceFile: str = ""
    updatedAt: str = ""
    status: str = "indexed"
    chunkCount: int = 0
    errorMessage: str | None = None


class KbFailedTask(BaseModel):
    taskId: str = ""
    sourceFile: str = ""
    reason: str = ""
    occurredAt: str = ""


class KbIndexStatus(BaseModel):
    status: str = "ready"
    rebuilding: bool = False
    documentCount: int = 0
    chunkCount: int = 0
    lastIndexedAt: str = ""
    message: str | None = None
    failedTasks: list[KbFailedTask] | None = None


class KbRebuildResult(BaseModel):
    taskId: str | None = None
    status: str = "started"
    message: str | None = None


@router.post("/internal/chat", response_model=AssistantChatResult)
@router.post("/assistant/chat", response_model=AssistantChatResult)
def chat_with_assistant(params: AssistantChatParams, request: Request) -> dict[str, Any]:
    return _chat_payload(params, request)


@router.post("/internal/chat/stream")
@router.post("/assistant/chat/stream")
def stream_chat_with_assistant(params: AssistantChatParams, request: Request) -> StreamingResponse:
    def event_stream():
        try:
            payload = _chat_payload(params, request)
            answer = payload.get("answer") or "知识库暂无依据"
            for index in range(0, len(answer), 6):
                chunk = answer[index : index + 6]
                yield _sse("delta", {"type": "delta", "delta": chunk})
                time.sleep(0.025)
            yield _sse("done", {"type": "done", "data": payload})
        except HTTPException as exc:
            yield _sse("error", {"type": "error", "message": str(exc.detail)})
        except Exception as exc:
            yield _sse("error", {"type": "error", "message": f"RAG回答生成失败: {exc}"})

    return StreamingResponse(event_stream(), media_type="text/event-stream")


def _chat_payload(params: AssistantChatParams, request: Request) -> dict[str, Any]:
    settings: Settings = request.app.state.settings
    session_id = params.sessionId or str(uuid.uuid4())
    trace_id = str(uuid.uuid4())
    history = [message.model_dump() for message in params.history or []]

    try:
        rag_result = get_rag_service(settings).chat(params.question, session_id, history)
    except RagServiceError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"RAG回答生成失败: {exc}") from exc

    sources = [
        AssistantSource(
            documentTitle=source.document_title,
            category=source.category,
            sourceFile=source.source_file,
            chunkId=source.chunk_id,
            score=source.score,
            selected=source.selected,
            reason=source.reason,
            matchedTerms=source.matched_terms,
            scoreBreakdown=source.score_breakdown,
            snippet=source.snippet,
            content=source.content,
        )
        for source in rag_result.sources
    ]
    trace = rag_result.retrieval_trace

    return AssistantChatResult(
        sessionId=session_id,
        answer=rag_result.answer,
        status="success",
        refused=rag_result.refused,
        hitCount=len([source for source in sources if source.selected]),
        traceId=trace_id,
        sources=sources,
        questionType=rag_result.question_type,
        confidence=rag_result.confidence,
        retrievalTrace=AssistantRetrievalTrace(
            totalCandidates=trace.total_candidates if trace else len(sources),
            selectedCount=trace.selected_count if trace else len([source for source in sources if source.selected]),
            discardedCount=trace.discarded_count if trace else 0,
            rewrittenQuery=trace.rewritten_query if trace else None,
            expandedQueries=trace.expanded_queries if trace else None,
            requiredTerms=trace.required_terms if trace else None,
        ),
    ).model_dump()


def _sse(event: str, payload: dict[str, Any]) -> str:
    data = json.dumps(payload, ensure_ascii=False)
    return f"event: {event}\ndata: {data}\n\n"


@router.get("/internal/kb/documents", response_model=list[KbDocument])
@router.get("/assistant/kb/documents", response_model=list[KbDocument])
def get_kb_documents(request: Request) -> list[dict[str, Any]]:
    settings: Settings = request.app.state.settings
    documents = list_knowledge_base_documents(settings)

    return [
        KbDocument(
            title=document.title,
            category=document.category,
            sourceFile=document.source_file,
            updatedAt=document.updated_at,
            status=document.status,
            chunkCount=document.chunk_count,
            errorMessage=document.error_message,
        ).model_dump()
        for document in documents
    ]


@router.get("/internal/kb/status", response_model=KbIndexStatus)
@router.get("/assistant/kb/index/status", response_model=KbIndexStatus)
def get_kb_index_status(request: Request) -> dict[str, Any]:
    settings: Settings = request.app.state.settings
    documents = list_knowledge_base_documents(settings)
    failed_tasks = _build_failed_tasks(documents)
    document_chunk_count = sum(document.chunk_count for document in documents)

    try:
        rag_service = get_rag_service(settings)
        chunk_count = rag_service.chunk_count()
        expected_index = rag_service.knowledge_base.expected_index_state()
        index_current = (
            not expected_index.failed
            and chunk_count == expected_index.chunk_count
            and rag_service.vector_service.indexed_document_hashes() == expected_index.document_hashes
        )
    except RagServiceError as exc:
        return KbIndexStatus(
            status="failed",
            rebuilding=False,
            documentCount=len([document for document in documents if document.status == "indexed"]),
            chunkCount=document_chunk_count,
            lastIndexedAt=datetime.now(timezone.utc).isoformat(),
            message=f"向量库暂不可用: {exc}",
            failedTasks=failed_tasks,
        ).model_dump()

    indexed_count = len([document for document in documents if document.status == "indexed"])
    if not index_current:
        return KbIndexStatus(
            status="degraded",
            rebuilding=False,
            documentCount=indexed_count,
            chunkCount=chunk_count,
            lastIndexedAt=datetime.now(timezone.utc).isoformat(),
            message=f"知识库索引已过期，当前向量分片 {chunk_count} 个，知识目录应有 {expected_index.chunk_count} 个，请重建索引",
            failedTasks=failed_tasks,
        ).model_dump()

    return KbIndexStatus(
        status="degraded" if failed_tasks else "ready",
        rebuilding=False,
        documentCount=indexed_count,
        chunkCount=chunk_count or document_chunk_count,
        lastIndexedAt=datetime.now(timezone.utc).isoformat(),
        message="知识库索引正常运行" if not failed_tasks else "部分文档读取失败",
        failedTasks=failed_tasks,
    ).model_dump()


@router.post("/internal/kb/reindex", response_model=KbRebuildResult)
@router.post("/assistant/kb/index/rebuild", response_model=KbRebuildResult)
def rebuild_kb_index(request: Request) -> dict[str, Any]:
    settings: Settings = request.app.state.settings
    task_id = str(uuid.uuid4())

    try:
        summary = get_rag_service(settings).rebuild_index()
    except RagServiceError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"知识库索引重建失败: {exc}") from exc

    return KbRebuildResult(
        taskId=task_id,
        status="completed" if not summary.failed else "completed_with_errors",
        message=f"知识库索引重建完成，文档 {len(summary.documents)} 个，分片 {summary.chunk_count} 个",
    ).model_dump()


def _build_failed_tasks(documents: list[Any]) -> list[KbFailedTask]:
    return [
        KbFailedTask(
            taskId=f"document-{index + 1}",
            sourceFile=document.source_file,
            reason=document.error_message or "文档读取失败",
            occurredAt=datetime.now(timezone.utc).isoformat(),
        )
        for index, document in enumerate(documents)
        if document.status == "failed"
    ]
