from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any

from retrievers.local_embeddings import LocalHashEmbeddings


class JsonVectorStore:
    def __init__(self, persist_dir: Path, embedding: LocalHashEmbeddings) -> None:
        self.persist_dir = persist_dir
        self.embedding = embedding
        self.persist_dir.mkdir(parents=True, exist_ok=True)
        self.path = self.persist_dir / "vectors.json"
        self.records = self._load()

    def add_documents(self, documents: list[Any]) -> None:
        texts = [document.page_content for document in documents]
        vectors = self.embedding.embed_documents(texts)
        for document, vector in zip(documents, vectors, strict=True):
            self.records.append(
                {
                    "page_content": document.page_content,
                    "metadata": dict(document.metadata),
                    "vector": vector,
                }
            )
        self._save()

    def similarity_search(self, query: str, k: int, min_score: float) -> list[tuple[Any, float]]:
        from langchain_core.documents import Document

        query_vector = self.embedding.embed_query(query)
        scored: list[tuple[float, dict[str, Any]]] = []
        for record in self.records:
            score = _cosine_similarity(query_vector, record["vector"])
            if score >= min_score:
                scored.append((score, record))
        scored.sort(key=lambda item: item[0], reverse=True)
        return [
            (
                Document(
                    page_content=record["page_content"],
                    metadata=dict(record["metadata"]),
                ),
                score,
            )
            for score, record in scored[:k]
        ]

    def count(self) -> int:
        return len(self.records)

    def reset(self) -> None:
        self.records = []
        self._save()

    def _load(self) -> list[dict[str, Any]]:
        if not self.path.exists():
            return []
        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return []
        if not isinstance(data, list):
            return []
        return data

    def _save(self) -> None:
        self.path.write_text(
            json.dumps(self.records, ensure_ascii=False),
            encoding="utf-8",
        )


def _cosine_similarity(left: list[float], right: list[float]) -> float:
    numerator = sum(a * b for a, b in zip(left, right, strict=True))
    left_norm = math.sqrt(sum(value * value for value in left))
    right_norm = math.sqrt(sum(value * value for value in right))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return numerator / (left_norm * right_norm)
