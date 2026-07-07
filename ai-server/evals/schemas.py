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
    # 판정 안정성 메트릭 — 단일 호출 (judge_runs=1) 이면 std 는 0.0.
    judge_runs: int = 1
    groundedness_std: float = 0.0
    hallucination_count_std: float = 0.0
    retry_triggered: bool = False
    # 다중 judge — 두 Claude 모델 (Sonnet 4.6 + Opus 4.7) 로 교차검증한 경우에만 채워짐.
    # per_model: {"sonnet": JudgeVerdict-dict, "opus": ...}. 상위 필드는 median-of-medians.
    # groundedness_disagreement 는 두 모델 median 값의 차이 절댓값 (편향 지표).
    per_model: dict[str, dict[str, Any]] = field(default_factory=dict)
    groundedness_disagreement: float = 0.0
    hallucination_count_disagreement: float = 0.0


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
