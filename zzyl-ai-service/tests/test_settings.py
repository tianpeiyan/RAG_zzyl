from __future__ import annotations

import pytest
from pathlib import Path

from config.settings import Settings, SettingsLoadError, get_settings


def test_missing_required_settings_raise_clear_error(
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    monkeypatch.chdir(tmp_path)
    for key in [
        "AI_SERVICE_KNOWLEDGE_BASE_DIR",
        "AI_SERVICE_CHROMA_PERSIST_DIR",
        "AI_SERVICE_BAILIAN_API_KEY",
        "AI_SERVICE_BAILIAN_CHAT_MODEL",
        "AI_SERVICE_BAILIAN_EMBEDDING_MODEL",
        "AI_SERVICE_INTERNAL_API_KEY",
    ]:
        monkeypatch.delenv(key, raising=False)

    with pytest.raises(SettingsLoadError) as exc_info:
        get_settings()

    message = str(exc_info.value)
    assert "AI_SERVICE_BAILIAN_API_KEY" in message
    assert "AI_SERVICE_BAILIAN_CHAT_MODEL" in message
    assert "AI_SERVICE_BAILIAN_EMBEDDING_MODEL" in message
    assert "AI_SERVICE_CHROMA_PERSIST_DIR" in message
    assert "AI_SERVICE_INTERNAL_API_KEY" in message
    assert "AI_SERVICE_KNOWLEDGE_BASE_DIR" in message


def test_production_default_embedding_provider_is_semantic(tmp_path: Path) -> None:
    settings = Settings(
        knowledge_base_dir=tmp_path / "kb",
        chroma_persist_dir=tmp_path / "chroma",
        bailian_api_key="test-key",
        bailian_chat_model="qwen-plus",
        bailian_embedding_model="text-embedding-v4",
        internal_api_key="internal-key",
    )

    assert settings.embedding_provider in {"dashscope", "dashscope-multimodal"}
    assert settings.embedding_provider != "local"
    assert settings.embedding_batch_size == 20
