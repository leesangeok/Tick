"""NoOp trace — Langfuse adapter 도입 전 (다음 PR) 까지 사용."""

import logging

from app.ports.trace_port import TraceEvent

log = logging.getLogger("tick.ai.trace")


class NoOpTraceAdapter:
    async def record(self, event: TraceEvent) -> None:
        log.info("trace event=%s meta=%s", event.name, event.metadata)
