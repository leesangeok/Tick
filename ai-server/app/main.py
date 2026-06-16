import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import embed, health, summary
from app.config.settings import settings
from app.deps import close_pool, open_pool

logging.basicConfig(level=settings.log_level)
log = logging.getLogger("tick.ai")


@asynccontextmanager
async def lifespan(app: FastAPI):
    await open_pool()
    log.info("ai-server startup complete")
    try:
        yield
    finally:
        await close_pool()
        log.info("ai-server shutdown complete")


app = FastAPI(title="Tick AI", version="0.2.0", lifespan=lifespan)
app.include_router(health.router)
app.include_router(summary.router)
app.include_router(embed.router)
