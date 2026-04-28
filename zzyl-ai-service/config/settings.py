from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from pydantic import Field, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict


class SettingsLoadError(RuntimeError):
    """Raised when required runtime configuration is missing or invalid."""


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        env_prefix="AI_SERVICE_",
        extra="ignore",
        case_sensitive=False,
    )

    app_name: str = "zzyl-ai-service"
    app_env: str = "dev"
    log_level: str = "INFO"
    host: str = "127.0.0.1"
    port: int = 8000
    knowledge_base_dir: Path = Field(...)
    chroma_persist_dir: Path = Field(...)
    bailian_api_key: str = Field(..., min_length=1)
    bailian_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    bailian_chat_model: str = Field(..., min_length=1)
    bailian_embedding_model: str = Field(..., min_length=1)
    bailian_embedding_dimension: int | None = 1152
    bailian_rerank_model: str = "qwen3-vl-rerank"
    bailian_rerank_url: str = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank"
    request_timeout_seconds: float = 30.0
    request_max_retries: int = 2
    request_retry_backoff_seconds: float = 0.5
    chunk_size: int = 800
    chunk_overlap: int = 120
    collection_name: str = "zzyl_knowledge_base"
    chat_history_dir: Path = Path("./data/chat_history")
    embedding_provider: str = "dashscope"
    embedding_batch_size: int = 20
    local_embedding_dimensions: int = 384
    auto_reset_chroma_on_error: bool = True
    llm_fallback_to_context: bool = True
    retrieval_top_k: int = 5
    rerank_enabled: bool = True
    rerank_candidate_k: int = 12
    retrieval_min_score: float = 0.2
    final_context_min_score: float = 0.34
    short_query_final_context_min_score: float = 0.46
    answerability_min_score: float = 0.55
    query_rewrite_enabled: bool = True
    query_rewrite_llm_enabled: bool = True
    internal_api_key: str = Field(..., min_length=1)


def _format_validation_error(exc: ValidationError) -> str:
    env_names: list[str] = []
    for error in exc.errors():
        location = error.get("loc", ())
        if not location:
            continue
        env_key = f"AI_SERVICE_{str(location[0]).upper()}"
        env_names.append(env_key)

    unique_env_names = ", ".join(sorted(set(env_names)))
    return (
        "Invalid AI service configuration. Missing or invalid required settings: "
        f"{unique_env_names}. Please fill them in via environment variables or .env."
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    try:
        return Settings()
    except ValidationError as exc:
        raise SettingsLoadError(_format_validation_error(exc)) from exc


def clear_settings_cache() -> None:
    get_settings.cache_clear()


def prepare_runtime_directories(settings: Settings) -> None:
    settings.knowledge_base_dir.mkdir(parents=True, exist_ok=True)
    settings.chroma_persist_dir.mkdir(parents=True, exist_ok=True)
    settings.chat_history_dir.mkdir(parents=True, exist_ok=True)
