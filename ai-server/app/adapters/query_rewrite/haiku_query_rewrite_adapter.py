"""Claude Haiku 로 검색 쿼리 재작성.

요약 LLM (`AnthropicClaudeAdapter`) 과 분리 — 두 호출의 온도/max_tokens/시스템 프롬프트가
다르고, 리라이트 실패가 요약 실패로 이어지지 않도록 예외 격리도 다르다.

Haiku 를 쓰는 이유: 재작성은 창의성보다 속도/비용 중요. Haiku 4.5 로 충분.
"""

import json
import logging

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage

from app.application.prompts.query_rewrite_prompt import SYSTEM_PROMPT, build_user_prompt
from app.config.settings import settings
from app.domain.value_objects.stock_symbol import StockSymbol

log = logging.getLogger(__name__)

_MAX_VARIANTS = 3


class HaikuQueryRewriteAdapter:
    def __init__(self) -> None:
        self._client = ChatAnthropic(
            model=settings.query_rewrite_model,
            api_key=settings.anthropic_api_key,
            # 리라이트 답은 짧다. 배열 하나면 충분.
            max_tokens=256,
        )

    async def rewrite(
        self, symbol: StockSymbol, stock_name: str, base_query: str
    ) -> list[str]:
        try:
            response = await self._client.ainvoke(
                [
                    SystemMessage(content=SYSTEM_PROMPT),
                    HumanMessage(
                        content=build_user_prompt(symbol.value, stock_name, base_query)
                    ),
                ]
            )
            variants = self._parse_variants(response.content)
        except Exception as e:
            # 리라이트 실패는 조용히 fallback — 요약 자체는 base_query 로 진행 가능해야 함.
            log.warning("query rewrite failed symbol=%s error=%s", symbol.value, e)
            return [base_query]

        # base_query 는 항상 포함 (원본 신호 손실 방지)
        merged = [base_query]
        for v in variants:
            v = v.strip()
            if v and v not in merged:
                merged.append(v)
        return merged[: _MAX_VARIANTS + 1]  # +1 = base

    @staticmethod
    def _parse_variants(content) -> list[str]:
        text = content if isinstance(content, str) else _join_blocks(content)
        s = text.strip()
        # 코드펜스 방어
        if s.startswith("```"):
            s = s.split("```", 2)[1]
            if s.startswith("json"):
                s = s[4:]
            s = s.rsplit("```", 1)[0].strip()
        try:
            data = json.loads(s)
        except json.JSONDecodeError:
            # 배열 부분만 찾아서 재시도
            i, j = s.find("["), s.rfind("]")
            if i != -1 and j > i:
                try:
                    data = json.loads(s[i : j + 1])
                except json.JSONDecodeError:
                    return []
            else:
                return []
        if not isinstance(data, list):
            return []
        return [str(x) for x in data if isinstance(x, str | int | float) and str(x).strip()]


def _join_blocks(blocks) -> str:
    parts = []
    for b in blocks:
        if isinstance(b, dict) and b.get("type") == "text":
            parts.append(b.get("text", ""))
        elif isinstance(b, str):
            parts.append(b)
    return "\n".join(parts)
