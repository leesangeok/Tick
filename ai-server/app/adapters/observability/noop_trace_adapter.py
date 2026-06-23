"""NoOp trace — Langfuse 키가 없는 환경에서 사용."""

import logging
from contextlib import asynccontextmanager

from app.ports.trace_port import TraceEvent

log = logging.getLogger("tick.ai.trace")


class NoOpTraceAdapter:
    @asynccontextmanager
    async def span(self, name: str, **metadata):
        log.info("trace span start name=%s meta=%s", name, metadata)
        try:
            yield
        finally:
            log.info("trace span end name=%s", name)

    async def record(self, event: TraceEvent) -> None:
        log.info("trace event=%s meta=%s", event.name, event.metadata)
