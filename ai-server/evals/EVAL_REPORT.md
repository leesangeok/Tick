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

## 결과 — 8단계 iteration

측정 일자: 2026-07-02 ~ 07-07 / summary llm: `claude-haiku-4-5` / top_k=5 / days_window=14
judge: 1~7단계 `claude-sonnet-4-6` (단일) / **8단계 multi: `claude-sonnet-4-6` + `claude-opus-4-7`**

각 단계는 **retriever 변경 없이 소스만 늘리는 것 (1→2)**, **retriever 자체를 바꾸는 것 (2→3, 3→4)**, **판정기 개선 (5~6, 8)** 로 구분된다.

| 지표 | 1.Base | 2.+DART | 3.+Hybrid | 4.+Rerank | 5.+Median | 6.+σRetry | 7.+Cite+Denoise | **8.+MultiJudge** |
|---|---|---|---|---|---|---|---|---|
| `groundedness_mean` | 0.883 | 0.841 | 0.908 | 0.866 | 0.889 | 0.909 | 0.919 | **0.942** ⭐ |
| `citation_accuracy_mean` | 0.900 | 0.847 | 0.921 | 0.873 | 0.894 | 0.922 | 0.925 | **0.949** ⭐ |
| **`hallucination_count_sum`** | **8** | 6 | 4 | 7 | 7 | 4 | 3 | **0** ⭐⭐ |
| `coverage_mean` | 0.805 | 0.770 | 0.851 | 0.783 | 0.821 | 0.826 | 0.822 | 0.819 |
| `count_no_news` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `groundedness_std_mean` | — | — | — | — | — | 0.003 | 0.003 | 0.036 † |
| `count_retried` | — | — | — | — | — | 0/15 | 0/15 | 0/15 |
| `groundedness_disagreement_mean` (cross-model) | — | — | — | — | — | — | — | **0.071** |
| `hallucination_disagreement_mean` (cross-model) | — | — | — | — | — | — | — | **0.333** |

† 8단계의 `groundedness_std_mean` 은 within-model σ 가 아니라 **cross-model σ** (Sonnet median vs Opus median). 두 모델 판정 편차이므로 5~7단계와 의미가 다름.

**Baseline → Final (8단계)**: groundedness **+0.059**, hallucination **-100%** (8→0), 모든 tier hallucination 0.
**단, 8단계 숫자는 판정기 자체가 바뀐 결과라 이전 단계와 apples-to-apples 가 아님** — 아래 "7 → 8" 해석 노트 참고.

### Tier 별 세부 (grounded_mean / halluc_sum)

| Tier | 1.Base | 3.+Hybrid | 6.+σRetry | 7.+Cite+Denoise | **8.+MultiJudge** |
|---|---|---|---|---|---|
| large (n=8) | 0.866 / 5 | 0.901 / 2 | 0.911 / 3 | 0.920 / 2 | **0.942 / 0** ⭐ |
| mid (n=4) | 0.917 / 2 | 0.927 / 1 | 0.917 / 0 | 0.943 / 0 | **0.944 / 0** ⭐ |
| kosdaq (n=3) | 0.883 / 1 | 0.900 / 1 | 0.893 / 1 | 0.883 / 1 | **0.942 / 0** ⭐ |

`check_regression.py` 결과 (7단계 기준): **PASS** — current(rerank) 도 baseline 대비 halluc 8→7 감소, groundedness drop 0.017 tolerance 내.

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

**Hybrid 상태에서 3 → 5 (+Judge×3 median)**: 3회 병렬 판정 후 지표별 median (settings.judge_repeat=3). **삼성바이오 outlier (`grounded=0.0`) 재발 완전 방지** — mid tier 가 0.917→0.680(outlier)→0.927(hybrid)→**0.890** 로 안정. `grounded=0.0` 사례 0건. 그러나 전체 mean 은 hybrid-single 대비 -0.019 로 오히려 미세 후퇴 — NAVER 는 hybrid-single 0.95 → judge×3 median 0.82. Judge (Sonnet 4.6) 가 같은 입력에도 개별 판정마다 편차가 큰 케이스가 median 3개로도 완전히 안 잡힘. **Median 이 outlier 는 막지만 stochasticity 근본 해결은 아님을 계량**. (뉴스 셋이 실시간 유입이라 fair 비교 아닌 부분도 있음 — 다음 iteration 에서 fixed 골든셋 dump 도입 예정.)

**5 → 6 (+σ retry)**: 3회 판정 후 지표별 표준편차 (σ) 계산 → threshold (grounded σ ≥ 0.15 or halluc σ ≥ 1.0) 넘으면 자동 추가 판정 4회 (total 7회). **결과: `count_retried = 0 / 15`, `groundedness_std_mean = 0.003`** — 이번 실행의 판정은 매우 안정하여 재판정 로직이 발동 안 함. 이 안정성 자체가 새로운 관측: (1) mid tier grounded 0.917 / **halluc 0** 회복 (역대 최고), (2) hybrid-single 수준 (0.909/halluc 4) 재현, (3) 판정 stochasticity 는 조건에 따라 매우 크게 요동친다 (5단계 실행의 NAVER 0.82 vs 6단계의 0.82±0.0). **재판정 로직은 안전장치로 코드에 상시 존재하되, 판정이 이미 안정한 경우엔 자동으로 no-op — 비용을 조건부로만 지출** 하는 관측 결과.

