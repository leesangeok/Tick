from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class TraceEvent:
    name: str
    metadata: dict


class TracePort(Protocol):
    """AI 실행 추적. 다음 PR (Langfuse) 에서 실제 구현. 지금은 NoOp."""

    async def record(self, event: TraceEvent) -> None: ...
