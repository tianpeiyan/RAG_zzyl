from __future__ import annotations

import uvicorn

from api.app import app
from config.settings import get_settings


def run() -> None:
    settings = get_settings()
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        reload=False,
        factory=False,
    )


if __name__ == "__main__":
    run()
