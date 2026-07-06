# evals — RAG 요약 품질 회귀 방지

이 프로젝트에서 LLM 요약이 뉴스 근거를 벗어나 hallucinate 하는지, 새로 소스를 추가/프롬프트를 바꿨을 때
품질이 실제로 개선되는지를 **숫자로** 잡는다.

## 구조

```
evals/
├── golden_set.jsonl        # 종목 15개 (대형/중형/KOSDAQ 혼합) + 기대 토픽 + hallucination 함정 힌트
├── judge.py                # Claude Sonnet 4.6 로 채점 (groundedness / citation / hallucination / coverage)
├── run_eval.py             # 순회 러너. summarize_stock 로직을 직접 조합해 실행 (cache 우회)
├── schemas.py              # dataclass
└── results/                # YYYY-MM-DD_<label>.json — 실행 결과 (git 트래킹, baseline 비교용)
```

## Judge 채점 지표

| 지표 | 범위 | 의미 |
|---|---|---|
| `groundedness` | 0.0~1.0 | 요약 문장의 각 주장이 검색 뉴스 안에 있는가 |
| `citation_accuracy` | 0.0~1.0 | `key_reasons` 각 항목이 뉴스로 직접 뒷받침되는가 |
| `hallucination_count` | int | 뉴스에 없는 사실 주장 개수 (숫자, M&A, 실적, 일정 등) |
| `coverage` | 0.0~1.0 | 뉴스의 핵심 내용을 요약이 반영한 정도 |

Judge 는 요약 생성 모델(Haiku)보다 큰 Sonnet 을 쓴다. 같은 모델로 자기 채점하면 신뢰 못 함.

## 실행

전제:
- 백엔드가 뉴스를 이미 수집·임베딩 (`POST /api/v1/news/{symbol}/collect` + `POST /api/v1/ai/stocks/{symbol}/embed`) 해둔 상태여야 한다.
- 골든셋 15개 종목 전부에 대해 수집이 완료돼 있어야 baseline 이 의미 있다.

```bash
cd ai-server
export OPENAI_API_KEY=sk-proj-...
export ANTHROPIC_API_KEY=sk-ant-...
export POSTGRES_DSN=postgresql://tick:tick@localhost:5432/tick

# baseline (네이버만)
uv run python -m evals.run_eval --label baseline-naver-only

# DART 통합 후
uv run python -m evals.run_eval --label after-dart-added

# 결과: evals/results/2026-07-02_baseline-naver-only.json
#      evals/results/2026-07-02_after-dart-added.json
```

## 비용

- 종목 15개 × (Haiku 요약 + Sonnet 채점) ≈ 회당 $0.3~0.5 (매우 rough)
- 매 PR 마다 자동 실행 X. 수동/label 트리거 (CI workflow 참고).

## 결과 파일 형태 (요약)

```json
{
  "label": "baseline-naver-only",
  "llm_model": "claude-haiku-4-5",
  "judge_model": "claude-sonnet-4-6",
  "aggregate": {
    "count_total": 15,
    "count_judged": 13,
    "count_no_news": 2,
    "groundedness_mean": 0.72,
    "citation_accuracy_mean": 0.65,
    "hallucination_count_sum": 8,
    "coverage_mean": 0.58,
    "by_tier": { "large": {...}, "mid": {...}, "kosdaq": {...} }
  },
  "records": [ { "symbol": "005930", "judge": {...}, ... }, ... ]
}
```

## 후속

- Langfuse Datasets 로 sync (실행별 run_id 를 Langfuse dataset run 에 매핑)
- CI 회귀 게이트: `groundedness_mean < threshold` 또는 `hallucination_count_sum > threshold` 면 실패
- Judge 자체의 안정성 측정 (같은 입력 반복 실행 시 분산)
