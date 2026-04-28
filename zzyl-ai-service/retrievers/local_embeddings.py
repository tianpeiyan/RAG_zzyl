from __future__ import annotations

import hashlib
import math
import re

from langchain_core.embeddings import Embeddings


class LocalHashEmbeddings(Embeddings):
    """Deterministic local embeddings for small internal knowledge bases."""

    def __init__(self, dimensions: int = 384) -> None:
        if dimensions <= 0:
            raise ValueError("dimensions must be positive")
        self.dimensions = dimensions

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)

    def _embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "big") % self.dimensions
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            vector[index] += sign

        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector
        return [value / norm for value in vector]

    def _tokens(self, text: str) -> list[str]:
        words = re.findall(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]", text.lower())
        cjk_terms = re.findall(r"[\u4e00-\u9fff]{2,}", text)
        bigrams = [
            text[index : index + 2]
            for index in range(max(0, len(text) - 1))
            if text[index : index + 2].strip()
        ]
        trigrams = [
            text[index : index + 3]
            for index in range(max(0, len(text) - 2))
            if text[index : index + 3].strip()
        ]
        return words + cjk_terms + bigrams + trigrams
