from __future__ import annotations

from contextlib import asynccontextmanager
from fastapi import FastAPI

from api.routes.health import router as health_router
from api.routes.internal import router as internal_router
from config.logging import configure_logging, get_logger
from config.settings import Settings, get_settings, prepare_runtime_directories


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    configure_logging(settings.log_level)
    prepare_runtime_directories(settings)

    app.state.settings = settings
    logger = get_logger("zzyl_ai_service.startup")
    logger.info(
        "service_starting",
        extra={
            "app_name": settings.app_name,
            "app_env": settings.app_env,
            "host": settings.host,
            "port": settings.port,
        },
    )
    try:
        yield
    finally:
        logger.info("service_stopped", extra={"app_name": settings.app_name})


def create_app() -> FastAPI:
    app = FastAPI(
        title="ZZYL AI Service",
        version="0.1.0",
        lifespan=lifespan,
    )
    app.include_router(health_router)
    app.include_router(internal_router)
    return app


def get_app_settings(app: FastAPI) -> Settings:
    return app.state.settings


app = create_app()
