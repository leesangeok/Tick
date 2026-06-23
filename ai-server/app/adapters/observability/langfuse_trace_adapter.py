"""TracePort 의 Langfuse 구현.

`span()` 으로 use case 전체를 감싸는 root span 을 연다.
그 안에서 LangChain `CallbackHandler` 가 만든 LLM/embedding span 들이 child 로 들어간다.
`record()` 는 그 root span 에 application-level metadata 를 머지한다.
"""

import logging
from contextlib import asynccontextmanager

from langfuse import get_client

from app.ports.trace_port import TraceEvent

log = logging.getLogger("tick.ai.trace")


class LangfuseTraceAdapter:
    def __init__(self) -> None:
        # env (LANGFUSE_*) 자동 사용.
        self._client = get_client()

    @asynccontextmanager
    async def span(self, name: str, **metadata):
        # langfuse SDK 는 OpenTelemetry contextvar 기반이라 async 안에서도
        # 같은 task 흐름이면 context 가 살아있다.
        with self._client.start_as_current_observation(
            name=name, as_type="span", metadata=metadata or None
        ):
            yield

    async def record(self, event: TraceEvent) -> None:
        try:
            self._client.update_current_span(
                name=event.name,
                metadata=event.metadata,
            )
        except Exception as e:
            # observability 가 도메인 흐름을 깨면 안 됨.
            log.warning("langfuse trace failed name=%s err=%s", event.name, e)
