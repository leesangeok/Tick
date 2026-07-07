"""소형주 등 뉴스 0건 종목을 위한 섹터 fallback.

기본 retriever 가 종목별 뉴스를 반환하지 못하면 (KOSDAQ 마이너 종목 등), 같은 sector 의
다른 종목 최신 뉴스를 뽑아 요약 근거로 대체. LLM 프롬프트는 "직접 뉴스가 부족해 섹터
관련 뉴스를 참고" 라고 명시해 사용자가 오해하지 않게 한다.
"""

from typing import Protocol

from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol


class SectorFallbackRetrieverPort(Protocol):
    async def retrieve_sector_peers(
        self, symbol: StockSymbol, top_k: int, days_window: int
    ) -> list[RetrievedNews]:
        """symbol 자신은 제외하고 같은 sector 의 최신 뉴스 top_k. sector 정보 없으면 빈 리스트."""
        ...
