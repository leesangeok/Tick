"""RAG 쿼리 재작성 port.

원본 쿼리는 종목명+심볼+일반적 시황 키워드로 고정돼 있어, 종목별 이슈 (임상결과, 계약 체결, 감사의견
비적정, 규제 이슈 등) 를 반영하지 못한다. LLM 으로 종목 특성을 파악해 다변량 쿼리를 생성 → hybrid
retrieval 결과를 RRF 로 병합해 recall 향상.
"""

from typing import Protocol

from app.domain.value_objects.stock_symbol import StockSymbol


class QueryRewritePort(Protocol):
    async def rewrite(
        self, symbol: StockSymbol, stock_name: str, base_query: str
    ) -> list[str]:
        """base_query 를 시드로 관련 쿼리 여러 개 생성. 최소 1개 (base) 는 항상 포함."""
        ...
