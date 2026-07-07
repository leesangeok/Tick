"""Eval 전용 설정. `app.config.settings.Settings` 와 물리적으로 격리.

이유: multi-judge (Sonnet 4.6 + Opus 4.7 병렬) 는 요약 1건당 최대 7회 LLM 판정. eval 파이프라인에선
품질/편향 정량화 목적으로 감수하지만, prod 요약 use case 가 실수로 `judge_multi_enabled` 를 참조하는
순간 요청당 비용이 폭발할 수 있다. 그래서 이 값들은 `app/` 아래에서 절대 import 되지 않도록 evals
모듈로 옮겼다.

공유 필드 (anthropic_api_key, openai_api_key, retrieval_* 등) 는 계속 `app.config.settings`.
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class EvalSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # LLM-as-a-judge 를 N회 병렬 호출해 지표별 median 을 사용. 홀수 권장.
    # 1 이면 단일 호출 (하위호환). 3 이면 outlier 완화 + 비용 3배.
    judge_repeat: int = 3

    # 판정 편차 (표준편차) 가 threshold 를 넘으면 자동 재판정 (extras 회 추가 → total 홀수 유지).
    # 판정 3회로도 stochasticity 가 안 잡히는 종목만 선택적으로 재판정. 0 이면 재판정 비활성.
    judge_std_threshold_grounded: float = 0.15
    judge_std_threshold_halluc: float = 1.0
    judge_retry_extras: int = 4

    # 다중 judge (판정 편향 교차검증). True 면 Sonnet 4.6 + Opus 4.7 두 세대의 Claude 로 병렬
    # 판정 후 지표별 median-of-medians. 두 모델 간 판정 편차 (disagreement σ) 도 함께 기록.
    # OpenAI 는 이 프로젝트에서 embedding 전용이라 chat judge 로는 사용 X.
    judge_multi_enabled: bool = False
    opus_judge_model: str = "claude-opus-4-7"


eval_settings = EvalSettings()
