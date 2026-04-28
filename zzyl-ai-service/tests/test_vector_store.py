from __future__ import annotations

from types import SimpleNamespace

from retrievers.langchain_vector_store import VectorStoreService


class _RecordingVectorStore:
    def __init__(self) -> None:
        self.batch_lengths: list[int] = []

    def add_documents(self, documents: list[object]) -> None:
        self.batch_lengths.append(len(documents))


def test_vector_store_batches_embedding_writes() -> None:
    service = object.__new__(VectorStoreService)
    vector_store = _RecordingVectorStore()
    service.settings = SimpleNamespace(embedding_batch_size=20)
    service.vector_store = vector_store

    service.add_documents([object() for _ in range(45)])

    assert vector_store.batch_lengths == [20, 20, 5]
