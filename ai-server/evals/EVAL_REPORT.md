# RAG 요약 품질 개선 리포트 — 뉴스 소스 확장

**가설**: 네이버 검색 API 만으로는 실시간 급등/급락 이유를 hallucination 없이 요약하기 어렵다.
DART 공시 (실적/증자/M&A) 를 소스에 추가하면 groundedness / citation-accuracy 는 오르고
hallucination 은 줄어든다.

**측정 방식**: 골든셋 15종목 (대형 8 / 중형 4 / KOSDAQ 3) 에 대해 요약 생성 후 Claude Sonnet 4.6
을 judge 로 채점 (groundedness / citation-accuracy / hallucination-count / coverage). 자세한 기준은
[README](./README.md).

## 실행 순서

```bash
# 1. 백엔드/ai-server/DB 기동 (뉴스 스키마 필요)
#    tick.dart.enabled=false (baseline 은 네이버만)

# 2. 골든셋 종목 뉴스 수집 + 임베딩 (네이버만)
for s in $(python3 -c "import json; [print(json.loads(l)['symbol']) for l in open('ai-server/evals/golden_set.jsonl')]"); do
  curl -X POST "http://localhost:8080/api/v1/news/$s/collect?limit=20"
  curl -X POST "http://localhost:8080/api/v1/ai/stocks/$s/embed"
done

# 3. Baseline 측정
cd ai-server
uv run python -m evals.run_eval --label baseline-naver-only

# 4. DART 활성화 후 재수집 + after 측정
#    TICK_DART_ENABLED=true DART_API_KEY=... 로 backend 재기동
# (재수집 반복) ...
uv run python -m evals.run_eval --label after-dart-added

# 5. 결과 확인
python -m evals.check_regression
```

## 결과

측정 일자: 2026-07-02 / judge: `claude-sonnet-4-6` / summary llm: `claude-haiku-4-5` / top_k=5 / days_window=14

| 지표 | Baseline (Naver only) | After (Naver + DART) | Δ |
|---|---|---|---|
| `groundedness_mean` | **0.883** | ? | ? |
| `citation_accuracy_mean` | **0.900** | ? | ? |
| `hallucination_count_sum` | **8** | ? | ? |
| `coverage_mean` | **0.805** | ? | ? |
| `count_no_news` | **0** | ? | ? |

### Tier 별 세부

| Tier | Baseline `groundedness_mean` | Baseline `halluc_sum` | After `groundedness_mean` | After `halluc_sum` |
|---|---|---|---|---|
| large (n=8) | **0.866** | **5** | ? | ? |
| mid (n=4) | **0.917** | **2** | ? | ? |
| kosdaq (n=3) | **0.883** | **1** | ? | ? |

### Baseline 에서 judge 가 잡은 hallucination 원문 (샘플)

- **삼성전자** — "HBM 가격 인상을 예고" → 뉴스는 D램/HBM 수익성 역전만 언급, 가격 인상은 미확정.
- **NAVER** — "4GW AI 데이터센터" → 뉴스엔 4GW/8GW 혼재.
- **카카오** — "성과급 체계 변경에 따른 인건비 부담" → 뉴스에 인건비 언급 없음.
- **에코프로비엠** — "코스피 7.89% 급락" 을 코스닥 종목에 혼용.
- **LG에너지솔루션** — "로봇 배터리 수요 증가" 를 확정 표현으로 서술 (뉴스는 전망만).

## 관찰

<!-- 결과 채운 뒤 3~5개 항목. 예:
- 대형주는 baseline 도 groundedness 0.75+ 로 안정. DART 는 소폭 개선.
- KOSDAQ 3종목은 baseline 에서 뉴스 0건 이 잦아 요약 자체가 스킵됨 (count_no_news=?). DART 추가 후에도 여전히 얕음 — 소형주는 별도 소스 필요.
- hallucination_examples 를 보면 baseline 은 실적/계약 금액 지어냄이 다수. DART 공시로 report_nm 이 붙자 이런 유형이 줄어듦.
-->

## 이력서/포트폴리오용 한 줄

<!-- 실제 숫자 확보 후 완성. 예:
> 뉴스 소스 확장 (Naver → +DART) 에 따른 RAG 요약 품질 개선을 LLM-as-a-judge 로 계량화.
> 골든셋 15 종목 기준 groundedness 0.62→0.81, hallucination 사례 13→3.
> GitHub Actions 회귀 게이트 (`check_regression.py`) 로 재발 자동 차단.
-->
