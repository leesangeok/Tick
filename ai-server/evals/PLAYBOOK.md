# PLAYBOOK — Baseline vs After 검증 순서

**목표**: `results/*.json` 두 개를 만들어 `aggregate.groundedness_mean` / `hallucination_count_sum` 을 비교. 그 숫자가 이력서 문구가 된다.

## 전제 조건 체크리스트

- [ ] **Postgres (pgvector)** 로컬 기동: `docker compose -f backend/compose.yaml up -d`
- [ ] **ai-server** 로컬 기동 (백엔드가 `POST /ai/embeddings/{symbol}` 를 이걸 통해 호출) — 로컬 uvicorn 또는 컨테이너
- [ ] **backend** 로컬 기동 with env:
  - [ ] `TICK_AUTH_DEV_TOKEN_ENABLED=true` (JWT 우회 필수)
  - [ ] `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` (baseline 을 위해)
  - [ ] `TICK_AI_SERVER_URL=http://localhost:8000` (ai-server 위치)
- [ ] **API 키**:
  - [ ] `OPENAI_API_KEY` (embedding)
  - [ ] `ANTHROPIC_API_KEY` (summary + judge)
  - [ ] `DART_API_KEY` — opendart.fss.or.kr 무료 발급 (after 단계에서 필요)

## 실행 — 두 번

### Baseline (네이버만)

```bash
# backend 를 DART 비활성 상태로 기동
export TICK_DART_ENABLED=false
# ... backend bootRun ...

# harness 실행
cd ai-server
export OPENAI_API_KEY=sk-proj-...
export ANTHROPIC_API_KEY=sk-ant-...
export POSTGRES_DSN=postgresql://tick:tick@localhost:5432/tick
./evals/harness.sh baseline-naver-only
```

`evals/results/YYYY-MM-DD_baseline-naver-only.json` 생성 → 커밋.

### After (네이버 + DART)

```bash
# backend 를 DART 활성 상태로 재기동
export TICK_DART_ENABLED=true
export DART_API_KEY=<opendart 발급 키>
# ... backend bootRun ...

cd ai-server
./evals/harness.sh after-dart-added
```

`evals/results/YYYY-MM-DD_after-dart-added.json` 생성 → 커밋.

## 확인

```bash
cd ai-server
python3 -m evals.check_regression
```

- 통과하면 `[eval-guard] PASS`
- baseline 대비 hallucination 이 늘거나 groundedness 가 급락하면 `[eval-guard] FAIL` + 사유
- 두 결과가 다 있어야 실제 비교. baseline 만 있으면 skip.

## 이력서 문구 채우기

두 JSON 의 `aggregate` 값을 `EVAL_REPORT.md` 표에 채우고, 마지막 "한 줄" 을 완성.

## 시간/비용 대략

- collect + embed: 종목당 ~5초 × 15 = **1~2 분** (× 2회 = 3~5분)
- LLM (Haiku summary + Sonnet judge): 종목당 ~10초 × 15 = **2~3 분** (× 2회)
- API 비용: baseline+after 합쳐서 대략 **$1 미만** (매우 rough)

## 부담되면

- **15종목 대신 5종목**만: golden_set 을 편집해서 대형주 3 + 중형 1 + KOSDAQ 1 만 남기고 실행 → 시간/비용 1/3
- **judge 를 Haiku 로**: `evals/judge.py` 의 `JUDGE_MODEL` 을 `claude-haiku-4-5` 로 (신뢰도 소폭 하락, 비용 큰 폭 절감)
- **DART 만 켜고 재수집 없이**: 이건 baseline / after 차이가 안 나오니 의미 없음. 재수집은 꼭 해야 한다.
