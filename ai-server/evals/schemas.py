from dataclasses import dataclass, field
from typing import Any


@dataclass
class GoldenItem:
    symbol: str
    stock_name: str
    tier: str
    expected_topics: list[str] = field(default_factory=list)
    known_hallucination_traps: list[str] = field(default_factory=list)
    notes: str = ""


@dataclass
class JudgeVerdict:
    groundedness: float
    citation_accuracy: float
    hallucination_count: int
    hallucination_examples: list[str]
    coverage: float
    notes: str


@dataclass
class EvalRecord:
    symbol: str
    stock_name: str
    tier: str
    ok: bool
    error: str | None
    summary: str | None
    key_reasons: list[str]
    risk_notes: list[str]
    source_count: int
    source_breakdown: dict[str, int]
    retrieval_top_k: int
    elapsed_ms: int
    judge: JudgeVerdict | None


@dataclass
class EvalRun:
    label: str
    started_at: str
    finished_at: str
    top_k: int
    days_window: int
    llm_model: str
    judge_model: str
    aggregate: dict[str, Any]
    records: list[EvalRecord]
