from __future__ import annotations

from http import HTTPStatus
from typing import Any

from langchain_core.embeddings import Embeddings


class DashScopeMultiModalEmbeddings(Embeddings):
    """DashScope multimodal embedding wrapper for text-only RAG chunks."""

    def __init__(
        self,
        model: str,
        api_key: str,
        dimension: int | None = None,
    ) -> None:
        if not model:
            raise ValueError("model must not be empty")
        if not api_key:
            raise ValueError("api_key must not be empty")
        self.model = model
        self.api_key = api_key
        self.dimension = dimension

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return self._embed([{"text": text} for text in texts])

    def embed_query(self, text: str) -> list[float]:
        vectors = self._embed([{"text": text}])
        return vectors[0] if vectors else []

    def _embed(self, inputs: list[dict[str, str]]) -> list[list[float]]:
        if not inputs:
            return []

        import dashscope

        kwargs: dict[str, Any] = {
            "api_key": self.api_key,
            "model": self.model,
            "input": inputs,
        }
        if self.dimension is not None:
            kwargs["dimension"] = self.dimension

        response = dashscope.MultiModalEmbedding.call(**kwargs)
        status_code = getattr(response, "status_code", None)
        if status_code != HTTPStatus.OK:
            message = getattr(response, "message", None) or getattr(response, "code", None)
            raise RuntimeError(f"DashScope embedding request failed: {message}")

        return self._extract_vectors(response)

    def _extract_vectors(self, response: Any) -> list[list[float]]:
        output = getattr(response, "output", None)
        if output is None and isinstance(response, dict):
            output = response.get("output")
        if not isinstance(output, dict):
            raise RuntimeError("DashScope embedding response missing output")

        embeddings = output.get("embeddings") or output.get("embedding")
        if embeddings is None:
            raise RuntimeError("DashScope embedding response missing embeddings")

        if isinstance(embeddings, dict):
            embeddings = [embeddings]

        vectors: list[list[float]] = []
        for item in embeddings:
            if isinstance(item, dict):
                vector = item.get("embedding") or item.get("vector")
            else:
                vector = item
            if not isinstance(vector, list):
                raise RuntimeError("DashScope embedding response contains invalid vector")
            vectors.append([float(value) for value in vector])
        return vectors
