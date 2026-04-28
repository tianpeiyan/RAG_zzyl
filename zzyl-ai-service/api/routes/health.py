from __future__ import annotations

from datetime import datetime, timezone

from fastapi import APIRouter, Request

from config.settings import Settings

router = APIRouter()


@router.get("/health")
def health(request: Request) -> dict[str, str]:
    settings: Settings = request.app.state.settings
    return {
        "status": "ok",
        "service": settings.app_name,
        "environment": settings.app_env,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
