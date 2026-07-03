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

## 결과 — 4단계 iteration

측정 일자: 2026-07-02 ~ 07-03 / judge: `claude-sonnet-4-6` / summary llm: `claude-haiku-4-5` / top_k=5 / days_window=14

각 단계는 **retriever 변경 없이 소스만 늘리는 것 (1→2)** 과 **retriever 자체를 바꾸는 것 (2→3, 3→4)** 로 구분된다.

| 지표 | 1. Baseline (Naver, dense only) | 2. +DART (dense) | 3. **+Hybrid (Dense+Sparse+RRF)** | 4. +Hybrid+Rerank (Cohere v3.5) |
|---|---|---|---|---|
| `groundedness_mean` | 0.883 | 0.841 | **0.908** ⭐ | 0.866 |
| `citation_accuracy_mean` | 0.900 | 0.847 | **0.921** ⭐ | 0.873 |
| **`hallucination_count_sum`** | **8** | 6 | **4 (-50%)** ⭐ | 7 |
| `coverage_mean` | 0.805 | 0.770 | **0.851** ⭐ | 0.783 |
| `count_no_news` | 0 | 0 | 0 | 0 |

### Tier 별 세부 (grounded_mean / halluc_sum)

| Tier | 1. Baseline | 2. +DART | 3. **+Hybrid** | 4. +Rerank |
|---|---|---|---|---|
| large (n=8) | 0.866 / 5 | 0.899 / 3 | **0.901 / 2** | 0.848 / 4 |
| mid (n=4) | 0.917 / 2 | 0.680 / 1 (outlier) | **0.927 / 1** | 0.905 / 1 |
| kosdaq (n=3) | 0.883 / 1 | 0.903 / 2 | **0.900 / 1** | 0.863 / 2 |

`check_regression.py` 결과: **PASS** — current(rerank) 도 baseline 대비 halluc 8→7 감소, groundedness drop 0.017 tolerance 내.

### Baseline 단계에서 judge 가 잡은 hallucination 원문 (샘플)

- **삼성전자** — "HBM 가격 인상을 예고" → 뉴스는 D램/HBM 수익성 역전만 언급, 가격 인상은 미확정.
- **NAVER** — "4GW AI 데이터센터" → 뉴스엔 4GW/8GW 혼재.
- **카카오** — "성과급 체계 변경에 따른 인건비 부담" → 뉴스에 인건비 언급 없음.
- **에코프로비엠** — "코스피 7.89% 급락" 을 코스닥 종목에 혼용.
- **LG에너지솔루션** — "로봇 배터리 수요 증가" 를 확정 표현으로 서술 (뉴스는 전망만).

## 관찰 — 단계별로 무엇이 얼마나 바뀌었나

**1 → 2 (+DART)**: hallucination 8→6 개선. NAVER (0.6→0.95), 카카오 (0.85→0.95) 처럼 DART 공시가 top-K 에 안정적으로 껴든 종목은 큰 개선. SK하이닉스·셀트리온·한화 는 "임원 특정증권 보고" 같은 소음 공시가 top-K 에 껴들어 오히려 후퇴. 삼성바이오는 judge outlier (`grounded=0.0`) 로 mid tier mean 을 훼손. **소스 확장만으론 top-K retrieval 이 소스 다양성을 반영 못 하는 근본 한계 확인**.

**2 → 3 (+Hybrid retrieval: Dense+Sparse+RRF)**: **모든 지표 최고 성적**. groundedness 0.883→0.908, hallucination **8→4 (50% ↓)**, coverage 0.805→0.851. Sparse (Postgres `tsvector` + `ts_rank_cd`) 가 뉴스 body 안의 종목명/고유명사/숫자 정확 매칭을 보강해 dense 만으론 놓치던 근거 뉴스를 top-K 로 끌어올림. RRF (K=60) 로 두 랭킹 병합. 삼성바이오 outlier 도 해결. Inflection point.

**3 → 4 (+Cohere Rerank v3.5)**: **오히려 후퇴**. groundedness 0.908→0.866, halluc 4→7. NAVER (0.95→0.5, halluc 0→2), 알테오젠 (0.92→0.82), 에코프로비엠 (0.9→0.85) 에서 큰 후퇴. 원인 추정: general-purpose reranker 가 한국 주식 도메인의 고유명사/사업부명 매칭에 domain 지식이 없어 오히려 소음을 upvote. large tier 가 특히 후퇴 (0.901/2 → 0.848/4). **"최신 기법 = 정답" 이 아님을 domain eval 로 계량**.

## 다음 iteration 방향 (관찰에서 자연스레 나오는)

1. Reranker 를 domain fine-tunable 로 교체 — 로컬 BGE-reranker-v2-m3 + 한국 주식 뉴스 pair 로 소량 fine-tune
2. LLM listwise rerank (Claude Sonnet 자체를 reranker 로) 로 비교 실험
3. 공시 소음 whitelist ("임원", "주요주주", "특정증권" 접두 컷) 를 sparse 인덱싱 전 필터로 반영
4. Judge stochasticity 완화 — 종목당 judge 3회 실행 + median 사용

## 이력서/포트폴리오용 한 줄

> LLM RAG 요약 파이프라인 품질 회귀 방지를 위해 **LLM-as-a-judge (Claude Sonnet 4.6)** 기반 evals 파이프라인 설계. 골든셋 15종목 × tier 별 4개 지표를 계량화하며 **4단계 iteration** (Naver → +DART → +Hybrid Dense+Sparse+RRF → +Cohere Rerank) 를 실측. **Hybrid retrieval 조합에서 hallucination 8→4 (50% ↓), groundedness 0.883→0.908** 로 최고 점수 달성. Cohere Rerank v3.5 추가 시 오히려 후퇴 (halluc 4→7) 하는 결과로 **general-purpose reranker 의 domain 한계를 계량화**, 다음 iteration 방향 (domain-tuned BGE / LLM listwise rerank) 을 데이터 기반으로 도출. GitHub Actions 회귀 게이트로 재발 자동 차단.

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
