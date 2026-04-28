from __future__ import annotations

from dataclasses import replace
from typing import TYPE_CHECKING, Any

import httpx

from config.settings import Settings

if TYPE_CHECKING:
    from retrievers.langchain_vector_store import RetrievedChunk


class DashScopeReranker:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def rerank(
        self,
        query: str,
        chunks: list[RetrievedChunk],
        top_n: int | None = None,
    ) -> list[RetrievedChunk]:
        limit = top_n or self.settings.retrieval_top_k
        if not self.settings.rerank_enabled or len(chunks) <= 1:
            return chunks[:limit]
        if self.settings.bailian_api_key in {"dummy-key", "replace-with-api-key"}:
            return chunks[:limit]

        documents = [{"text": chunk.page_content} for chunk in chunks[:100]]
        limit = min(limit, len(documents))
        payload = {
            "model": self.settings.bailian_rerank_model,
            "input": {
                "query": {"text": query},
                "documents": documents,
            },
            "parameters": {
                "return_documents": False,
                "top_n": limit,
            },
        }
        headers = {
            "Authorization": f"Bearer {self.settings.bailian_api_key}",
            "Content-Type": "application/json",
        }

        try:
            response = httpx.post(
                self.settings.bailian_rerank_url,
                json=payload,
                headers=headers,
                timeout=self.settings.request_timeout_seconds,
            )
            response.raise_for_status()
            results = self._extract_results(response.json())
        except Exception:
            return chunks[:limit]

        reranked: list[RetrievedChunk] = []
        for result in results:
            index = result.get("index")
            if not isinstance(index, int) or index < 0 or index >= len(chunks):
                continue
            score = result.get("relevance_score", result.get("score"))
            try:
                numeric_score = float(score) if score is not None else chunks[index].score
            except (TypeError, ValueError):
                numeric_score = chunks[index].score
            reranked.append(
                replace(
                    chunks[index],
                    score=numeric_score,
                    rerank_score=numeric_score,
                )
            )

        if not reranked:
            return chunks[:limit]
        return reranked[:limit]

    def _extract_results(self, payload: dict[str, Any]) -> list[dict[str, Any]]:
        output = payload.get("output")
        if not isinstance(output, dict):
            return []
        results = output.get("results") or output.get("documents")
        if not isinstance(results, list):
            return []
        return [item for item in results if isinstance(item, dict)]
