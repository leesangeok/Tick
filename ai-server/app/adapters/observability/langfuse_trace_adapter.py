"""TracePort 의 Langfuse 구현.

LangChain `CallbackHandler` 가 LLM/embedding 자동 trace 를 만든다.
이 adapter 는 use case 레벨의 application event 를 추가 기록한다 (span 안의 event).
"""

import logging

from langfuse import get_client

from app.ports.trace_port import TraceEvent

log = logging.getLogger("tick.ai.trace")


class LangfuseTraceAdapter:
    def __init__(self) -> None:
        # env (LANGFUSE_*) 자동 사용. 키 없으면 SDK 는 noop.
        self._client = get_client()

    async def record(self, event: TraceEvent) -> None:
        try:
            # 현재 active span 에 metadata 머지.
            # CallbackHandler 가 만든 root span 안의 application-level marker 역할.
            self._client.update_current_span(
                name=event.name,
                metadata=event.metadata,
            )
        except Exception as e:
            # observability 가 도메인 흐름을 깨면 안 됨.
            log.warning("langfuse trace failed name=%s err=%s", event.name, e)