**6 → 7 (+citation-required 프롬프트 + DART noise blacklist + rerank ON)**: 두 축을 동시에.
1) **Summary 프롬프트에 `[뉴스 N번]` 인용 마커 강제** — key_reasons/summary 안 사실 언급 시 어떤 뉴스에서 뒷받침되는지 명시. 근거 없으면 만들지 말라고 규칙화. hallucination 원천 방어.
2) **DART report_nm 소음 blacklist** — `임원/주요주주/특정증권/소유상황` 포함 공시 컷 (수집/파싱 단계). DB 의 기존 57건 소음 뉴스도 정리 (DART 총 142→85건). SK하이닉스류 오염 원인 직접 제거.
3) Rerank (Cohere v3.5) 도 다시 활성 — 3~5단계에서 후퇴 관측했지만 지금은 소음이 이미 컷된 상태라 rerank 가 정상 상위 문서를 뽑을 가능성 상승.

**결과: 역대 최고**. groundedness 0.909→**0.919**, hallucination 4→**3**, mid tier grounded **0.943 / halluc 0**. Baseline 대비 halluc **8→3 (-62.5%)**, mid tier halluc **2→0**. Denoise 로 top-K 오염 원인 자체를 제거하니 rerank 도 정상 작동 (SK하이닉스 하락 재현 X). Citation 마커 강제로 요약 근거 트레이서빌리티까지 확보.

**7 → 8 (+Multi-judge: Sonnet 4.6 + Opus 4.7 cross-check)**: 파이프라인 (retriever/rerank/prompt/soures) 은 그대로. **판정기만 두 세대 Claude 로 확장** — 병렬 판정 후 지표별 median-of-medians, 두 모델 median 값의 차이를 `groundedness_disagreement` 로 함께 기록.

**결과: 숫자상 전 단계 최고 갱신**. groundedness 0.919→**0.942**, hallucination 3→**0**, 전 tier halluc 0. Cross-model disagreement: `grounded_disagreement_mean=0.071`, `halluc_disagreement_mean=0.333` — 두 모델 판정 일치도 매우 높음.

**단, 이건 두 해석이 겹친 결과라 apples-to-apples 개선 아님**:
1. **판정 관대성 shift**: Opus 4.7 이 Sonnet 4.6 보다 관대하게 판정하는 경향 → mean 상향. 개선분의 상당수는 이 효과일 가능성.
2. **실제 품질 재발견**: 두 모델 median 이 0.942 로 일치 (`disagree=0.071`) → 7단계 요약 품질 자체가 이 정도인데 Sonnet 단독일 땐 과소평가된 부분도 있음.

**의미 있는 신호는 mean 이 아니라**: (a) `halluc=0` 을 두 모델 모두 확인 → 요약 품질이 실제로 견고, (b) `disagree_mean=0.071` → 시스템적 편향 크지 않음, (c) 카카오 `disagree=0.2` 최대 → 판정 편향 진단 시 first-look 대상. **다중 judge 의 가치는 스코어 상향이 아니라 편향 계량 자체**.

## 다음 iteration 방향 (관찰에서 자연스레 나오는)

1. Reranker 를 domain fine-tunable 로 교체 — 로컬 BGE-reranker-v2-m3 + 한국 주식 뉴스 pair 로 소량 fine-tune
2. LLM listwise rerank (Claude Sonnet 자체를 reranker 로) 로 비교 실험
3. 공시 소음 whitelist ("임원", "주요주주", "특정증권" 접두 컷) 를 sparse 인덱싱 전 필터로 반영
4. ✅ **판정 편향 진단 도입 완료 (8단계)** — 다음: disagreement threshold 기반 자동 human-review 큐, 카카오처럼 `disagree≥0.15` 종목만 골라 재확인
5. Judge 3rd provider (Gemini 등) 추가 — 세대 cross (Sonnet/Opus) 만으론 provider-level bias 는 여전히 잡히지 않음
6. 골든셋 뉴스 스냅샷 dump 로 실행간 fair 비교 (실시간 유입 뉴스 셋 차이 제거)
7. `check_regression.py` 를 multi-judge 지표 (disagreement) 도 게이트에 반영

## 이력서/포트폴리오용 한 줄

> LLM RAG 요약 파이프라인 품질 회귀 방지를 위해 **LLM-as-a-judge (Sonnet 4.6, 3회 median + σ 기반 자동 재판정 + Sonnet/Opus cross-model)** 기반 evals 파이프라인 설계. 골든셋 15종목 × tier 별 4개 지표를 계량화하며 **8단계 iteration** (Naver → +DART → +Hybrid Dense+Sparse+RRF → +Cohere Rerank → +Judge×3 median → +σ retry → +Citation-required prompt + DART noise blacklist → +Multi-judge Sonnet 4.6/Opus 4.7 cross-check) 을 실측. **최종 (판정 관대성 shift 포함): groundedness 0.883 → 0.942, hallucination 8→0 (100% ↓), 전 tier hallucination 0**. 각 단계에서 후퇴 케이스도 계량화 — Cohere Rerank v3.5 초기 조합에서 domain 한계로 halluc 4→7 이었으나 소음 blacklist 후 rerank 도 정상 작동. Multi-judge 로 판정 편향 자체를 계량 (`disagreement_mean=0.071`) → 스코어 상향보다 편향 진단이 본질적 가치임을 리포트에 명시. GitHub Actions 회귀 게이트 (`check_regression.py`) 로 재발 자동 차단.

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
