from __future__ import annotations

import hashlib
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from config.settings import Settings
from loaders import iter_knowledge_files, load_document_text
from rag.chunker import split_knowledge_text
from retrievers import VectorStoreService


@dataclass(frozen=True)
class IndexedDocument:
    title: str
    category: str
    source_file: str
    updated_at: str
    status: str
    chunk_count: int
    error_message: str | None = None


@dataclass(frozen=True)
class RebuildSummary:
    documents: list[IndexedDocument]
    chunk_count: int
    failed: list[IndexedDocument]


@dataclass(frozen=True)
class ExpectedIndexState:
    documents: list[IndexedDocument]
    chunk_count: int
    document_hashes: dict[str, str]
    failed: list[IndexedDocument]


class KnowledgeBaseService:
    def __init__(self, settings: Settings, vector_service: VectorStoreService) -> None:
        self.settings = settings
        self.vector_service = vector_service

    def rebuild(self) -> RebuildSummary:
        self.vector_service.reset()
        documents: list[IndexedDocument] = []
        failed: list[IndexedDocument] = []
        chunk_total = 0

        for path in iter_knowledge_files(self.settings.knowledge_base_dir):
            try:
                indexed_document, langchain_documents = self._build_documents(path)
                self.vector_service.add_documents(langchain_documents)
                documents.append(indexed_document)
                chunk_total += indexed_document.chunk_count
            except Exception as exc:
                failed_document = self._failed_document(path, str(exc))
                documents.append(failed_document)
                failed.append(failed_document)

        return RebuildSummary(
            documents=documents,
            chunk_count=chunk_total,
            failed=failed,
        )

    def documents(self) -> list[IndexedDocument]:
        return list_knowledge_base_documents(self.settings)

    def expected_index_state(self) -> ExpectedIndexState:
        documents: list[IndexedDocument] = []
        failed: list[IndexedDocument] = []
        document_hashes: dict[str, str] = {}
        chunk_total = 0

        for path in iter_knowledge_files(self.settings.knowledge_base_dir):
            try:
                indexed_document, langchain_documents = self._build_documents(path)
                documents.append(indexed_document)
                chunk_total += indexed_document.chunk_count
                if langchain_documents:
                    content_hash = langchain_documents[0].metadata.get("contentHash")
                    if content_hash:
                        document_hashes[indexed_document.source_file] = str(content_hash)
            except Exception as exc:
                failed_document = self._failed_document(path, str(exc))
                documents.append(failed_document)
                failed.append(failed_document)

        return ExpectedIndexState(
            documents=documents,
            chunk_count=chunk_total,
            document_hashes=document_hashes,
            failed=failed,
        )

    def index_is_current(self) -> bool:
        expected = self.expected_index_state()
        if expected.failed:
            return False
        if self.vector_service.count() != expected.chunk_count:
            return False
        return self.vector_service.indexed_document_hashes() == expected.document_hashes

    def _build_documents(self, path: Path) -> tuple[IndexedDocument, list[Any]]:
        from langchain_core.documents import Document
        from langchain_text_splitters import RecursiveCharacterTextSplitter

        text = load_document_text(path).strip()
        if not text:
            raise ValueError("文档内容为空")

        relative_path = path.relative_to(self.settings.knowledge_base_dir)
        category = relative_path.parts[0] if len(relative_path.parts) > 1 else ""
        chunks = self._split_text(
            text,
            category=category,
            source_file=relative_path.as_posix(),
            splitter_cls=RecursiveCharacterTextSplitter,
        )
        updated_at = datetime.fromtimestamp(
            path.stat().st_mtime,
            tz=timezone.utc,
        ).isoformat()
        content_hash = hashlib.md5(text.encode("utf-8")).hexdigest()
        metadata = {
            "documentTitle": path.stem,
            "category": category,
            "sourceFile": relative_path.as_posix(),
            "contentHash": content_hash,
            "updatedAt": updated_at,
        }
        langchain_documents = [
            Document(
                page_content=chunk,
                metadata={
                    **metadata,
                    **chunk_metadata,
                    "chunkId": f"{content_hash}-{index + 1}",
                    "chunkIndex": index,
                },
            )
            for index, (chunk, chunk_metadata) in enumerate(chunks)
        ]
        return (
            IndexedDocument(
                title=path.stem,
                category=category,
                source_file=relative_path.as_posix(),
                updated_at=updated_at,
                status="indexed",
                chunk_count=len(langchain_documents),
            ),
            langchain_documents,
        )

    def _split_text(
        self,
        text: str,
        *,
        category: str,
        source_file: str,
        splitter_cls: type,
    ) -> list[tuple[str, dict[str, Any]]]:
        chunks = split_knowledge_text(
            text,
            category=category,
            source_file=source_file,
            chunk_size=self.settings.chunk_size,
            chunk_overlap=self.settings.chunk_overlap,
            splitter_cls=splitter_cls,
        )
        return [(chunk.content, chunk.metadata) for chunk in chunks]

    def _failed_document(self, path: Path, message: str) -> IndexedDocument:
        try:
            relative_path = path.relative_to(self.settings.knowledge_base_dir)
            source_file = relative_path.as_posix()
            category = relative_path.parts[0] if len(relative_path.parts) > 1 else ""
        except ValueError:
            source_file = path.name
            category = ""
        return IndexedDocument(
            title=path.stem,
            category=category,
            source_file=source_file,
            updated_at=datetime.now(timezone.utc).isoformat(),
            status="failed",
            chunk_count=0,
            error_message=message,
        )


def list_knowledge_base_documents(settings: Settings) -> list[IndexedDocument]:
    service = _DocumentMetadataService(settings)
    return service.documents()


class _DocumentMetadataService(KnowledgeBaseService):
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.vector_service = None

    def documents(self) -> list[IndexedDocument]:
        result: list[IndexedDocument] = []
        for path in iter_knowledge_files(self.settings.knowledge_base_dir):
            try:
                indexed_document, _ = self._build_documents(path)
                result.append(indexed_document)
            except Exception as exc:
                result.append(self._failed_document(path, str(exc)))
        return result
