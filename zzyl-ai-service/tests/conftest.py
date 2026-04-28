from __future__ import annotations

from pathlib import Path

import pytest

from config.settings import clear_settings_cache
from rag.service import clear_rag_service_cache


@pytest.fixture(autouse=True)
def reset_settings_cache() -> None:
    clear_settings_cache()
    clear_rag_service_cache()
    yield
    clear_settings_cache()
    clear_rag_service_cache()


@pytest.fixture
def configured_env(monkeypatch: pytest.MonkeyPatch, tmp_path: Path) -> dict[str, Path]:
    knowledge_base_dir = tmp_path / "knowledge-base"
    chroma_persist_dir = tmp_path / "data" / "chroma"

    monkeypatch.setenv("AI_SERVICE_APP_NAME", "zzyl-ai-service-test")
    monkeypatch.setenv("AI_SERVICE_APP_ENV", "test")
    monkeypatch.setenv("AI_SERVICE_LOG_LEVEL", "INFO")
    monkeypatch.setenv("AI_SERVICE_HOST", "127.0.0.1")
    monkeypatch.setenv("AI_SERVICE_PORT", "8000")
    monkeypatch.setenv("AI_SERVICE_KNOWLEDGE_BASE_DIR", str(knowledge_base_dir))
    monkeypatch.setenv("AI_SERVICE_CHROMA_PERSIST_DIR", str(chroma_persist_dir))
    monkeypatch.setenv("AI_SERVICE_BAILIAN_API_KEY", "dummy-key")
    monkeypatch.setenv(
        "AI_SERVICE_BAILIAN_BASE_URL",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
    )
    monkeypatch.setenv("AI_SERVICE_BAILIAN_CHAT_MODEL", "qwen-plus")
    monkeypatch.setenv("AI_SERVICE_BAILIAN_EMBEDDING_MODEL", "text-embedding-v4")
    monkeypatch.setenv("AI_SERVICE_REQUEST_TIMEOUT_SECONDS", "30")
    monkeypatch.setenv("AI_SERVICE_REQUEST_MAX_RETRIES", "2")
    monkeypatch.setenv("AI_SERVICE_REQUEST_RETRY_BACKOFF_SECONDS", "0")
    monkeypatch.setenv("AI_SERVICE_CHUNK_SIZE", "120")
    monkeypatch.setenv("AI_SERVICE_CHUNK_OVERLAP", "20")
    monkeypatch.setenv("AI_SERVICE_EMBEDDING_PROVIDER", "local")
    monkeypatch.setenv("AI_SERVICE_LOCAL_EMBEDDING_DIMENSIONS", "384")
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_TOP_K", "5")
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0.2")
    monkeypatch.setenv("AI_SERVICE_INTERNAL_API_KEY", "test-internal-key")

    return {
        "knowledge_base_dir": knowledge_base_dir,
        "chroma_persist_dir": chroma_persist_dir,
    }
