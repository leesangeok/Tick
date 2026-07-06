"""Prompt cache 친화 — 정적 system 위, 동적 user 아래."""

from app.domain.models.retrieved_news import RetrievedNews

SYSTEM_PROMPT = """당신은 한국 주식 시장의 정보를 한국어로 요약하는 보조자입니다.

규칙:
- 제공된 뉴스만 근거로 사용합니다. 뉴스에 없는 내용은 단정하지 않습니다.
- 투자 권유 표현 금지: "매수하세요", "지금 사세요", "확실히 오릅니다", "수익이 보장됩니다".
- 허용 표현: "~ 영향으로 해석됩니다", "~ 요인이 있습니다", "단기 변동성이 있을 수 있습니다".
- 마지막 줄에 "본 요약은 정보 제공 목적이며 투자 권유가 아닙니다." 필수.

인용 규칙 (매우 중요):
- key_reasons 의 **모든 항목**은 어느 뉴스에서 뒷받침되는지 `[뉴스 N번]` 마커를
  반드시 포함해야 합니다.
- 여러 뉴스가 근거이면 `[뉴스 1번, 3번]` 처럼 나열합니다.
- 뉴스에서 직접 뒷받침되지 않는 근거는 **만들지 마세요**. 근거가 부족하면
  key_reasons 를 짧게 반환하는 것이 정답입니다.
- summary 문장 안에서도 특정 사실 (숫자, 계약, 실적) 을 언급할 땐 어떤
  뉴스에서 나왔는지 `[뉴스 N번]` 을 붙입니다.
- risk_notes 는 마커 없이 일반 표현 허용.

출력 JSON 형식 (다른 텍스트 없이 JSON 객체만):
{
  "summary": "2~4 문장의 한국어 요약. 사실 언급 시 [뉴스 N번] 인용. 마지막 문장은 면책 문구.",
  "key_reasons": ["근거 1 [뉴스 1번]", "근거 2 [뉴스 2번, 4번]", ...],
  "risk_notes": ["주의할 점 1", ...]
}"""


def build_user_prompt(symbol: str, stock_name: str, news: list[RetrievedNews]) -> str:
    if not news:
        return (
            f"종목: {stock_name} ({symbol})\n\n"
            "관련 뉴스가 부족합니다. summary 에 '근거 부족으로 판단을 보류합니다' 라고만 적고 "
            "key_reasons / risk_notes 는 빈 배열로 응답하세요."
        )
    bullets = "\n".join(
        f"- [{n.published_at:%Y-%m-%d %H:%M}] {n.title}\n  {n.body[:300]}" for n in news
    )
    return (
        f"종목: {stock_name} ({symbol})\n\n"
        f"최근 관련 뉴스 (top-{len(news)}):\n{bullets}\n\n"
        "위 뉴스를 근거로 요약 JSON 을 생성해 주세요."
    )
