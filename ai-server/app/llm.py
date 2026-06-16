"""Anthropic Claude wrapper.

prompt caching: system 메시지에 cache_control 마커. 같은 system 으로 여러 요청 시 hit.
"""

from app.config import settings
from app.deps import anthropic_client

SYSTEM_PROMPT = """당신은 한국 주식 시장의 정보를 한국어로 요약하는 보조자입니다.

규칙:
- 제공된 뉴스/공시 내용만 근거로 사용합니다.
- 투자 권유 표현은 금지: "매수하세요", "지금 사세요", "확실히 오릅니다", "수익 보장" 등.
- 허용 표현: "~ 영향으로 해석됩니다", "~ 요인이 있습니다", "단기 변동성이 있을 수 있습니다".
- 마지막 줄에 반드시 다음 문구를 포함합니다:
  본 요약은 정보 제공 목적이며 투자 권유가 아닙니다.
- 출력 형식: 2~4 문장의 간결한 한국어 요약."""


def summarize(user_prompt: str) -> str:
    """system + user_prompt 로 LLM 호출. system 은 caching 활성."""
    response = anthropic_client().messages.create(
        model=settings.anthropic_model,
        max_tokens=settings.anthropic_max_tokens,
        system=[
            {
                "type": "text",
                "text": SYSTEM_PROMPT,
                "cache_control": {"type": "ephemeral"},
            }
        ],
        messages=[{"role": "user", "content": user_prompt}],
    )
    # Anthropic SDK 응답: content 는 블록 리스트. 텍스트 블록만 추출.
    parts = [b.text for b in response.content if getattr(b, "type", None) == "text"]
    return "\n".join(parts).strip()
