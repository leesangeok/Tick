from contextlib import AbstractAsyncContextManager
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class TraceEvent:
    name: str
    metadata: dict


class TracePort(Protocol):
    """AI 실행 추적."""

    def span(self, name: str, **metadata) -> AbstractAsyncContextManager[None]:
        """use case 전체를 감싸는 root span.

        LLM / embedding 호출의 LangChain callback span 들이 이 안에서 child 로 들어간다.
        `record()` 는 active span 이 있어야 동작하므로 use case 시작 시 이 span 을
        열어두는 게 안전하다.
        """
        ...

    async def record(self, event: TraceEvent) -> None: ...
