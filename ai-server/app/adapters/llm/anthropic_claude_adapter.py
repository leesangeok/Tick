"""LangChain `ChatAnthropic` 으로 Claude 호출.

Prompt cache 는 LangChain ChatAnthropic 가 system message 의 `cache_control` 메타데이터를
지원. 다만 가장 명확한 제어 위해 직접 anthropic SDK 도 옵션으로 보유 (caching 우선이라면).
일단 langchain 통일.
"""

import json

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage

from app.adapters.observability.langfuse_callback import langfuse_callback_handler
from app.application.prompts.stock_summary_prompt import SYSTEM_PROMPT, build_user_prompt
from app.config.settings import settings
from app.domain.models.ai_summary import AiSummary
from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol


class AnthropicClaudeAdapter:
    def __init__(self) -> None:
        self._client = ChatAnthropic(
            model=settings.anthropic_model,
            api_key=settings.anthropic_api_key,
            max_tokens=settings.anthropic_max_tokens,
        )
        # Langfuse 자동 trace (model, tokens, prompt, response 다 캡처)
        self._config = {
            "callbacks": [langfuse_callback_handler()],
            "run_name": "stock-summary-generation",
        }

    async def generate_stock_summary(
        self, symbol: StockSymbol, stock_name: str, news: list[RetrievedNews]
    ) -> AiSummary:
        user_prompt = build_user_prompt(symbol.value, stock_name, news)

        # system message 에 cache_control 부여 → prompt cache 활성 (Anthropic ephemeral)
        system = SystemMessage(
            content=[
                {
                    "type": "text",
                    "text": SYSTEM_PROMPT,
                    "cache_control": {"type": "ephemeral"},
                }
            ]
        )
        response = await self._client.ainvoke(
            [system, HumanMessage(content=user_prompt)],
            config=self._config,
        )
        content = response.content
        text = content if isinstance(content, str) else self._join_blocks(content)
        parsed = self._parse_json(text)

        return AiSummary(
            symbol=symbol.value,
            summary=parsed.get("summary", text.strip()),
            key_reasons=parsed.get("key_reasons", []),
            risk_notes=parsed.get("risk_notes", []),
            sources=AiSummary.sources_from(news),
        )

    @staticmethod
    def _join_blocks(blocks) -> str:
        parts = []
        for b in blocks:
            if isinstance(b, dict) and b.get("type") == "text":
                parts.append(b.get("text", ""))
            elif isinstance(b, str):
                parts.append(b)
        return "\n".join(parts)

    @staticmethod
    def _parse_json(text: str) -> dict:
        """LLM 이 가끔 ```json ... ``` 로 감싸거나 앞뒤에 텍스트 붙임. 안전 파싱."""
        s = text.strip()
        # 코드펜스 제거
        if s.startswith("```"):
            s = s.split("```", 2)[1]
            if s.startswith("json"):
                s = s[4:]
            s = s.rsplit("```", 1)[0]
        s = s.strip()
        try:
            return json.loads(s)
        except json.JSONDecodeError:
            # 본문에서 첫 { ... 마지막 } 까지만 시도
            i, j = s.find("{"), s.rfind("}")
            if i != -1 and j > i:
                try:
                    return json.loads(s[i : j + 1])
                except json.JSONDecodeError:
                    return {}
            return {}
